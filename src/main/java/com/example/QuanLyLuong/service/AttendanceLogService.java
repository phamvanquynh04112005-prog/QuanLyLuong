package com.example.QuanLyLuong.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.QuanLyLuong.common.AttendanceSource;
import com.example.QuanLyLuong.dto.AttendanceImportResult;
import com.example.QuanLyLuong.entity.AttendanceLog;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.SalaryConfig;
import com.example.QuanLyLuong.entity.Timesheet;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.AttendanceLogRepository;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.TimesheetRepository;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceLogService {

    private static final DateTimeFormatter[] DATE_PATTERNS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    private static final DateTimeFormatter[] TIME_PATTERNS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm:ss")
    };

    private final AttendanceLogRepository attendanceLogRepository;
    private final EmployeeRepository employeeRepository;
    private final TimesheetRepository timesheetRepository;
    private final SalaryConfigService salaryConfigService;

    @Transactional(readOnly = true)
    public List<AttendanceLog> findByEmployeeAndMonth(Long employeeId, YearMonth yearMonth) {
        return attendanceLogRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                employeeId,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );
    }

    @Transactional(readOnly = true)
    public List<AttendanceLog> findByMonth(YearMonth yearMonth) {
        return attendanceLogRepository.findByAttendanceDateBetweenOrderByAttendanceDateAscEmployeeFullNameAsc(
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );
    }

    public AttendanceLog saveLog(Long employeeId,
                                 LocalDate attendanceDate,
                                 LocalTime checkInTime,
                                 LocalTime checkOutTime,
                                 Double regularHours,
                                 Double overtimeWeekdayHours,
                                 Double overtimeWeekendHours,
                                 Double overtimeHolidayHours,
                                 Integer lateMinutes,
                                 AttendanceSource source,
                                 String machineCode,
                                 String note) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + employeeId));

        SaveOutcome saveOutcome = upsertLog(
                employee,
                attendanceDate,
                checkInTime,
                checkOutTime,
                regularHours,
                overtimeWeekdayHours,
                overtimeWeekendHours,
                overtimeHolidayHours,
                lateMinutes,
                source,
                machineCode,
                note
        );
        syncTimesheetFromLogs(employeeId, YearMonth.from(attendanceDate));
        return saveOutcome.log();
    }

    public AttendanceImportResult importFromExcel(MultipartFile file, YearMonth targetMonth, AttendanceSource defaultSource) {
        if (file == null || file.isEmpty()) {
            return AttendanceImportResult.builder()
                    .importedCount(0)
                    .updatedCount(0)
                    .skippedCount(1)
                    .messages(List.of("File import đang rỗng."))
                    .build();
        }

        DataFormatter formatter = new DataFormatter();
        List<String> messages = new ArrayList<>();
        int importedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        Map<String, Integer> headers = new HashMap<>();
        Map<String, Employee> codeMap = new HashMap<>();
        Map<String, Employee> emailMap = new HashMap<>();
        Map<Long, Employee> idMap = new HashMap<>();
        Map<String, Employee> nameMap = new HashMap<>();

        for (Employee employee : employeeRepository.findAllByOrderByFullNameAsc()) {
            if (employee.getEmployeeCode() != null && !employee.getEmployeeCode().isBlank()) {
                codeMap.put(normalize(employee.getEmployeeCode()), employee);
            }
            if (employee.getEmail() != null && !employee.getEmail().isBlank()) {
                emailMap.put(normalize(employee.getEmail()), employee);
            }
            if (employee.getId() != null) {
                idMap.put(employee.getId(), employee);
            }
            if (employee.getFullName() != null && !employee.getFullName().isBlank()) {
                nameMap.put(normalize(employee.getFullName()), employee);
            }
        }

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                messages.add("File Excel không có dữ liệu.");
                return AttendanceImportResult.builder()
                        .importedCount(0)
                        .updatedCount(0)
                        .skippedCount(1)
                        .messages(messages)
                        .build();
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            for (Cell cell : headerRow) {
                headers.put(normalize(formatter.formatCellValue(cell)), cell.getColumnIndex());
            }

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                Employee employee = resolveEmployee(row, headers, formatter, codeMap, emailMap, idMap, nameMap);
                LocalDate attendanceDate = readDate(row, headers, formatter);
                if (employee == null) {
                    skippedCount++;
                    messages.add("Dòng " + (rowIndex + 1) + ": không tìm thấy nhân viên theo mã/email/tên.");
                    continue;
                }
                if (attendanceDate == null) {
                    skippedCount++;
                    messages.add("Dòng " + (rowIndex + 1) + ": không đọc được ngày chấm công.");
                    continue;
                }
                if (!YearMonth.from(attendanceDate).equals(targetMonth)) {
                    skippedCount++;
                    messages.add("Dòng " + (rowIndex + 1) + ": ngày " + attendanceDate + " nằm ngoài tháng được chọn.");
                    continue;
                }

                LocalTime checkIn = readTime(row, headers, formatter, "checkin", "checkintime", "intime", "giovao");
                LocalTime checkOut = readTime(row, headers, formatter, "checkout", "checkouttime", "outtime", "giora");
                Double regularHours = readDouble(row, headers, formatter, "regularhours", "hours", "workhours", "giocong");
                Double overtimeWeekdayHours = readDouble(row, headers, formatter, "overtimeweekdayhours", "otweekday", "tangcangaythuong");
                Double overtimeWeekendHours = readDouble(row, headers, formatter, "overtimeweekendhours", "otweekend", "tangcacuoituan");
                Double overtimeHolidayHours = readDouble(row, headers, formatter, "overtimeholidayhours", "otholiday", "tangcale");
                Integer lateMinutes = readInteger(row, headers, formatter, "lateminutes", "late", "ditrem");
                String machineCode = readString(row, headers, formatter, "machinecode", "deviceid", "maychamcong");
                String note = readString(row, headers, formatter, "note", "ghichu");
                AttendanceSource source = resolveSource(readString(row, headers, formatter, "source", "nguon"), defaultSource);

                SaveOutcome saveOutcome = upsertLog(
                        employee,
                        attendanceDate,
                        checkIn,
                        checkOut,
                        regularHours,
                        overtimeWeekdayHours,
                        overtimeWeekendHours,
                        overtimeHolidayHours,
                        lateMinutes,
                        source,
                        machineCode,
                        note
                );
                syncTimesheetFromLogs(employee.getId(), targetMonth);
                if (saveOutcome.created()) {
                    importedCount++;
                } else {
                    updatedCount++;
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc file Excel import chấm công.", exception);
        }

        if (messages.isEmpty()) {
            messages.add("Import thanh cong file " + file.getOriginalFilename() + ".");
        }

        return AttendanceImportResult.builder()
                .importedCount(importedCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .messages(messages)
                .build();
    }

    public Timesheet syncTimesheetFromLogs(Long employeeId, YearMonth yearMonth) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + employeeId));
        Timesheet timesheet = timesheetRepository.findByEmployeeIdAndMonthAndYear(employeeId, yearMonth.getMonthValue(), yearMonth.getYear())
                .orElseGet(Timesheet::new);
        List<AttendanceLog> attendanceLogs = findByEmployeeAndMonth(employeeId, yearMonth);
        SalaryConfig salaryConfig = salaryConfigService.getEffectiveConfig(employeeId, yearMonth);

        int standardWorkDays = positiveInteger(salaryConfig.getStandardWorkDays(), 26);
        double totalRegularHours = attendanceLogs.stream().mapToDouble(log -> safeDouble(log.getRegularHours())).sum();
        double totalWeekdayOt = attendanceLogs.stream().mapToDouble(log -> safeDouble(log.getOvertimeWeekdayHours())).sum();
        double totalWeekendOt = attendanceLogs.stream().mapToDouble(log -> safeDouble(log.getOvertimeWeekendHours())).sum();
        double totalHolidayOt = attendanceLogs.stream().mapToDouble(log -> safeDouble(log.getOvertimeHolidayHours())).sum();
        long workDayCount = attendanceLogs.stream()
                .filter(this::hasWorkSignal)
                .count();
        int leaveDays = timesheet.getLeaveDays() == null ? 0 : timesheet.getLeaveDays();

        timesheet.setEmployee(employee);
        timesheet.setMonth(yearMonth.getMonthValue());
        timesheet.setYear(yearMonth.getYear());
        timesheet.setWorkDays((int) workDayCount);
        timesheet.setRegularHours(round2(totalRegularHours));
        timesheet.setOvertimeWeekdayHours(round2(totalWeekdayOt));
        timesheet.setOvertimeWeekendHours(round2(totalWeekendOt));
        timesheet.setOvertimeHolidayHours(round2(totalHolidayOt));
        timesheet.setImportedLogCount(attendanceLogs.size());
        timesheet.setAbsentDays(Math.max(0, standardWorkDays - timesheet.getWorkDays() - leaveDays));
        if (timesheet.getLeaveDays() == null) {
            timesheet.setLeaveDays(0);
        }
        return timesheetRepository.save(timesheet);
    }

    private SaveOutcome upsertLog(Employee employee,
                                  LocalDate attendanceDate,
                                  LocalTime checkInTime,
                                  LocalTime checkOutTime,
                                  Double regularHours,
                                  Double overtimeWeekdayHours,
                                  Double overtimeWeekendHours,
                                  Double overtimeHolidayHours,
                                  Integer lateMinutes,
                                  AttendanceSource source,
                                  String machineCode,
                                  String note) {
        if (attendanceDate == null) {
            throw new IllegalArgumentException("Ngày chấm công không được để trống.");
        }

        YearMonth yearMonth = YearMonth.from(attendanceDate);
        double standardWorkHoursPerDay = positiveDouble(
                salaryConfigService.getEffectiveConfig(employee.getId(), yearMonth).getStandardWorkHoursPerDay(),
                8.0
        );
        AttendanceLog attendanceLog = attendanceLogRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), attendanceDate)
                .orElseGet(AttendanceLog::new);
        boolean created = attendanceLog.getId() == null;

        double[] derivedHours = deriveHours(checkInTime, checkOutTime, regularHours, overtimeWeekdayHours, standardWorkHoursPerDay);
        attendanceLog.setEmployee(employee);
        attendanceLog.setAttendanceDate(attendanceDate);
        attendanceLog.setCheckInTime(checkInTime);
        attendanceLog.setCheckOutTime(checkOutTime);
        attendanceLog.setRegularHours(round2(derivedHours[0]));
        attendanceLog.setOvertimeWeekdayHours(round2(derivedHours[1]));
        attendanceLog.setOvertimeWeekendHours(round2(safeDouble(overtimeWeekendHours)));
        attendanceLog.setOvertimeHolidayHours(round2(safeDouble(overtimeHolidayHours)));
        attendanceLog.setLateMinutes(lateMinutes == null ? 0 : Math.max(0, lateMinutes));
        attendanceLog.setSource(source == null ? AttendanceSource.MANUAL : source);
        attendanceLog.setMachineCode(machineCode == null || machineCode.isBlank() ? null : machineCode.trim());
        attendanceLog.setNote(note == null || note.isBlank() ? null : note.trim());
        return new SaveOutcome(attendanceLogRepository.save(attendanceLog), created);
    }

    private Employee resolveEmployee(Row row,
                                     Map<String, Integer> headers,
                                     DataFormatter formatter,
                                     Map<String, Employee> codeMap,
                                     Map<String, Employee> emailMap,
                                     Map<Long, Employee> idMap,
                                     Map<String, Employee> nameMap) {
        String employeeCode = readString(row, headers, formatter, "employeecode", "code", "manv");
        if (employeeCode != null) {
            Employee employee = codeMap.get(normalize(employeeCode));
            if (employee != null) {
                return employee;
            }
        }

        String email = readString(row, headers, formatter, "email");
        if (email != null) {
            Employee employee = emailMap.get(normalize(email));
            if (employee != null) {
                return employee;
            }
        }

        Integer employeeId = readInteger(row, headers, formatter, "employeeid", "nhanvienid", "id");
        if (employeeId != null) {
            Employee employee = idMap.get(employeeId.longValue());
            if (employee != null) {
                return employee;
            }
        }

        String fullName = readString(row, headers, formatter, "fullname", "hoten", "employee");
        if (fullName != null) {
            return nameMap.get(normalize(fullName));
        }
        return null;
    }

    private AttendanceSource resolveSource(String rawSource, AttendanceSource defaultSource) {
        if (rawSource == null || rawSource.isBlank()) {
            return defaultSource == null ? AttendanceSource.EXCEL_IMPORT : defaultSource;
        }
        String normalized = normalize(rawSource);
        if (normalized.contains("machine") || normalized.contains("may")) {
            return AttendanceSource.MACHINE_IMPORT;
        }
        if (normalized.contains("manual") || normalized.contains("tay")) {
            return AttendanceSource.MANUAL;
        }
        return AttendanceSource.EXCEL_IMPORT;
    }

    private LocalDate readDate(Row row, Map<String, Integer> headers, DataFormatter formatter) {
        Integer columnIndex = findColumn(headers, "attendancedate", "date", "ngay");
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = formatter.formatCellValue(cell);
        if (text == null || text.isBlank()) {
            return null;
        }
        for (DateTimeFormatter datePattern : DATE_PATTERNS) {
            try {
                return LocalDate.parse(text.trim(), datePattern);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalTime readTime(Row row, Map<String, Integer> headers, DataFormatter formatter, String... aliases) {
        Integer columnIndex = findColumn(headers, aliases);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalTime();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double numericValue = cell.getNumericCellValue();
            if (numericValue >= 0 && numericValue < 1) {
                long seconds = Math.round(numericValue * 24 * 60 * 60);
                return LocalTime.ofSecondOfDay(seconds % (24 * 60 * 60));
            }
        }
        String text = formatter.formatCellValue(cell);
        if (text == null || text.isBlank()) {
            return null;
        }
        for (DateTimeFormatter timePattern : TIME_PATTERNS) {
            try {
                return LocalTime.parse(text.trim(), timePattern);
            } catch (DateTimeParseException ignored) {
            }
        }
        try {
            return LocalDateTime.parse(text.trim()).toLocalTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private Double readDouble(Row row, Map<String, Integer> headers, DataFormatter formatter, String... aliases) {
        String value = readString(row, headers, formatter, aliases);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer readInteger(Row row, Map<String, Integer> headers, DataFormatter formatter, String... aliases) {
        Double numericValue = readDouble(row, headers, formatter, aliases);
        return numericValue == null ? null : numericValue.intValue();
    }

    private String readString(Row row, Map<String, Integer> headers, DataFormatter formatter, String... aliases) {
        Integer columnIndex = findColumn(headers, aliases);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer findColumn(Map<String, Integer> headers, String... aliases) {
        for (String alias : aliases) {
            Integer columnIndex = headers.get(normalize(alias));
            if (columnIndex != null) {
                return columnIndex;
            }
        }
        return null;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int index = row.getFirstCellNum(); index < row.getLastCellNum(); index++) {
            if (index < 0) {
                continue;
            }
            Cell cell = row.getCell(index);
            if (cell != null && !formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasWorkSignal(AttendanceLog attendanceLog) {
        return safeDouble(attendanceLog.getRegularHours()) > 0
                || safeDouble(attendanceLog.getOvertimeWeekdayHours()) > 0
                || safeDouble(attendanceLog.getOvertimeWeekendHours()) > 0
                || safeDouble(attendanceLog.getOvertimeHolidayHours()) > 0
                || attendanceLog.getCheckInTime() != null
                || attendanceLog.getCheckOutTime() != null;
    }

    private double[] deriveHours(LocalTime checkInTime,
                                 LocalTime checkOutTime,
                                 Double regularHours,
                                 Double overtimeWeekdayHours,
                                 double standardWorkHoursPerDay) {
        double resolvedRegularHours = safeDouble(regularHours);
        double resolvedWeekdayOt = safeDouble(overtimeWeekdayHours);
        if (resolvedRegularHours <= 0 && checkInTime != null && checkOutTime != null && checkOutTime.isAfter(checkInTime)) {
            long workedMinutes = Duration.between(checkInTime, checkOutTime).toMinutes();
            double workedHours = Math.max(0.0, workedMinutes / 60.0);
            resolvedRegularHours = Math.min(workedHours, standardWorkHoursPerDay);
            if (resolvedWeekdayOt <= 0 && workedHours > standardWorkHoursPerDay) {
                resolvedWeekdayOt = workedHours - standardWorkHoursPerDay;
            }
        }
        return new double[] { resolvedRegularHours, resolvedWeekdayOt };
    }

    private String normalize(String input) {
        return input == null
                ? ""
                : input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private int positiveInteger(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private double positiveDouble(Double value, double fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record SaveOutcome(AttendanceLog log, boolean created) {
    }
}
