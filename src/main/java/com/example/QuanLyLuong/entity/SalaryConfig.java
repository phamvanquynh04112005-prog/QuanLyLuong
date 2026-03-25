package com.example.QuanLyLuong.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "salary_configs")
public class SalaryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Double allowance = 0.0;

    @Column(nullable = false)
    private Double deduction = 0.0;

    @Column(nullable = false)
    private Integer standardWorkDays = 26;

    @Column(nullable = false)
    private Double standardWorkHoursPerDay = 8.0;

    @Column(nullable = false)
    private Double overtimeWeekdayMultiplier = 1.5;

    @Column(nullable = false)
    private Double overtimeWeekendMultiplier = 2.0;

    @Column(nullable = false)
    private Double overtimeHolidayMultiplier = 3.0;

    @Column(nullable = false)
    private Double socialInsuranceRate = 0.08;

    @Column(nullable = false)
    private Double healthInsuranceRate = 0.015;

    @Column(nullable = false)
    private Double unemploymentInsuranceRate = 0.01;

    @Column(nullable = false)
    private Double personalIncomeTaxRate = 0.05;

    @Column(nullable = false)
    private Double personalDeduction = 11000000.0;

    @Column(nullable = false)
    private Double dependentDeductionPerPerson = 4400000.0;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDate effectiveDate = LocalDate.now();
}