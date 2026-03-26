package com.example.QuanLyLuong.service;

import java.nio.charset.StandardCharsets;

import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.User;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountCredentialMailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    public void sendAccountCredentials(Employee employee, User user, String rawPassword) {
        if (employee == null || user == null) {
            throw new IllegalStateException("Không thể gửi email vì dữ liệu tài khoản không hợp lệ.");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình SMTP (spring.mail.host).");
        }
        if (fromEmail == null || fromEmail.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình SMTP username/password.");
        }

        String employeeEmail = employee.getEmail();
        if (employeeEmail == null || employeeEmail.isBlank()) {
            throw new IllegalStateException("Nhân viên chưa có email để nhận tài khoản.");
        }

        String subject = "Thông tin tài khoản đăng nhập hệ thống quản lý lương";
        String content = buildMailContent(employee, user, rawPassword);

        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(employeeEmail.trim());
            helper.setSubject(subject);
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail.trim());
            }
            helper.setText(content, false);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new IllegalStateException(resolveMailError(exception), exception);
        }
    }

    private String buildMailContent(Employee employee, User user, String rawPassword) {
        return "Xin chào " + employee.getFullName() + ",\n\n"
                + "Hệ thống quản lý lương đã cấp tài khoản đăng nhập cho bạn.\n"
                + "Username: " + user.getUsername() + "\n"
                + "Mật khẩu tạm thời: " + rawPassword + "\n"
                + "Vai trò: " + user.getRole().getLabel() + "\n\n"
                + "Bạn vui lòng đăng nhập và đổi mật khẩu sau khi nhận được email này.\n\n"
                + "Quản Lý Lương";
    }

    private String resolveMailError(Exception exception) {
        if (exception instanceof MailAuthenticationException) {
            return "Xác thực SMTP thất bại. Kiểm tra spring.mail.username/password.";
        }
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String lowerMessage = root.getMessage() == null ? "" : root.getMessage().toLowerCase();
        if (lowerMessage.contains("authentication")) {
            return "Xác thực SMTP thất bại. Kiểm tra tài khoản gửi mail và App Password.";
        }
        if (lowerMessage.contains("timed out") || lowerMessage.contains("timeout")) {
            return "Kết nối SMTP bị timeout. Kiểm tra host/port và kết nối mạng.";
        }
        if (lowerMessage.contains("unknownhost") || lowerMessage.contains("could not connect")
                || lowerMessage.contains("connection refused")) {
            return "Không kết nối được SMTP server. Kiểm tra spring.mail.host và spring.mail.port.";
        }
        if (root.getMessage() != null && !root.getMessage().isBlank()) {
            return "Lỗi SMTP: " + root.getMessage();
        }
        return "Gửi email tài khoản thất bại. Vui lòng kiểm tra cấu hình SMTP.";
    }
}
