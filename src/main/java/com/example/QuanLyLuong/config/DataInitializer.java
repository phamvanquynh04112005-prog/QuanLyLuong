package com.example.QuanLyLuong.config;

import java.time.LocalDate;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.entity.Department;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.repository.DepartmentRepository;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.SalaryConfigRepository;
import com.example.QuanLyLuong.repository.UserRepository;
import com.example.QuanLyLuong.service.SalaryConfigService;
import com.example.QuanLyLuong.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryConfigRepository salaryConfigRepository;
    private final UserRepository userRepository;
    private final SalaryConfigService salaryConfigService;
    private final UserService userService;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        Department hr = ensureDepartment("Nhan su", "Tran Thi Lan");
        Department finance = ensureDepartment("Ke toan", "Nguyen Van Binh");
        Department it = ensureDepartment("Cong nghe thong tin", "Le Hoang Minh");

        Employee adminEmployee = ensureEmployee("EMP0001", "admin@quanlyluong.local", "Nguyen Van Admin", "Giam sat he thong", hr, 25000000d, LocalDate.of(2023, 1, 10));
        Employee hrEmployee = ensureEmployee("EMP0002", "hr01@quanlyluong.local", "Tran Thi HR", "Chuyen vien nhan su", hr, 18000000d, LocalDate.of(2023, 4, 5));
        Employee employee = ensureEmployee("EMP0003", "nv001@quanlyluong.local", "Le Van Nhan Vien", "Lap trinh vien", it, 15000000d, LocalDate.of(2024, 2, 1));
        ensureEmployee("EMP0004", "kt01@quanlyluong.local", "Pham Thi Ke Toan", "Ke toan tong hop", finance, 17000000d, LocalDate.of(2023, 8, 12));

        ensureSalaryConfig(adminEmployee.getId(), 2500000d, 800000d, "Phu cap quan tri");
        ensureSalaryConfig(hrEmployee.getId(), 1500000d, 500000d, "Phu cap nhan su");
        ensureSalaryConfig(employee.getId(), 1000000d, 400000d, "Phu cap ky thuat");

        ensureUser(adminEmployee.getId(), "admin", "Admin@123", Role.ROLE_ADMIN);
        ensureUser(hrEmployee.getId(), "hr01", "Hr@123", Role.ROLE_HR);
        ensureUser(employee.getId(), "nv001", "Nv@123", Role.ROLE_EMPLOYEE);
    }

    private Department ensureDepartment(String name, String managerName) {
        return departmentRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setName(name);
                    department.setManagerName(managerName);
                    return departmentRepository.save(department);
                });
    }

    private Employee ensureEmployee(String employeeCode,
                                    String email,
                                    String fullName,
                                    String position,
                                    Department department,
                                    Double baseSalary,
                                    LocalDate joinDate) {
        return employeeRepository.findByEmailIgnoreCase(email)
                .map(existing -> {
                    if (existing.getEmployeeCode() == null || existing.getEmployeeCode().isBlank()) {
                        existing.setEmployeeCode(employeeCode);
                    }
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
                    employee.setStatus(EmployeeStatus.ACTIVE);
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
        if (userRepository.findByUsername(username).isEmpty()) {
            userService.createUser(employeeId, username, password, role, true);
        }
    }
}