package com.example.QuanLyLuong.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.example.QuanLyLuong.common.CompensationItemType;
import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.dto.PayrollSearchCriteria;
import com.example.QuanLyLuong.dto.PayrollSearchSummary;
import com.example.QuanLyLuong.entity.CompensationItem;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.entity.SalaryConfig;
import com.example.QuanLyLuong.entity.Timesheet;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.PayrollRepository;
import com.example.QuanLyLuong.repository.TimesheetRepository;

import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {

    private static final double DEFAULT_STANDARD_MONTHLY_WORK_DAYS = 26.0;
    private static final double DEFAULT_STANDARD_WORK_HOURS_PER_DAY = 8.0;

    private final PayrollRepository payrollRepository;
    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryConfigService salaryConfigService;
    private final CompensationItemService compensationItemService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ACCOUNTANT')")
    public Payroll calculateForOne(Long employeeId, Integer month, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + employeeId));

        Timesheet timesheet = timesheetRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa có dữ liệu chấm công cho nhân viên ID " + employeeId + " tháng " + month + "/" + year));

        YearMonth yearMonth = YearMonth.of(year, month);
        SalaryConfig config = salaryConfigService.getEffectiveConfig(employeeId, yearMonth);
        List<CompensationItem> applicableItems = compensationItemService.findApplicableItems(employeeId, yearMonth);

        double standardWorkDays = positiveDouble(config.getStandardWorkDays() == null ? null : config.getStandardWorkDays().doubleValue(), DEFAULT_STANDARD_MONTHLY_WORK_DAYS);
        double standardWorkHoursPerDay = positiveDouble(config.getStandardWorkHoursPerDay(), DEFAULT_STANDARD_WORK_HOURS_PER_DAY);
        double standardMonthlyHours = standardWorkDays * standardWorkHoursPerDay;
        double hourlyRate = standardMonthlyHours <= 0 ? 0.0 : safeDouble(employee.getBaseSalary()) / standardMonthlyHours;
        double regularHours = safeDouble(timesheet.getRegularHours());
        if (regularHours <= 0 && safeInteger(timesheet.getWorkDays()) > 0) {
            regularHours = safeInteger(timesheet.getWorkDays()) * standardWorkHoursPerDay;
        }

        double baseSalaryAmount = round2(hourlyRate * Math.min(regularHours, standardMonthlyHours));
        double overtimePay = round2(hourlyRate * (
                safeDouble(timesheet.getOvertimeWeekdayHours()) * positiveDouble(config.getOvertimeWeekdayMultiplier(), 1.5)
                        + safeDouble(timesheet.getOvertimeWeekendHours()) * positiveDouble(config.getOvertimeWeekendMultiplier(), 2.0)
                        + safeDouble(timesheet.getOvertimeHolidayHours()) * positiveDouble(config.getOvertimeHolidayMultiplier(), 3.0)
        ));

        double dynamicAllowance = sumByType(applicableItems, CompensationItemType.ALLOWANCE);
        double dynamicBonus = sumByType(applicableItems, CompensationItemType.BONUS);
        double dynamicDeduction = sumByType(applicableItems, CompensationItemType.DEDUCTION);
        double taxableDynamicIncome = applicableItems.stream()
                .filter(item -> item.getComponentType() != CompensationItemType.DEDUCTION && Boolean.TRUE.equals(item.getTaxable()))
                .mapToDouble(item -> safeDouble(item.getAmount()))
                .sum();

        double totalAllowance = round2(safeDouble(config.getAllowance()) + dynamicAllowance);
        double totalBonus = round2(dynamicBonus);
        double otherDeductionAmount = round2(safeDouble(config.getDeduction()) + dynamicDeduction);
        double grossSalary = round2(baseSalaryAmount + overtimePay + totalAllowance + totalBonus);

        double insuranceRate = positiveDouble(config.getSocialInsuranceRate(), 0.08)
                + positiveDouble(config.getHealthInsuranceRate(), 0.015)
                + positiveDouble(config.getUnemploymentInsuranceRate(), 0.01);
        double insuranceAmount = round2(baseSalaryAmount * insuranceRate);

        double taxableIncome = Math.max(
                0.0,
                baseSalaryAmount
                        + overtimePay
                        + safeDouble(config.getAllowance())
                        + taxableDynamicIncome
                        - insuranceAmount
                        - otherDeductionAmount
                        - positiveDouble(config.getPersonalDeduction(), 11000000.0)
        );
        double taxAmount = round2(taxableIncome * positiveDouble(config.getPersonalIncomeTaxRate(), 0.05));
        double actualSalary = round2(Math.max(0.0, grossSalary - insuranceAmount - taxAmount - otherDeductionAmount));

        Payroll payroll = payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseGet(Payroll::new);

        payroll.setEmployee(employee);
        payroll.setTimesheet(timesheet);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setBaseSalaryAmount(baseSalaryAmount);
        payroll.setOvertimePay(overtimePay);
        payroll.setTotalAllowance(totalAllowance);
        payroll.setTotalBonus(totalBonus);
        payroll.setInsuranceAmount(insuranceAmount);
        payroll.setTaxAmount(taxAmount);
        payroll.setOtherDeductionAmount(otherDeductionAmount);
        payroll.setGrossSalary(grossSalary);
        payroll.setActualSalary(actualSalary);
        if (payroll.getPaymentStatus() == null) {
            payroll.setPaymentStatus(PaymentStatus.PENDING);
        }
        return payrollRepository.save(payroll);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ACCOUNTANT')")
    public List<Payroll> calculateForAll(Integer month, Integer year) {
        return timesheetRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year)
                .stream()
                .map(timesheet -> calculateForOne(timesheet.getEmployee().getId(), month, year))
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_ACCOUNTANT')")
    public Payroll markAsPaid(Long payrollId) {
        Payroll payroll = findById(payrollId);
        payroll.setPaymentStatus(PaymentStatus.PAID);
        payroll.setPaymentDate(LocalDate.now());
        return payrollRepository.save(payroll);
    }

    @Transactional(readOnly = true)
    public Payroll findById(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bảng lương có ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Payroll> findByMonth(Integer month, Integer year) {
        return payrollRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year);
    }

    @Transactional(readOnly = true)
    public List<Payroll> findByMonthWithEmployeeAndDepartment(Integer month, Integer year) {
        return payrollRepository.findByMonthAndYearWithEmployeeAndDepartment(month, year);
    }

    @Transactional(readOnly = true)
    public List<Payroll> findByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public double getTotalPayrollAmount(Integer month, Integer year) {
        Double total = payrollRepository.sumTotalSalaryByMonthAndYear(month, year);
        return total == null ? 0.0 : total;
    }

    @Transactional(readOnly = true)
    public List<Payroll> searchPayrolls(PayrollSearchCriteria criteria) {
        Specification<Payroll> specification = (root, query, builder) -> builder.conjunction();

        if (criteria.getMonth() != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("month"), criteria.getMonth()));
        }
        if (criteria.getYear() != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("year"), criteria.getYear()));
        }
        if (criteria.getDepartmentId() != null) {
            specification = specification.and((root, query, builder) -> builder.equal(
                    root.join("employee", JoinType.INNER).join("department", JoinType.LEFT).get("id"),
                    criteria.getDepartmentId()
            ));
        }
        if (criteria.getPaymentStatus() != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("paymentStatus"), criteria.getPaymentStatus()));
        }
        if (criteria.getMinSalary() != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("actualSalary"), criteria.getMinSalary()));
        }
        if (criteria.getMaxSalary() != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("actualSalary"), criteria.getMaxSalary()));
        }
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            String pattern = "%" + criteria.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> {
                var employee = root.join("employee", JoinType.INNER);
                return builder.or(
                        builder.like(builder.lower(employee.get("fullName")), pattern),
                        builder.like(builder.lower(employee.get("email")), pattern),
                        builder.like(builder.lower(employee.get("position")), pattern),
                        builder.like(builder.lower(employee.get("employeeCode")), pattern)
                );
            });
        }

        return payrollRepository.findAll(specification).stream()
                .sorted(Comparator
                        .comparing(Payroll::getYear, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Payroll::getMonth, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(payroll -> payroll.getEmployee().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public PayrollSearchSummary buildSearchSummary(List<Payroll> payrolls) {
        double totalSalary = payrolls.stream().mapToDouble(payroll -> payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary()).sum();
        long paidCount = payrolls.stream().filter(payroll -> payroll.getPaymentStatus() == PaymentStatus.PAID).count();
        long pendingCount = payrolls.stream().filter(payroll -> payroll.getPaymentStatus() == PaymentStatus.PENDING).count();
        double averageSalary = payrolls.isEmpty() ? 0.0 : totalSalary / payrolls.size();
        double minSalary = payrolls.stream().mapToDouble(payroll -> payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary()).min().orElse(0.0);
        double maxSalary = payrolls.stream().mapToDouble(payroll -> payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary()).max().orElse(0.0);

        return PayrollSearchSummary.builder()
                .resultCount(payrolls.size())
                .totalSalary(totalSalary)
                .averageSalary(averageSalary)
                .paidCount(paidCount)
                .pendingCount(pendingCount)
                .minSalary(minSalary)
                .maxSalary(maxSalary)
                .build();
    }

    private double sumByType(List<CompensationItem> applicableItems, CompensationItemType type) {
        return round2(applicableItems.stream()
                .filter(item -> item.getComponentType() == type)
                .mapToDouble(item -> safeDouble(item.getAmount()))
                .sum());
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private double positiveDouble(Double value, double fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

