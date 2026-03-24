package com.example.QuanLyLuong.service;

import java.util.List;

import com.example.QuanLyLuong.entity.Timesheet;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.TimesheetRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;

    @Transactional(readOnly = true)
    public List<Timesheet> findAllByMonth(Integer month, Integer year) {
        return timesheetRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(month, year);
    }

    @Transactional(readOnly = true)
    public List<Timesheet> findByEmployee(Long employeeId) {
        return timesheetRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public Timesheet findById(Long id) {
        return timesheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bang cong co ID: " + id));
    }

    public void delete(Long id) {
        timesheetRepository.deleteById(id);
    }

}
