package com.example.QuanLyLuong.dto;

import com.example.QuanLyLuong.common.PaymentStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PayrollSearchCriteria {
    private Integer month;
    private Integer year;
    private Long departmentId;
    private PaymentStatus paymentStatus;
    private String keyword;
    private Double minSalary;
    private Double maxSalary;
}