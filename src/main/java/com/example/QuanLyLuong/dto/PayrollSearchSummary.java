package com.example.QuanLyLuong.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollSearchSummary {
    private final long resultCount;
    private final double totalSalary;
    private final double averageSalary;
    private final long paidCount;
    private final long pendingCount;
    private final double minSalary;
    private final double maxSalary;
}