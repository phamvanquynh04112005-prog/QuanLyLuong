package com.example.QuanLyLuong.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.dto.DepartmentReportItem;
import com.example.QuanLyLuong.dto.MonthlyPayrollTrendItem;
import com.example.QuanLyLuong.dto.PayrollReportDashboard;
import com.example.QuanLyLuong.entity.Department;
import com.example.QuanLyLuong.entity.Payroll;
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

    public PayrollReportDashboard buildDepartmentReport(Integer month, Integer year, Integer rangeMonths) {
        int safeRangeMonths = rangeMonths == null || rangeMonths < 1 ? 6 : Math.min(rangeMonths, 12);
        YearMonth endMonth = YearMonth.of(year, month);

        List<MonthlyPayrollTrendItem> monthlyTrend = buildMonthlyTrend(endMonth, safeRangeMonths);
        List<DepartmentReportItem> departmentItems = buildDepartmentItems(month, year);

        double selectedMonthTotal = departmentItems.stream().mapToDouble(DepartmentReportItem::getTotalSalary).sum();
        double periodTotal = monthlyTrend.stream().mapToDouble(MonthlyPayrollTrendItem::getTotalSalary).sum();
        long periodPayrollCount = monthlyTrend.stream().mapToLong(MonthlyPayrollTrendItem::getPayrollCount).sum();
        long periodPaidCount = monthlyTrend.stream().mapToLong(MonthlyPayrollTrendItem::getPaidCount).sum();
        long periodPendingCount = monthlyTrend.stream().mapToLong(MonthlyPayrollTrendItem::getPendingCount).sum();
        double averageMonthlyTotal = monthlyTrend.isEmpty() ? 0.0 : periodTotal / monthlyTrend.size();
        double maxTrendValue = monthlyTrend.stream().mapToDouble(MonthlyPayrollTrendItem::getTotalSalary).max().orElse(0.0);

        DepartmentReportItem topDepartment = departmentItems.stream()
                .max(Comparator.comparingDouble(DepartmentReportItem::getTotalSalary))
                .orElse(null);

        double paidRatio = periodPayrollCount == 0 ? 0.0 : (periodPaidCount * 100.0) / periodPayrollCount;

        return PayrollReportDashboard.builder()
                .departmentItems(departmentItems)
                .monthlyTrend(monthlyTrend)
                .selectedMonthTotal(selectedMonthTotal)
                .periodTotal(periodTotal)
                .averageMonthlyTotal(averageMonthlyTotal)
                .periodPayrollCount(periodPayrollCount)
                .periodPaidCount(periodPaidCount)
                .periodPendingCount(periodPendingCount)
                .maxTrendValue(maxTrendValue)
                .topDepartmentName(topDepartment == null ? "Chua co du lieu" : topDepartment.getDepartmentName())
                .topDepartmentTotal(topDepartment == null ? 0.0 : topDepartment.getTotalSalary())
                .paidRatio(Math.round(paidRatio * 10.0) / 10.0)
                .build();
    }

    private List<DepartmentReportItem> buildDepartmentItems(Integer month, Integer year) {
        List<DepartmentReportItem> items = new ArrayList<>();
        List<Department> departments = departmentRepository.findAll();
        double overallTotal = departments.stream()
                .mapToDouble(department -> sumPayrolls(payrollRepository.findByDepartmentAndMonthAndYear(department.getId(), month, year)))
                .sum();

        for (Department department : departments) {
            List<Payroll> payrolls = payrollRepository.findByDepartmentAndMonthAndYear(department.getId(), month, year);
            long payrollCount = payrolls.size();
            double total = sumPayrolls(payrolls);
            long paidCount = payrolls.stream().filter(payroll -> payroll.getPaymentStatus() == PaymentStatus.PAID).count();
            long pendingCount = payrolls.stream().filter(payroll -> payroll.getPaymentStatus() == PaymentStatus.PENDING).count();
            double averageSalary = payrollCount == 0 ? 0.0 : total / payrollCount;
            double percentage = overallTotal == 0.0 ? 0.0 : (total / overallTotal) * 100.0;

            items.add(DepartmentReportItem.builder()
                    .departmentName(department.getName())
                    .totalSalary(total)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .payrollCount(payrollCount)
                    .paidCount(paidCount)
                    .pendingCount(pendingCount)
                    .averageSalary(averageSalary)
                    .build());
        }

        return items.stream()
                .sorted(Comparator.comparingDouble(DepartmentReportItem::getTotalSalary).reversed()
                        .thenComparing(DepartmentReportItem::getDepartmentName))
                .toList();
    }

    private List<MonthlyPayrollTrendItem> buildMonthlyTrend(YearMonth endMonth, int rangeMonths) {
        List<MonthlyPayrollTrendItem> items = new ArrayList<>();
        for (int index = rangeMonths - 1; index >= 0; index--) {
            YearMonth current = endMonth.minusMonths(index);
            List<Payroll> payrolls = payrollRepository.findByMonthAndYearOrderByEmployeeFullNameAsc(
                    current.getMonthValue(),
                    current.getYear()
            );
            long payrollCount = payrolls.size();
            long paidCount = payrolls.stream().filter(payroll -> payroll.getPaymentStatus() == PaymentStatus.PAID).count();
            long pendingCount = payrolls.stream().filter(payroll -> payroll.getPaymentStatus() == PaymentStatus.PENDING).count();
            double totalSalary = sumPayrolls(payrolls);
            double paidSalary = sumPayrolls(payrolls.stream().filter(payroll -> payroll.getPaymentStatus() == PaymentStatus.PAID).toList());
            double pendingSalary = sumPayrolls(payrolls.stream().filter(payroll -> payroll.getPaymentStatus() == PaymentStatus.PENDING).toList());

            items.add(MonthlyPayrollTrendItem.builder()
                    .month(current.getMonthValue())
                    .year(current.getYear())
                    .label(String.format("%02d/%d", current.getMonthValue(), current.getYear()))
                    .totalSalary(totalSalary)
                    .payrollCount(payrollCount)
                    .paidCount(paidCount)
                    .pendingCount(pendingCount)
                    .paidSalary(paidSalary)
                    .pendingSalary(pendingSalary)
                    .build());
        }
        return items;
    }

    private double sumPayrolls(List<Payroll> payrolls) {
        return payrolls.stream()
                .mapToDouble(payroll -> payroll.getActualSalary() == null ? 0.0 : payroll.getActualSalary())
                .sum();
    }
}