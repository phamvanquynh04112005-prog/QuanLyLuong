package com.example.QuanLyLuong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
        name = "timesheets",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"employee_id", "month_value", "year_value"})
        }
)
public class Timesheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "month_value", nullable = false)
    private Integer month;

    @Column(name = "year_value", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer workDays = 0;

    @Column(nullable = false)
    private Integer leaveDays = 0;

    @Column(length = 255)
    private String note;

    @OneToOne(mappedBy = "timesheet")
    private Payroll payroll;
}
