package com.example.QuanLyLuong.config;

import com.example.QuanLyLuong.service.DatabaseJsonExportService;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
public class DatabaseJsonAutoExportAspect {

    private static final String EXPORT_SYNC_KEY = DatabaseJsonAutoExportAspect.class.getName() + ".EXPORT_SYNC";

    private final DatabaseJsonExportService databaseJsonExportService;

    @Pointcut("""
            (
                execution(public * com.example.QuanLyLuong.service..*.save*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.update*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.delete*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.create*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.import*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.provision*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.toggle*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.change*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.calculate*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.mark*(..)) ||
                execution(public * com.example.QuanLyLuong.service..*.purge*(..))
            )
            && !within(com.example.QuanLyLuong.service.VietnamPersonalIncomeTaxService)
            """)
    public void mutatingServiceMethod() {
    }

    @AfterReturning("mutatingServiceMethod()")
    public void scheduleExportAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            databaseJsonExportService.exportNow();
            return;
        }
        if (TransactionSynchronizationManager.hasResource(EXPORT_SYNC_KEY)) {
            return;
        }

        TransactionSynchronizationManager.bindResource(EXPORT_SYNC_KEY, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                databaseJsonExportService.exportNow();
            }

            @Override
            public void afterCompletion(int status) {
                if (TransactionSynchronizationManager.hasResource(EXPORT_SYNC_KEY)) {
                    TransactionSynchronizationManager.unbindResource(EXPORT_SYNC_KEY);
                }
            }
        });
    }
}
