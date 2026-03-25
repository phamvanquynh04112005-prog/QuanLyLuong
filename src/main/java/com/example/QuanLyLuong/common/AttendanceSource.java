package com.example.QuanLyLuong.common;

public enum AttendanceSource {
    MANUAL("Nh\u1eadp tay"),
    EXCEL_IMPORT("Import Excel"),
    MACHINE_IMPORT("Log m\u00e1y ch\u1ea5m c\u00f4ng");

    private final String label;

    AttendanceSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
