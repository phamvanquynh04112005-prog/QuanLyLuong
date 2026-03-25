package com.example.QuanLyLuong.common;

public enum EmployeeStatus {
    ACTIVE("Đang làm việc"),
    INACTIVE("Đã nghỉ");

    private final String label;

    EmployeeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

