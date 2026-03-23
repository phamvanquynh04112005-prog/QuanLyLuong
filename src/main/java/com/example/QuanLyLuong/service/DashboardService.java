package com.example.QuanLyLuong.service;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.dto.DashboardStats;
import com.example.QuanLyLuong.repository.DepartmentRepository;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.PayrollRepository;
import com.example.QuanLyLuong.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PayrollRepository payrollRepository;

    public DashboardStats buildStats(Integer month, Integer year) {
        return DashboardStats.builder()
                .totalEmployees(employeeRepository.count())
                .activeEmployees(employeeRepository.countByStatus(EmployeeStatus.ACTIVE))
                .totalDepartments(departmentRepository.count())
                .totalUsers(userRepository.count())
                .payrollCount(payrollRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year).size())
                .pendingPayrollCount(payrollRepository.countByPaymentStatus(PaymentStatus.PENDING))
                .payrollTotal(defaultValue(payrollRepository.sumTotalSalaryByMonthAndYear(month, year)))
                .build();
    }

    private double defaultValue(Double value) {
        return value == null ? 0.0 : value;
    }
}
