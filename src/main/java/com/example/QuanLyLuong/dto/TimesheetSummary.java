package com.example.QuanLyLuong.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimesheetSummary {
    private final long totalEmployees;
    private final long fullAttendanceCount;
    private final double averageWorkDays;
    private final int standardWorkDays;
    private final double totalRegularHours;
    private final double totalOvertimeHours;
}
