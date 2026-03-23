package com.example.QuanLyLuong.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyPayrollTrendItem {
    private final Integer month;
    private final Integer year;
    private final String label;
    private final double totalSalary;
    private final long payrollCount;
    private final long paidCount;
    private final long pendingCount;
    private final double paidSalary;
    private final double pendingSalary;
}