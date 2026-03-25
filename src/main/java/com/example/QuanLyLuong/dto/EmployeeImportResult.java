package com.example.QuanLyLuong.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployeeImportResult {
    private int importedCount;
    private int updatedCount;
    private int skippedCount;
    private List<String> messages;
}

