package com.example.QuanLyLuong.dto;

import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountProvisionResult {

    private final Employee employee;
    private final User user;
    private final String rawPassword;
    private final boolean emailSent;
    private final String emailError;
}
