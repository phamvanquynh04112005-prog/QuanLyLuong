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
        model.addAttribute("errorTitle", "Không thể hoàn thành yêu cầu");
        model.addAttribute("errorMessage", safeMessage(exception));
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpectedException(Exception exception, Model model) {
        model.addAttribute("pageTitle", "Da xay ra loi");
        model.addAttribute("errorTitle", "Loi he thong");
        model.addAttribute("errorMessage", safeMessage(exception));
        return "error";
    }

    private String safeMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Hệ thống gặp lỗi không xác định. Vui lòng thử lại hoặc liên hệ quản trị viên.";
        }
        return exception.getMessage();
    }
}

