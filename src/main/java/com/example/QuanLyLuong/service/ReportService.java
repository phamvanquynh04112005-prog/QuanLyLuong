package com.example.QuanLyLuong.service;

import java.util.ArrayList;
import java.util.List;

import com.example.QuanLyLuong.dto.DepartmentReportItem;
import com.example.QuanLyLuong.entity.Department;
import com.example.QuanLyLuong.repository.DepartmentRepository;
import com.example.QuanLyLuong.repository.PayrollRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final DepartmentRepository departmentRepository;
    private final PayrollRepository payrollRepository;

    public List<DepartmentReportItem> reportByDepartment(Integer month, Integer year) {
        List<Department> departments = departmentRepository.findAll();
        List<DepartmentReportItem> items = new ArrayList<>();

        double overallTotal = departments.stream()
                .mapToDouble(department -> payrollRepository
                        .findByDepartmentAndMonthAndYear(department.getId(), month, year)
                        .stream()
                        .mapToDouble(payroll -> payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary())
                        .sum())
                .sum();

        for (Department department : departments) {
            double total = payrollRepository.findByDepartmentAndMonthAndYear(department.getId(), month, year)
                    .stream()
                    .mapToDouble(payroll -> payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary())
                    .sum();

            double percentage = overallTotal == 0.0 ? 0.0 : (total / overallTotal) * 100;
            items.add(DepartmentReportItem.builder()
                    .departmentName(department.getName())
                    .totalSalary(total)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .build());
        }

        return items;
    }
}
