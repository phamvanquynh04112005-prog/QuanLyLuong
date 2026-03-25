package com.example.QuanLyLuong.common;

public enum AttendanceSource {
    MANUAL("Nhập tay"),
    EXCEL_IMPORT("Import Excel"),
    MACHINE_IMPORT("Log máy chấm công");

    private final String label;

    AttendanceSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
