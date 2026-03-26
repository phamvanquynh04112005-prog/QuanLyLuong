package com.example.QuanLyLuong.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseJsonExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.db-json.auto-export.enabled:true}")
    private boolean autoExportEnabled;

    @Value("${app.db-json.export.path:data/quanlyluong-export.json}")
    private String exportPath;

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    public synchronized void exportNow() {
        if (!autoExportEnabled) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("generatedAt", TIMESTAMP_FORMATTER.format(Instant.now()));
        payload.put("jdbcUrl", jdbcUrl);

        List<String> tableNames = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = 'PUBLIC'
                  and table_type = 'BASE TABLE'
                order by table_name
                """, String.class);

        payload.put("tableCount", tableNames.size());

        Map<String, Object> tables = new LinkedHashMap<>();
        for (String tableName : tableNames) {
            List<Map<String, Object>> rows = jdbcTemplate.query("select * from " + tableName, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                int columnCount = rs.getMetaData().getColumnCount();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    String columnName = rs.getMetaData().getColumnLabel(columnIndex).toLowerCase(Locale.ROOT);
                    row.put(columnName, normalizeValue(rs.getObject(columnIndex)));
                }
                return row;
            });
            tables.put(tableName.toLowerCase(Locale.ROOT), rows);
        }
        payload.put("tables", tables);

        Path output = Paths.get(exportPath);
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, toJson(payload, 0), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể ghi file JSON export database.", exception);
        }
    }

    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof Time time) {
            return time.toLocalTime().toString();
        }
        return value;
    }

    private String toJson(Object value, int indent) throws IOException {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + escape(text) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            return mapToJson(map, indent);
        }
        if (value instanceof List<?> list) {
            return listToJson(list, indent);
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private String mapToJson(Map<?, ?> map, int indent) throws IOException {
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        int index = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            builder.append(" ".repeat(indent + 2))
                    .append("\"")
                    .append(escape(String.valueOf(entry.getKey())))
                    .append("\": ")
                    .append(toJson(entry.getValue(), indent + 2));
            if (++index < map.size()) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append(" ".repeat(indent)).append("}");
        return builder.toString();
    }

    private String listToJson(List<?> list, int indent) throws IOException {
        if (list.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[\n");
        for (int index = 0; index < list.size(); index++) {
            builder.append(" ".repeat(indent + 2))
                    .append(toJson(list.get(index), indent + 2));
            if (index < list.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append(" ".repeat(indent)).append("]");
        return builder.toString();
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
