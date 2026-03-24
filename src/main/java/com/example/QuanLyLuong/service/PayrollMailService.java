package com.example.QuanLyLuong.service;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;

import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.service.export.PdfExportService;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayrollMailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PdfExportService pdfExportService;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    public void sendPayslipEmail(Payroll payroll) {
        if (payroll == null || payroll.getEmployee() == null) {
            throw new IllegalStateException("Khong the gui mail vi du lieu payroll khong hop le.");
        }
        if (payroll.getPaymentStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Chi gui mail sau khi bang luong da o trang thai Da chi.");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || mailHost == null || mailHost.isBlank()) {
            throw new IllegalStateException("Chua cau hinh SMTP (spring.mail.host).");
        }
        if (fromEmail == null || fromEmail.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            throw new IllegalStateException("Chua cau hinh SMTP username/password.");
        }

        String employeeEmail = payroll.getEmployee().getEmail();
        if (employeeEmail == null || employeeEmail.isBlank()) {
            throw new IllegalStateException("Nhan vien chua co email de nhan phieu luong.");
        }

        byte[] pdfData;
        try {
            pdfData = pdfExportService.exportPayslipToPdf(payroll);
        } catch (Exception exception) {
            throw new IllegalStateException("Khong tao duoc file PDF phieu luong.", exception);
        }

        String subject = "Phieu luong thang " + payroll.getMonth() + "/" + payroll.getYear();
        String filename = "phieu-luong-" + payroll.getId() + ".pdf";
        String content = buildMailContent(payroll);

        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(employeeEmail.trim());
            helper.setSubject(subject);
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail.trim());
            }
            helper.setText(content, false);
            helper.addAttachment(filename, new ByteArrayResource(pdfData), "application/pdf");
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new IllegalStateException(resolveMailError(exception), exception);
        }
    }

    private String resolveMailError(Exception exception) {
        if (exception instanceof MailAuthenticationException) {
            return "Xac thuc SMTP that bai. Kiem tra spring.mail.username/password (neu dung Gmail thi dung App Password 16 ky tu).";
        }
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String lowerMessage = root.getMessage() == null ? "" : root.getMessage().toLowerCase();
        if (lowerMessage.contains("authentication")) {
            return "Xac thuc SMTP that bai. Kiem tra tai khoan gui mail va App Password.";
        }
        if (lowerMessage.contains("timed out") || lowerMessage.contains("timeout")) {
            return "Ket noi SMTP bi timeout. Kiem tra host/port va ket noi mang.";
        }
        if (lowerMessage.contains("unknownhost") || lowerMessage.contains("could not connect")
                || lowerMessage.contains("connection refused")) {
            return "Khong ket noi duoc SMTP server. Kiem tra spring.mail.host va spring.mail.port.";
        }
        if (root.getMessage() != null && !root.getMessage().isBlank()) {
            return "Loi SMTP: " + root.getMessage();
        }
        return "Gui mail that bai. Vui long kiem tra cau hinh SMTP.";
    }

    private String buildMailContent(Payroll payroll) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        return "Xin chao " + payroll.getEmployee().getFullName() + ",\n\n"
                + "He thong gui den ban phieu luong thang " + payroll.getMonth() + "/" + payroll.getYear() + ".\n"
                + "Luong thuc nhan: " + numberFormat.format(payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary()) + " VND.\n\n"
                + "File PDF phieu luong duoc dinh kem trong email nay.\n\n"
                + "Quan Ly Luong";
    }
}
