package com.example.QuanLyLuong.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentReportItem {
    private final String departmentName;
    private final double totalSalary;
    private final double percentage;
}
