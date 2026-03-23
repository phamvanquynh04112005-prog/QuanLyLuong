package com.example.QuanLyLuong.repository;

import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.entity.Timesheet;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {

    Optional<Timesheet> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    List<Timesheet> findByMonthAndYearOrderByEmployeeFullNameAsc(Integer month, Integer year);

    List<Timesheet> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);
}
