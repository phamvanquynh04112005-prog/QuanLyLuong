package com.example.QuanLyLuong.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.dto.TimesheetSummary;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.SalaryConfig;
import com.example.QuanLyLuong.entity.Timesheet;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.TimesheetRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TimesheetService {

    private static final int DEFAULT_STANDARD_WORK_DAYS = 26;
    private static final double DEFAULT_STANDARD_WORK_HOURS = 8.0;

    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryConfigService salaryConfigService;

    public Timesheet saveOrUpdate(Long employeeId, Integer month, Integer year, Integer workDays, Integer leaveDays, String note) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien co ID: " + employeeId));
        YearMonth yearMonth = YearMonth.of(year, month);
        SalaryConfig salaryConfig = salaryConfigService.getEffectiveConfig(employeeId, yearMonth);
        Timesheet timesheet = timesheetRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseGet(Timesheet::new);

        int resolvedWorkDays = workDays == null ? 0 : Math.max(0, workDays);
        int resolvedLeaveDays = leaveDays == null ? 0 : Math.max(0, leaveDays);
        int standardWorkDays = positiveInteger(salaryConfig.getStandardWorkDays(), DEFAULT_STANDARD_WORK_DAYS);
        double standardWorkHoursPerDay = positiveDouble(salaryConfig.getStandardWorkHoursPerDay(), DEFAULT_STANDARD_WORK_HOURS);
        int importedLogCount = timesheet.getImportedLogCount() == null ? 0 : timesheet.getImportedLogCount();

        timesheet.setEmployee(employee);
        timesheet.setMonth(month);
        timesheet.setYear(year);
        timesheet.setWorkDays(resolvedWorkDays);
        timesheet.setLeaveDays(resolvedLeaveDays);
        timesheet.setAbsentDays(Math.max(0, standardWorkDays - resolvedWorkDays - resolvedLeaveDays));
        timesheet.setNote(note == null || note.isBlank() ? null : note.trim());
        timesheet.setImportedLogCount(importedLogCount);
        if (importedLogCount == 0) {
            timesheet.setRegularHours(round2(resolvedWorkDays * standardWorkHoursPerDay));
            timesheet.setOvertimeWeekdayHours(0.0);
            timesheet.setOvertimeWeekendHours(0.0);
            timesheet.setOvertimeHolidayHours(0.0);
        }
        return timesheetRepository.save(timesheet);
    }

    public int initForAllEmployees(Integer month, Integer year) {
        int createdCount = 0;
        YearMonth yearMonth = YearMonth.of(year, month);
        for (Employee employee : employeeRepository.findByStatusOrderByFullNameAsc(EmployeeStatus.ACTIVE)) {
            boolean exists = timesheetRepository
                    .findByEmployeeIdAndMonthAndYear(employee.getId(), month, year)
                    .isPresent();
            if (!exists) {
                SalaryConfig salaryConfig = salaryConfigService.getEffectiveConfig(employee.getId(), yearMonth);
                Timesheet timesheet = new Timesheet();
                timesheet.setEmployee(employee);
                timesheet.setMonth(month);
                timesheet.setYear(year);
                timesheet.setWorkDays(0);
                timesheet.setLeaveDays(0);
                timesheet.setAbsentDays(positiveInteger(salaryConfig.getStandardWorkDays(), DEFAULT_STANDARD_WORK_DAYS));
                timesheet.setRegularHours(0.0);
                timesheet.setOvertimeWeekdayHours(0.0);
                timesheet.setOvertimeWeekendHours(0.0);
                timesheet.setOvertimeHolidayHours(0.0);
                timesheet.setImportedLogCount(0);
                timesheetRepository.save(timesheet);
                createdCount++;
            }
        }
        return createdCount;
    }

    @Transactional(readOnly = true)
    public Optional<Timesheet> findByEmployeeAndMonth(Long employeeId, Integer month, Integer year) {
        return timesheetRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year);
    }

    @Transactional(readOnly = true)
    public List<Timesheet> findAllByMonth(Integer month, Integer year) {
        return timesheetRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year);
    }

    @Transactional(readOnly = true)
    public List<Timesheet> findByEmployee(Long employeeId) {
        return timesheetRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId);
    }

    public void delete(Long id) {
        timesheetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TimesheetSummary getMonthSummary(Integer month, Integer year) {
        List<Timesheet> timesheets = findAllByMonth(month, year);
        double averageWorkDays = timesheets.stream()
                .map(Timesheet::getWorkDays)
                .filter(workDays -> workDays != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        long fullAttendance = timesheets.stream()
                .filter(timesheet -> timesheet.getWorkDays() != null && timesheet.getWorkDays() >= DEFAULT_STANDARD_WORK_DAYS)
                .count();
        double totalRegularHours = timesheets.stream().mapToDouble(timesheet -> safeDouble(timesheet.getRegularHours())).sum();
        double totalOvertimeHours = timesheets.stream().mapToDouble(timesheet ->
                safeDouble(timesheet.getOvertimeWeekdayHours())
                        + safeDouble(timesheet.getOvertimeWeekendHours())
                        + safeDouble(timesheet.getOvertimeHolidayHours())
        ).sum();

        return TimesheetSummary.builder()
                .totalEmployees(timesheets.size())
                .fullAttendanceCount(fullAttendance)
                .averageWorkDays(Math.round(averageWorkDays * 10.0) / 10.0)
                .standardWorkDays(YearMonth.of(year, month).lengthOfMonth())
                .totalRegularHours(round2(totalRegularHours))
                .totalOvertimeHours(round2(totalOvertimeHours))
                .build();
    }

    private int positiveInteger(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private double positiveDouble(Double value, double fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}