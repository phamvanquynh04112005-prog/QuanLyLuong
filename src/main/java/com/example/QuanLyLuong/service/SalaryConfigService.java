package com.example.QuanLyLuong.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.SalaryConfig;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.SalaryConfigRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SalaryConfigService {

    private final SalaryConfigRepository salaryConfigRepository;
    private final EmployeeRepository employeeRepository;

    public SalaryConfig save(Long employeeId, Double allowance, Double deduction, String description, LocalDate effectiveDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien co ID: " + employeeId));

        SalaryConfig config = new SalaryConfig();
        config.setEmployee(employee);
        config.setAllowance(allowance == null ? 0.0 : allowance);
        config.setDeduction(deduction == null ? 0.0 : deduction);
        config.setDescription(description);
        config.setEffectiveDate(effectiveDate == null ? LocalDate.now() : effectiveDate);
        return salaryConfigRepository.save(config);
    }

    @Transactional(readOnly = true)
    public List<SalaryConfig> findHistoryByEmployee(Long employeeId) {
        return salaryConfigRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public List<SalaryConfig> findLatestForAllEmployees() {
        List<SalaryConfig> latestConfigs = new ArrayList<>();
        for (Employee employee : employeeRepository.findAllByOrderByFullNameAsc()) {
            latestConfigs.add(getLatestConfigOrDefault(employee.getId()));
        }
        return latestConfigs;
    }

    @Transactional(readOnly = true)
    public SalaryConfig getLatestConfigOrDefault(Long employeeId) {
        return salaryConfigRepository.findTopByEmployeeIdOrderByEffectiveDateDesc(employeeId)
                .orElseGet(() -> createEmptyConfig(employeeId));
    }

    @Transactional(readOnly = true)
    public SalaryConfig getEffectiveConfig(Long employeeId, YearMonth yearMonth) {
        LocalDate endOfMonth = yearMonth.atEndOfMonth();
        return salaryConfigRepository
                .findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(employeeId, endOfMonth)
                .orElseGet(() -> createEmptyConfig(employeeId));
    }

    private SalaryConfig createEmptyConfig(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien co ID: " + employeeId));
        SalaryConfig config = new SalaryConfig();
        config.setEmployee(employee);
        config.setAllowance(0.0);
        config.setDeduction(0.0);
        config.setDescription("Mac dinh chua co phu cap va khau tru");
        config.setEffectiveDate(LocalDate.now());
        return config;
    }
}
