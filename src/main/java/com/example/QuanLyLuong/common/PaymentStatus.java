package com.example.QuanLyLuong.common;

public enum PaymentStatus {
    PENDING("Chua chi"),
    PAID("Da chi");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
