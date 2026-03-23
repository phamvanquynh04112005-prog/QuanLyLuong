package com.example.QuanLyLuong.controller;

import com.example.QuanLyLuong.exception.ResourceNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Controller
public class AppExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, IllegalArgumentException.class, AccessDeniedException.class})
    public String handleKnownException(Exception exception, Model model) {
        model.addAttribute("pageTitle", "Da xay ra loi");
        model.addAttribute("errorTitle", "Khong the hoan thanh yeu cau");
        model.addAttribute("errorMessage", exception.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpectedException(Exception exception, Model model) {
        model.addAttribute("pageTitle", "Da xay ra loi");
        model.addAttribute("errorTitle", "Loi he thong");
        model.addAttribute("errorMessage", exception.getMessage());
        return "error";
    }
}
