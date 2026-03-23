package com.example.QuanLyLuong.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStats {
    private final long totalEmployees;
    private final long activeEmployees;
    private final long totalDepartments;
    private final long totalUsers;
    private final long payrollCount;
    private final long pendingPayrollCount;
    private final double payrollTotal;
}
