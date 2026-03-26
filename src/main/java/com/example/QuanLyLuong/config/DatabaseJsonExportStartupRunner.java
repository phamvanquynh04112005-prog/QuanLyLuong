package com.example.QuanLyLuong.config;

import com.example.QuanLyLuong.service.DatabaseJsonExportService;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseJsonExportStartupRunner implements ApplicationRunner {

    private final DatabaseJsonExportService databaseJsonExportService;

    @Override
    public void run(ApplicationArguments args) {
        databaseJsonExportService.exportNow();
    }
}
