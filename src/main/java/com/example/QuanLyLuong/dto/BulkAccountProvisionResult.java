package com.example.QuanLyLuong.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkAccountProvisionResult {

    private final int createdCount;
    private final int emailedCount;
    private final int skippedCount;
    private final List<String> warnings;
}
