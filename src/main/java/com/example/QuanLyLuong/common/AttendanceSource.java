package com.example.QuanLyLuong.common;

public enum AttendanceSource {
    MANUAL("Nhap tay"),
    EXCEL_IMPORT("Import Excel"),
    MACHINE_IMPORT("Log may cham cong");

    private final String label;

    AttendanceSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}