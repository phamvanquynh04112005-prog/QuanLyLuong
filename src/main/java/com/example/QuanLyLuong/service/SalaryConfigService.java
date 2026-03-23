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
        return save(
                employeeId,
                allowance,
                deduction,
                26,
                8.0,
                1.5,
                2.0,
                3.0,
                0.08,
                0.015,
                0.01,
                0.05,
                11000000.0,
                description,
                effectiveDate
        );
    }

    public SalaryConfig save(Long employeeId,
                             Double allowance,
                             Double deduction,
                             Integer standardWorkDays,
                             Double standardWorkHoursPerDay,
                             Double overtimeWeekdayMultiplier,
                             Double overtimeWeekendMultiplier,
                             Double overtimeHolidayMultiplier,
                             Double socialInsuranceRate,
                             Double healthInsuranceRate,
                             Double unemploymentInsuranceRate,
                             Double personalIncomeTaxRate,
                             Double personalDeduction,
                             String description,
                             LocalDate effectiveDate) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien co ID: " + employeeId));

        SalaryConfig config = new SalaryConfig();
        config.setEmployee(employee);
        config.setAllowance(allowance == null ? 0.0 : Math.max(0.0, allowance));
        config.setDeduction(deduction == null ? 0.0 : Math.max(0.0, deduction));
        config.setStandardWorkDays(standardWorkDays == null || standardWorkDays <= 0 ? 26 : standardWorkDays);
        config.setStandardWorkHoursPerDay(standardWorkHoursPerDay == null || standardWorkHoursPerDay <= 0 ? 8.0 : standardWorkHoursPerDay);
        config.setOvertimeWeekdayMultiplier(overtimeWeekdayMultiplier == null || overtimeWeekdayMultiplier <= 0 ? 1.5 : overtimeWeekdayMultiplier);
        config.setOvertimeWeekendMultiplier(overtimeWeekendMultiplier == null || overtimeWeekendMultiplier <= 0 ? 2.0 : overtimeWeekendMultiplier);
        config.setOvertimeHolidayMultiplier(overtimeHolidayMultiplier == null || overtimeHolidayMultiplier <= 0 ? 3.0 : overtimeHolidayMultiplier);
        config.setSocialInsuranceRate(socialInsuranceRate == null || socialInsuranceRate < 0 ? 0.08 : socialInsuranceRate);
        config.setHealthInsuranceRate(healthInsuranceRate == null || healthInsuranceRate < 0 ? 0.015 : healthInsuranceRate);
        config.setUnemploymentInsuranceRate(unemploymentInsuranceRate == null || unemploymentInsuranceRate < 0 ? 0.01 : unemploymentInsuranceRate);
        config.setPersonalIncomeTaxRate(personalIncomeTaxRate == null || personalIncomeTaxRate < 0 ? 0.05 : personalIncomeTaxRate);
        config.setPersonalDeduction(personalDeduction == null || personalDeduction < 0 ? 11000000.0 : personalDeduction);
        config.setDescription(description == null ? null : description.trim());
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
        config.setStandardWorkDays(26);
        config.setStandardWorkHoursPerDay(8.0);
        config.setOvertimeWeekdayMultiplier(1.5);
        config.setOvertimeWeekendMultiplier(2.0);
        config.setOvertimeHolidayMultiplier(3.0);
        config.setSocialInsuranceRate(0.08);
        config.setHealthInsuranceRate(0.015);
        config.setUnemploymentInsuranceRate(0.01);
        config.setPersonalIncomeTaxRate(0.05);
        config.setPersonalDeduction(11000000.0);
        config.setDescription("Mac dinh chua co cau hinh luong nang cao");
        config.setEffectiveDate(LocalDate.now());
        return config;
    }
}