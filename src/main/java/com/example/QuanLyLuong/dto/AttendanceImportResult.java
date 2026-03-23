package com.example.QuanLyLuong.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttendanceImportResult {
    private final int importedCount;
    private final int updatedCount;
    private final int skippedCount;
    private final List<String> messages;

    public static AttendanceImportResult empty() {
        return AttendanceImportResult.builder()
                .importedCount(0)
                .updatedCount(0)
                .skippedCount(0)
                .messages(new ArrayList<>())
                .build();
    }
}