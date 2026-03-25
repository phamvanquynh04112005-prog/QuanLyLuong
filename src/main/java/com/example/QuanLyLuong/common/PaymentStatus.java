package com.example.QuanLyLuong.common;

public enum PaymentStatus {
    PENDING("Chưa chi"),
    PAID("Đã chi");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

