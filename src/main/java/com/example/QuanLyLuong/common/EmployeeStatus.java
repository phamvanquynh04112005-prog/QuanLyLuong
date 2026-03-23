package com.example.QuanLyLuong.common;

public enum EmployeeStatus {
    ACTIVE("Dang lam viec"),
    INACTIVE("Da nghi");

    private final String label;

    EmployeeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
