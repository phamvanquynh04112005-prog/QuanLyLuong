package com.example.QuanLyLuong.service;

import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.dto.EmployeeImportResult;
import com.example.QuanLyLuong.entity.Department;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.DepartmentRepository;
import com.example.QuanLyLuong.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private static final Pattern EMPLOYEE_CODE_PATTERN = Pattern.compile("^EMP(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + id));
    }

    @Transactional(readOnly = true)
    public String generateNextEmployeeCode() {
        List<Integer> usedSequences = employeeRepository.findAll().stream()
                .map(Employee::getEmployeeCode)
                .map(this::extractEmployeeCodeNumber)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        int nextSequence = 1;
        for (Integer usedSequence : usedSequences) {
            if (usedSequence == null || usedSequence < nextSequence) {
                continue;
            }
            if (usedSequence > nextSequence) {
                break;
            }
            nextSequence++;
        }
        return formatEmployeeCode(nextSequence);
    }

    public Employee save(Employee employee) {
        employee.setDepartment(resolveDepartment(employee));
        employee.setEmployeeCode(generateNextEmployeeCode());
        employee.setEmail(normalizeEmail(employee.getEmail()));
        employee.setFullName(normalizeText(employee.getFullName()));
        employee.setPosition(normalizeText(employee.getPosition()));
        employee.setJoinDate(employee.getJoinDate() == null ? LocalDate.now() : employee.getJoinDate());
        employee.setDependentCount(0);
        employee.setStatus(employee.getStatus() == null ? EmployeeStatus.ACTIVE : employee.getStatus());
        return employeeRepository.save(employee);
    }

    public Employee update(Long id, Employee updatedEmployee) {
        Employee existing = findById(id);
        existing.setFullName(normalizeText(updatedEmployee.getFullName()));
        existing.setEmail(normalizeEmail(updatedEmployee.getEmail()));
        existing.setPosition(normalizeText(updatedEmployee.getPosition()));
        existing.setBaseSalary(updatedEmployee.getBaseSalary());
        existing.setJoinDate(updatedEmployee.getJoinDate() == null
                ? (existing.getJoinDate() == null ? LocalDate.now() : existing.getJoinDate())
                : updatedEmployee.getJoinDate());
        if (updatedEmployee.getDependentCount() != null) {
            existing.setDependentCount(Math.max(0, updatedEmployee.getDependentCount()));
        }
        existing.setStatus(updatedEmployee.getStatus());
        existing.setDepartment(resolveDepartment(updatedEmployee));
        existing.setEmployeeCode(normalizeCode(existing.getEmployeeCode()));
        return employeeRepository.save(existing);
    }

    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Employee> searchByName(String keyword) {
        String normalizedKeyword = normalizeText(keyword);
        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            return findAll();
        }
        return employeeRepository.findByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc(
                normalizedKeyword,
                normalizedKeyword
        );
    }

    @Transactional(readOnly = true)
    public List<Employee> findByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentIdOrderByFullNameAsc(departmentId);
    }

    public EmployeeImportResult importFromExcel(MultipartFile file) {
        List<String> messages = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            messages.add("Vui lòng chọn file Excel để import.");
            return EmployeeImportResult.builder()
                    .importedCount(0)
                    .updatedCount(0)
                    .skippedCount(0)
                    .messages(messages)
                    .build();
        }

        int imported = 0;
        int updated = 0;
        int skipped = 0;

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                messages.add("File Excel không có sheet dữ liệu.");
                return EmployeeImportResult.builder()
                        .importedCount(0)
                        .updatedCount(0)
                        .skippedCount(0)
                        .messages(messages)
                        .build();
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            int headerRowIndex = sheet.getFirstRowNum();
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                messages.add("Không tìm thấy dòng tiêu đề trong file Excel.");
                return EmployeeImportResult.builder()
                        .importedCount(0)
                        .updatedCount(0)
                        .skippedCount(0)
                        .messages(messages)
                        .build();
            }

            Map<String, Integer> headers = buildHeaderMap(headerRow, formatter);
            Integer fullNameCol = findColumn(headers, "fullname", "hoten", "tennhanvien", "name");
            Integer emailCol = findColumn(headers, "email");
            if (fullNameCol == null || emailCol == null) {
                messages.add("Thiếu cột bắt buộc: Họ tên hoặc Email.");
                return EmployeeImportResult.builder()
                        .importedCount(0)
                        .updatedCount(0)
                        .skippedCount(0)
                        .messages(messages)
                        .build();
            }

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter)) {
                    continue;
                }

                try {
                    String employeeCode = normalizeCode(readString(row, headers, formatter,
                            "employeecode", "manv", "manhanvien", "ma"));
                    String fullName = normalizeText(readString(row, headers, formatter,
                            "fullname", "hoten", "tennhanvien", "name"));
                    String email = normalizeEmail(readString(row, headers, formatter, "email"));
                    String position = normalizeText(readString(row, headers, formatter, "position", "chucvu"));
                    String departmentName = normalizeText(readString(row, headers, formatter,
                            "department", "departmentname", "phongban", "tenphongban"));
                    Double baseSalary = readDouble(row, headers, formatter, "basesalary", "luongcoban", "salary");
                    LocalDate joinDate = readDate(row, headers, formatter, "joindate", "ngayvaolam", "hiredate");
                    EmployeeStatus status = readStatus(row, headers, formatter, "status", "trangthai");

                    if (fullName == null || fullName.isBlank() || email == null || email.isBlank()) {
                        skipped++;
                        messages.add("Dòng " + (rowIndex + 1) + ": thiếu Họ tên hoặc Email.");
                        continue;
                    }

                    Employee employee = resolveExistingEmployee(employeeCode, email);
                    boolean isNew = employee == null;
                    if (isNew) {
                        employee = new Employee();
                    }

                    if (employeeCode != null) {
                        employee.setEmployeeCode(employeeCode);
                    } else if (employee.getEmployeeCode() == null || employee.getEmployeeCode().isBlank()) {
                        employee.setEmployeeCode(generateNextEmployeeCode());
                    }
                    employee.setFullName(fullName);
                    employee.setEmail(email);
                    employee.setPosition(position);
                    employee.setDepartment(resolveDepartmentByName(departmentName));
                    if (baseSalary != null) {
                        employee.setBaseSalary(baseSalary);
                    } else if (employee.getBaseSalary() == null) {
                        employee.setBaseSalary(0.0);
                    }
                    if (joinDate != null) {
                        employee.setJoinDate(joinDate);
                    } else if (employee.getJoinDate() == null) {
                        employee.setJoinDate(LocalDate.now());
                    }
                    employee.setStatus(status != null ? status : EmployeeStatus.ACTIVE);
                    if (employee.getDependentCount() == null) {
                        employee.setDependentCount(0);
                    }

                    employeeRepository.save(employee);

                    if (isNew) {
                        imported++;
                    } else {
                        updated++;
                    }
                } catch (IllegalArgumentException exception) {
                    skipped++;
                    messages.add("Dòng " + (rowIndex + 1) + ": " + exception.getMessage());
                } catch (Exception exception) {
                    skipped++;
                    messages.add("Dòng " + (rowIndex + 1) + ": dữ liệu không hợp lệ.");
                }
            }
        } catch (Exception exception) {
            messages.add("Không thể đọc file Excel import nhân viên.");
        }

        return EmployeeImportResult.builder()
                .importedCount(imported)
                .updatedCount(updated)
                .skippedCount(skipped)
                .messages(messages)
                .build();
    }

    private Department resolveDepartment(Employee employee) {
        if (employee.getDepartment() == null || employee.getDepartment().getId() == null) {
            return null;
        }
        return departmentRepository.findById(employee.getDepartment().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy phòng ban có ID: " + employee.getDepartment().getId()));
    }

    private Integer extractEmployeeCodeNumber(String employeeCode) {
        if (employeeCode == null || employeeCode.isBlank()) {
            return null;
        }
        Matcher matcher = EMPLOYEE_CODE_PATTERN.matcher(employeeCode.trim());
        if (!matcher.matches()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String formatEmployeeCode(int sequence) {
        return String.format("EMP%04d", Math.max(sequence, 1));
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private Employee resolveExistingEmployee(String employeeCode, String email) {
        Optional<Employee> byEmail = email == null ? Optional.empty() : employeeRepository.findByEmailIgnoreCase(email);
        Optional<Employee> byCode = employeeCode == null
                ? Optional.empty()
                : employeeRepository.findByEmployeeCodeIgnoreCase(employeeCode);

        if (byEmail.isPresent() && byCode.isPresent() && !Objects.equals(byEmail.get().getId(), byCode.get().getId())) {
            throw new IllegalArgumentException("Mã nhân viên và email đang thuộc hai nhân viên khác nhau.");
        }
        return byEmail.orElseGet(() -> byCode.orElse(null));
    }

    private Department resolveDepartmentByName(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            return null;
        }
        return departmentRepository.findByNameIgnoreCase(departmentName)
                .orElseGet(() -> {
                    Department department = new Department();
                    department.setName(departmentName);
                    return departmentRepository.save(department);
                });
    }

    private Map<String, Integer> buildHeaderMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String normalized = normalizeHeader(formatter.formatCellValue(cell));
            if (!normalized.isBlank()) {
                headers.putIfAbsent(normalized, cell.getColumnIndex());
            }
        }
        return headers;
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[\\s_./-]+", "");
    }

    private Integer findColumn(Map<String, Integer> headers, String... aliases) {
        return Stream.of(aliases)
                .map(this::normalizeHeader)
                .map(headers::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
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
        return value == null ? null : value.trim();
    }

    private Double readDouble(Row row, Map<String, Integer> headers, DataFormatter formatter, String... aliases) {
        Integer columnIndex = findColumn(headers, aliases);
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        String raw = formatter.formatCellValue(cell);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replaceAll("[^\\d.-]", "");
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDate readDate(Row row, Map<String, Integer> headers, DataFormatter formatter, String... aliases) {
        Integer columnIndex = findColumn(headers, aliases);
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

        String raw = formatter.formatCellValue(cell);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy")
        );
        for (DateTimeFormatter dateTimeFormatter : formatters) {
            try {
                return LocalDate.parse(raw.trim(), dateTimeFormatter);
            } catch (DateTimeParseException ignored) {
                // thử pattern kế tiếp
            }
        }
        return null;
    }

    private EmployeeStatus readStatus(Row row, Map<String, Integer> headers, DataFormatter formatter, String... aliases) {
        String value = readString(row, headers, formatter, aliases);
        if (value == null || value.isBlank()) {
            return EmployeeStatus.ACTIVE;
        }
        String normalized = normalizeHeader(value);
        if (normalized.contains("inactive")
                || normalized.contains("nghi")
                || normalized.contains("off")
                || normalized.contains("dung")) {
            return EmployeeStatus.INACTIVE;
        }
        return EmployeeStatus.ACTIVE;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        int firstCell = Math.max(row.getFirstCellNum(), 0);
        int lastCell = Math.max(row.getLastCellNum(), 0);
        for (int i = firstCell; i < lastCell; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
