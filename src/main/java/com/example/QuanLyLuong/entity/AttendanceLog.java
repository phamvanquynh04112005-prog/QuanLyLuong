package com.example.QuanLyLuong.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.QuanLyLuong.common.AttendanceSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "attendance_logs",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"employee_id", "attendance_date"})
        }
)
public class AttendanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    @Column(nullable = false)
    private Double regularHours = 0.0;

    @Column(nullable = false)
    private Double overtimeWeekdayHours = 0.0;

    @Column(nullable = false)
    private Double overtimeWeekendHours = 0.0;

    @Column(nullable = false)
    private Double overtimeHolidayHours = 0.0;

    @Column(nullable = false)
    private Integer lateMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceSource source = AttendanceSource.MANUAL;

    @Column(length = 80)
    private String machineCode;

    @Column(length = 255)
    private String note;
}