package com.example.QuanLyLuong.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.entity.SalaryConfig;
import com.example.QuanLyLuong.entity.Timesheet;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.PayrollRepository;
import com.example.QuanLyLuong.repository.TimesheetRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {

    private static final double STANDARD_MONTHLY_WORK_DAYS = 26.0;

    private final PayrollRepository payrollRepository;
    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryConfigService salaryConfigService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR')")
    public Payroll calculateForOne(Long employeeId, Integer month, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien co ID: " + employeeId));

        Timesheet timesheet = timesheetRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chua co du lieu cham cong cho nhan vien ID " + employeeId + " thang " + month + "/" + year));

        SalaryConfig config = salaryConfigService.getEffectiveConfig(employeeId, YearMonth.of(year, month));
        double actualSalary = computeSalary(
                employee.getBaseSalary(),
                timesheet.getWorkDays(),
                config.getAllowance(),
                config.getDeduction()
        );

        Payroll payroll = payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseGet(Payroll::new);

        payroll.setEmployee(employee);
        payroll.setTimesheet(timesheet);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setActualSalary(actualSalary);
        if (payroll.getPaymentStatus() == null) {
            payroll.setPaymentStatus(PaymentStatus.PENDING);
        }
        return payrollRepository.save(payroll);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR')")
    public List<Payroll> calculateForAll(Integer month, Integer year) {
        return timesheetRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year)
                .stream()
                .map(timesheet -> calculateForOne(timesheet.getEmployee().getId(), month, year))
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_HR')")
    public Payroll markAsPaid(Long payrollId) {
        Payroll payroll = findById(payrollId);
        payroll.setPaymentStatus(PaymentStatus.PAID);
        payroll.setPaymentDate(LocalDate.now());
        return payrollRepository.save(payroll);
    }

    @Transactional(readOnly = true)
    public Payroll findById(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bang luong co ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Payroll> findByMonth(Integer month, Integer year) {
        return payrollRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year);
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

    private double computeSalary(Double baseSalary, Integer workDays, Double allowance, Double deduction) {
        double safeBaseSalary = baseSalary == null ? 0.0 : baseSalary;
        double safeWorkDays = workDays == null ? 0.0 : workDays;
        double safeAllowance = allowance == null ? 0.0 : allowance;
        double safeDeduction = deduction == null ? 0.0 : deduction;
        return (safeBaseSalary / STANDARD_MONTHLY_WORK_DAYS) * safeWorkDays + safeAllowance - safeDeduction;
    }
}
