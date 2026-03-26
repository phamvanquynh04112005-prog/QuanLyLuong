package com.example.QuanLyLuong.config;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.entity.Department;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.repository.DepartmentRepository;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.PayrollRepository;
import com.example.QuanLyLuong.repository.SalaryConfigRepository;
import com.example.QuanLyLuong.repository.TimesheetRepository;
import com.example.QuanLyLuong.repository.UserRepository;
import com.example.QuanLyLuong.service.PayrollService;
import com.example.QuanLyLuong.service.SalaryConfigService;
import com.example.QuanLyLuong.service.TimesheetService;
import com.example.QuanLyLuong.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollRepository payrollRepository;
    private final TimesheetRepository timesheetRepository;
    private final SalaryConfigRepository salaryConfigRepository;
    private final UserRepository userRepository;
    private final PayrollService payrollService;
    private final SalaryConfigService salaryConfigService;
    private final TimesheetService timesheetService;
    private final UserService userService;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        Department hr = ensureDepartment("Nh\u00e2n s\u1ef1", "Tr\u1ea7n Th\u1ecb Lan", "Nhan su");
        Department finance = ensureDepartment("K\u1ebf to\u00e1n", "Nguy\u1ec5n V\u0103n B\u00ecnh", "Ke toan");
        Department it = ensureDepartment("C\u00f4ng ngh\u1ec7 th\u00f4ng tin", "L\u00ea Ho\u00e0ng Minh", "Cong nghe thong tin");

        Employee adminEmployee = ensureEmployee("EMP0001", "admin@quanlyluong.local", "Nguy\u1ec5n V\u0103n Admin", "Gi\u00e1m s\u00e1t h\u1ec7 th\u1ed1ng", hr, 25000000d, LocalDate.of(2023, 1, 10), 1);
        Employee hrEmployee = ensureEmployee("EMP0002", "hr01@quanlyluong.local", "Tr\u1ea7n Th\u1ecb HR", "Chuy\u00ean vi\u00ean nh\u00e2n s\u1ef1", hr, 18000000d, LocalDate.of(2023, 4, 5), 0);
        Employee employee = ensureEmployee("EMP0003", "nv001@quanlyluong.local", "L\u00ea V\u0103n Nh\u00e2n Vi\u00ean", "L\u1eadp tr\u00ecnh vi\u00ean", it, 15000000d, LocalDate.of(2024, 2, 1), 2);
        Employee financeEmployee = ensureEmployee("EMP0004", "kt01@quanlyluong.local", "Ph\u1ea1m Th\u1ecb K\u1ebf To\u00e1n", "K\u1ebf to\u00e1n t\u1ed5ng h\u1ee3p", finance, 17000000d, LocalDate.of(2023, 8, 12), 1);

        ensureSalaryConfig(adminEmployee.getId(), 2500000d, 800000d, "Ph\u1ee5 c\u1ea5p qu\u1ea3n tr\u1ecb");
        ensureSalaryConfig(hrEmployee.getId(), 1500000d, 500000d, "Ph\u1ee5 c\u1ea5p nh\u00e2n s\u1ef1");
        ensureSalaryConfig(employee.getId(), 1000000d, 400000d, "Ph\u1ee5 c\u1ea5p k\u1ef9 thu\u1eadt");

        ensureUser(adminEmployee.getId(), "admin", "Admin@123", Role.ROLE_SYSTEM_ADMIN);
        ensureUser(hrEmployee.getId(), "hr01", "Hr@123", Role.ROLE_HR);
        ensureUser(financeEmployee.getId(), "kt01", "Kt@123", Role.ROLE_ACCOUNTANT);
        ensureUser(employee.getId(), "nv001", "Nv@123", Role.ROLE_EMPLOYEE);

        seedDemoTimesheetsAndPayrolls();
    }

    private Department ensureDepartment(String name, String managerName, String... legacyNames) {
        Department department = departmentRepository.findByNameIgnoreCase(name).orElse(null);
        if (department == null && legacyNames != null) {
            for (String legacyName : legacyNames) {
                department = departmentRepository.findByNameIgnoreCase(legacyName).orElse(null);
                if (department != null) {
                    break;
                }
            }
        }

        if (department == null) {
            department = new Department();
        }

        department.setName(name);
        department.setManagerName(managerName);
        return departmentRepository.save(department);
    }

    private Employee ensureEmployee(String employeeCode,
                                    String email,
                                    String fullName,
                                    String position,
                                    Department department,
                                    Double baseSalary,
                                    LocalDate joinDate,
                                    Integer dependentCount) {
        return employeeRepository.findByEmailIgnoreCase(email)
                .map(existing -> {
                    existing.setEmployeeCode(employeeCode);
                    existing.setFullName(fullName);
                    existing.setPosition(position);
                    existing.setDepartment(department);
                    existing.setBaseSalary(baseSalary);
                    existing.setJoinDate(joinDate);
                    existing.setDependentCount(dependentCount == null ? 0 : dependentCount);
                    existing.setStatus(EmployeeStatus.ACTIVE);
                    existing.setInactiveSince(null);
                    return employeeRepository.save(existing);
                })
                .orElseGet(() -> {
                    Employee employee = new Employee();
                    employee.setEmployeeCode(employeeCode);
                    employee.setEmail(email);
                    employee.setFullName(fullName);
                    employee.setPosition(position);
                    employee.setDepartment(department);
                    employee.setBaseSalary(baseSalary);
                    employee.setJoinDate(joinDate);
                    employee.setDependentCount(dependentCount == null ? 0 : dependentCount);
                    employee.setStatus(EmployeeStatus.ACTIVE);
                    employee.setInactiveSince(null);
                    return employeeRepository.save(employee);
                });
    }

    private void ensureSalaryConfig(Long employeeId, Double allowance, Double deduction, String description) {
        boolean exists = salaryConfigRepository.findTopByEmployeeIdOrderByEffectiveDateDesc(employeeId).isPresent();
        if (!exists) {
            salaryConfigService.save(employeeId, allowance, deduction, description, LocalDate.now().minusMonths(1));
        }
    }

    private void ensureUser(Long employeeId, String username, String password, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(existing -> {
            userService.updateUser(existing.getId(), employeeId, username, password, role, true);
        }, () -> userService.createUser(employeeId, username, password, role, true));
    }

    private void seedDemoTimesheetsAndPayrolls() {
        if (timesheetRepository.count() > 0 || payrollRepository.count() > 0) {
            return;
        }
        List<Employee> activeEmployees = employeeRepository.findByStatusOrderByFullNameAsc(EmployeeStatus.ACTIVE);
        if (activeEmployees.isEmpty()) {
            return;
        }

        YearMonth current = YearMonth.now();
        for (int offset = 0; offset < 6; offset++) {
            YearMonth period = current.minusMonths(offset);
            int month = period.getMonthValue();
            int year = period.getYear();
            int cycleOffset = offset;
            YearMonth cyclePeriod = period;

            List<Payroll> existingPayrolls = payrollRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year);
            List<Payroll> periodPayrolls;

            if (existingPayrolls.size() < activeEmployees.size()) {
                for (Employee employee : activeEmployees) {
                    boolean hasExistingTimesheet = timesheetService
                            .findByEmployeeAndMonth(employee.getId(), month, year)
                            .isPresent();
                    if (hasExistingTimesheet) {
                        continue;
                    }
                    int workDays = 20 + Math.floorMod((int) (employee.getId() + offset), 7);
                    int leaveDays = Math.floorMod((int) (employee.getId() + offset), 3);
                    if (workDays + leaveDays > 26) {
                        leaveDays = Math.max(0, 26 - workDays);
                    }
                    timesheetService.saveOrUpdate(
                            employee.getId(),
                            month,
                            year,
                            workDays,
                            leaveDays,
                            "D\u1eef li\u1ec7u m\u1eabu t\u1ef1 \u0111\u1ed9ng cho b\u1ed9 l\u1ecdc v\u00e0 dashboard"
                    );
                }

                runAsAccountant(() -> {
                    List<Payroll> generatedPayrolls = payrollService.calculateForAll(month, year);
                    for (int index = 0; index < generatedPayrolls.size(); index++) {
                        Payroll payroll = generatedPayrolls.get(index);
                        boolean shouldMarkPaid = Math.floorMod(index + cycleOffset, 3) != 0;
                        if (shouldMarkPaid) {
                            payroll.setPaymentStatus(PaymentStatus.PAID);
                            payroll.setPaymentDate(cyclePeriod.atDay(Math.min(28, cyclePeriod.lengthOfMonth())));
                        } else {
                            payroll.setPaymentStatus(PaymentStatus.PENDING);
                            payroll.setPaymentDate(null);
                        }
                    }
                    payrollRepository.saveAll(generatedPayrolls);
                });
                periodPayrolls = payrollRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year);
            } else {
                periodPayrolls = existingPayrolls;
            }

            applySyntheticCategoryData(periodPayrolls, period);
            payrollRepository.saveAll(periodPayrolls);
        }
    }

    private void applySyntheticCategoryData(List<Payroll> payrolls, YearMonth period) {
        for (Payroll payroll : payrolls) {
            long employeeId = payroll.getEmployee() == null || payroll.getEmployee().getId() == null
                    ? 0L
                    : payroll.getEmployee().getId();
            long seed = employeeId * 17L + period.getMonthValue() * 13L + period.getYear();

            double baseCost = safe(payroll.getBaseSalaryAmount()) + safe(payroll.getOvertimePay());
            double allowanceRate = 0.05 + (Math.floorMod(seed, 6) * 0.01);
            double bonusRate = 0.02 + (Math.floorMod(seed, 5) * 0.015);
            if (period.getMonthValue() % 3 == 0) {
                bonusRate += 0.04;
            }
            if (period.equals(YearMonth.now())) {
                bonusRate += 0.08;
            }
            double insuranceRate = 0.095 + (Math.floorMod(seed, 3) * 0.003);
            double benefitsRate = 0.01 + (Math.floorMod(seed, 4) * 0.0075);

            double allowance = round2(baseCost * allowanceRate);
            double bonus = round2(baseCost * bonusRate);
            double insurance = round2(baseCost * insuranceRate);
            double benefits = round2(baseCost * benefitsRate);

            double grossSalary = round2(baseCost + allowance + bonus);
            double taxableIncome = Math.max(0.0, grossSalary - insurance - benefits - 11000000.0);
            double tax = round2(taxableIncome * 0.05);
            double actualSalary = round2(Math.max(0.0, grossSalary - insurance - tax - benefits));

            payroll.setTotalAllowance(allowance);
            payroll.setTotalBonus(bonus);
            payroll.setInsuranceAmount(insurance);
            payroll.setOtherDeductionAmount(benefits);
            payroll.setGrossSalary(grossSalary);
            payroll.setTaxAmount(tax);
            payroll.setActualSalary(actualSalary);
        }
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void runAsAccountant(Runnable action) {
        Authentication previous = SecurityContextHolder.getContext().getAuthentication();
        Authentication accountant = new UsernamePasswordAuthenticationToken(
                "seed-runner",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ACCOUNTANT"))
        );
        SecurityContextHolder.getContext().setAuthentication(accountant);
        try {
            action.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
