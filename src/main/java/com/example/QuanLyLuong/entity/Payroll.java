package com.example.QuanLyLuong.entity;

import java.time.LocalDate;

import com.example.QuanLyLuong.common.PaymentStatus;

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
        name = "payrolls",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"employee_id", "month_value", "year_value"})
        }
)
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id")
    private Timesheet timesheet;

    @Column(name = "month_value", nullable = false)
    private Integer month;

    @Column(name = "year_value", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Double baseSalaryAmount = 0.0;

    @Column(nullable = false)
    private Double overtimePay = 0.0;

    @Column(nullable = false)
    private Double totalAllowance = 0.0;

    @Column(nullable = false)
    private Double totalBonus = 0.0;

    @Column(nullable = false)
    private Double insuranceAmount = 0.0;

    @Column(nullable = false)
    private Double dependentDeductionAmount = 0.0;

    @Column(nullable = false)
    private Double taxableIncome = 0.0;

    @Column(nullable = false)
    private Double taxAmount = 0.0;

    @Column(nullable = false)
    private Double otherDeductionAmount = 0.0;

    @Column(nullable = false)
    private Double grossSalary = 0.0;

    @Column(nullable = false)
    private Double actualSalary = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private LocalDate paymentDate;
}