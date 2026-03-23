package com.example.QuanLyLuong.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.QuanLyLuong.common.EmployeeStatus;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(unique = true, length = 30)
    private String employeeCode;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 100)
    private String position;

    @Column(nullable = false)
    private Double baseSalary = 0.0;

    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @OneToMany(mappedBy = "employee")
    private List<SalaryConfig> salaryConfigs = new ArrayList<>();

    @OneToMany(mappedBy = "employee")
    private List<Timesheet> timesheets = new ArrayList<>();

    @OneToMany(mappedBy = "employee")
    private List<Payroll> payrolls = new ArrayList<>();

    @OneToMany(mappedBy = "employee")
    private List<AttendanceLog> attendanceLogs = new ArrayList<>();

    @OneToMany(mappedBy = "employee")
    private List<CompensationItem> compensationItems = new ArrayList<>();

    @OneToOne(mappedBy = "employee")
    private User user;
}