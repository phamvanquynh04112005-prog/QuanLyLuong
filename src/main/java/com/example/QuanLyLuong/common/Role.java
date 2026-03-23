package com.example.QuanLyLuong.common;

public enum Role {
    ROLE_ADMIN("Admin"),
    ROLE_HR("Nhan su"),
    ROLE_EMPLOYEE("Nhan vien");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
