package com.example.QuanLyLuong.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.entity.AttendanceLog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    Optional<AttendanceLog> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<AttendanceLog> findByAttendanceDateBetweenOrderByAttendanceDateAscEmployeeFullNameAsc(LocalDate startDate, LocalDate endDate);

    List<AttendanceLog> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(Long employeeId, LocalDate startDate, LocalDate endDate);

    long countByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);
}