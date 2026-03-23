package com.example.QuanLyLuong.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollReportDashboard {
    private final List<DepartmentReportItem> departmentItems;
    private final List<MonthlyPayrollTrendItem> monthlyTrend;
    private final double selectedMonthTotal;
    private final double periodTotal;
    private final double averageMonthlyTotal;
    private final long periodPayrollCount;
    private final long periodPaidCount;
    private final long periodPendingCount;
    private final double maxTrendValue;
    private final String topDepartmentName;
    private final double topDepartmentTotal;
    private final double paidRatio;
}