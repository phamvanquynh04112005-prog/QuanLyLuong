package com.example.QuanLyLuong.common;

public enum Role {
    ROLE_ADMIN("Admin"),
    ROLE_HR("Nhân sự"),
    ROLE_ACCOUNTANT("Kế toán"),
    ROLE_EMPLOYEE("Nhân viên");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

