package com.example.QuanLyLuong.common;

public enum CompensationItemType {
    ALLOWANCE("Phu cap"),
    BONUS("Thuong"),
    DEDUCTION("Khau tru");

    private final String label;

    CompensationItemType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}