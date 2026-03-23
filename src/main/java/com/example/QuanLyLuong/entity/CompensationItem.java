package com.example.QuanLyLuong.entity;

import java.time.LocalDate;

import com.example.QuanLyLuong.common.CompensationItemType;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "compensation_items")
public class CompensationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompensationItemType componentType;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Double amount = 0.0;

    @Column(nullable = false)
    private Boolean taxable = Boolean.FALSE;

    @Column(nullable = false)
    private Boolean recurring = Boolean.TRUE;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(nullable = false)
    private LocalDate effectiveDate = LocalDate.now();

    private LocalDate endDate;

    @Column(length = 255)
    private String note;
}