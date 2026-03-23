package com.example.QuanLyLuong.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.dto.TimesheetSummary;
import com.example.QuanLyLuong.entity.Employee;
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

    private static final int STANDARD_WORK_DAYS = 26;

    private final TimesheetRepository timesheetRepository;
    private final EmployeeRepository employeeRepository;

    public Timesheet saveOrUpdate(Long employeeId, Integer month, Integer year, Integer workDays, Integer leaveDays, String note) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien co ID: " + employeeId));

        Timesheet timesheet = timesheetRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseGet(Timesheet::new);

        timesheet.setEmployee(employee);
        timesheet.setMonth(month);
        timesheet.setYear(year);
        timesheet.setWorkDays(workDays);
        timesheet.setLeaveDays(leaveDays);
        timesheet.setNote(note);
        return timesheetRepository.save(timesheet);
    }

    public int initForAllEmployees(Integer month, Integer year) {
        int createdCount = 0;
        for (Employee employee : employeeRepository.findByStatusOrderByFullNameAsc(EmployeeStatus.ACTIVE)) {
            boolean exists = timesheetRepository
                    .findByEmployeeIdAndMonthAndYear(employee.getId(), month, year)
                    .isPresent();
            if (!exists) {
                Timesheet timesheet = new Timesheet();
                timesheet.setEmployee(employee);
                timesheet.setMonth(month);
                timesheet.setYear(year);
                timesheet.setWorkDays(0);
                timesheet.setLeaveDays(0);
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
                .filter(timesheet -> timesheet.getWorkDays() != null && timesheet.getWorkDays() >= STANDARD_WORK_DAYS)
                .count();

        return TimesheetSummary.builder()
                .totalEmployees(timesheets.size())
                .fullAttendanceCount(fullAttendance)
                .averageWorkDays(Math.round(averageWorkDays * 10.0) / 10.0)
                .standardWorkDays(YearMonth.of(year, month).lengthOfMonth())
                .build();
    }
}
