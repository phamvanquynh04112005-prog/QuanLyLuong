package com.example.QuanLyLuong.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.QuanLyLuong.common.CompensationItemType;
import com.example.QuanLyLuong.entity.CompensationItem;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.CompensationItemRepository;
import com.example.QuanLyLuong.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompensationItemService {

    private final CompensationItemRepository compensationItemRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<CompensationItem> findAll(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return compensationItemRepository.findAll().stream()
                .filter(item -> normalizedKeyword.isBlank() || matchesKeyword(item, normalizedKeyword))
                .sorted(Comparator
                        .comparing((CompensationItem item) -> safeText(item.getEmployee().getFullName()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CompensationItem::getEffectiveDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(item -> safeText(item.getName()), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean matchesKeyword(CompensationItem item, String keyword) {
        if (item == null || item.getEmployee() == null) {
            return false;
        }
        String fullName = safeText(item.getEmployee().getFullName()).toLowerCase();
        String employeeCode = safeText(item.getEmployee().getEmployeeCode()).toLowerCase();
        return fullName.contains(keyword) || employeeCode.contains(keyword);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    public CompensationItem save(Long employeeId,
                                 CompensationItemType componentType,
                                 String name,
                                 Double amount,
                                 Boolean taxable,
                                 Boolean recurring,
                                 Boolean active,
                                 LocalDate effectiveDate,
                                 LocalDate endDate,
                                 String note) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + employeeId));

        CompensationItem item = new CompensationItem();
        item.setEmployee(employee);
        item.setComponentType(componentType);
        item.setName(name == null ? "" : name.trim());
        item.setAmount(amount == null ? 0.0 : Math.max(0.0, amount));
        item.setTaxable(Boolean.TRUE.equals(taxable));
        item.setRecurring(recurring == null || recurring);
        item.setActive(active == null || active);
        item.setEffectiveDate(effectiveDate == null ? LocalDate.now() : effectiveDate);
        item.setEndDate(endDate);
        item.setNote(note == null ? null : note.trim());
        return compensationItemRepository.save(item);
    }

    public void delete(Long id) {
        compensationItemRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<CompensationItem> findApplicableItems(Long employeeId, YearMonth yearMonth) {
        return compensationItemRepository.findApplicableItems(
                employeeId,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        );
    }

    @Transactional(readOnly = true)
    public Map<CompensationItemType, Double> summarizeApplicableItems(Long employeeId, YearMonth yearMonth) {
        return findApplicableItems(employeeId, yearMonth).stream()
                .collect(Collectors.groupingBy(
                        CompensationItem::getComponentType,
                        Collectors.summingDouble(item -> item.getAmount() == null ? 0.0 : item.getAmount())
                ));
    }
}
