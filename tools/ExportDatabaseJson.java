import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportDatabaseJson {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    public static void main(String[] args) throws Exception {
        String jdbcUrl = args.length > 0
                ? args[0]
                : "jdbc:h2:file:./data/quanlyluong;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        Path outputPath = Paths.get(args.length > 1 ? args[1] : "data/quanlyluong-export.json");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("generatedAt", TIMESTAMP_FORMATTER.format(Instant.now()));
        payload.put("jdbcUrl", jdbcUrl);

        Map<String, Object> tables = new LinkedHashMap<>();
        payload.put("tables", tables);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            List<String> tableNames = loadTableNames(connection);
            payload.put("tableCount", tableNames.size());

            for (String tableName : tableNames) {
                tables.put(tableName.toLowerCase(Locale.ROOT), loadRows(connection, tableName));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Không thể kết nối file H2 để export JSON. Nếu ứng dụng đang chạy, hãy tắt app rồi chạy lại script.",
                    exception
            );
        }

        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, toJson(payload, 0), StandardCharsets.UTF_8);
        System.out.println("Đã export dữ liệu ra: " + outputPath.toAbsolutePath());
    }

    private static List<String> loadTableNames(Connection connection) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        String sql = """
                select table_name
                from information_schema.tables
                where table_schema = 'PUBLIC'
                  and table_type = 'BASE TABLE'
                order by table_name
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tableNames.add(resultSet.getString("table_name"));
            }
        }
        return tableNames;
    }

    private static List<Map<String, Object>> loadRows(Connection connection, String tableName) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "select * from " + tableName;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    String columnName = metaData.getColumnLabel(columnIndex).toLowerCase(Locale.ROOT);
                    row.put(columnName, normalizeValue(resultSet.getObject(columnIndex)));
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof java.sql.Time time) {
            return time.toLocalTime().toString();
        }
        return value;
    }

    private static String toJson(Object value, int indent) throws IOException {
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

    private static String mapToJson(Map<?, ?> map, int indent) throws IOException {
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

    private static String listToJson(List<?> list, int indent) throws IOException {
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

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
