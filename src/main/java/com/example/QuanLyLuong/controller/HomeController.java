package com.example.QuanLyLuong.controller;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.dto.DashboardStats;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.Payroll;
import com.example.QuanLyLuong.entity.User;
import com.example.QuanLyLuong.service.DashboardService;
import com.example.QuanLyLuong.service.PayrollService;
import com.example.QuanLyLuong.service.UserService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final double STRONG_INCREASE_THRESHOLD = 20.0;
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> CHART_COLORS = List.of(
            "#1f6f43", "#2a7a54", "#4c9f70", "#7fb685", "#8b9d5e", "#cf8a3d", "#7d8fa6", "#6a6f8d"
    );

    private final DashboardService dashboardService;
    private final PayrollService payrollService;
    private final UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/post-login";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/post-login";
        }
        return "login";
    }

    @GetMapping("/post-login")
    public String postLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        User currentUser = userService.getAuthenticatedUser(authentication);
        if (currentUser.getRole() == Role.ROLE_EMPLOYEE) {
            return "redirect:/payrolls/my";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            User currentUser = userService.getAuthenticatedUser(authentication);
            if (currentUser.getRole() == Role.ROLE_EMPLOYEE) {
                return "redirect:/payrolls/my";
            }
        }

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        DashboardStats stats = dashboardService.buildStats(month, year);
        List<Payroll> recentPayrolls = payrollService.findByMonth(month, year).stream().limit(5).toList();

        model.addAttribute("stats", stats);
        model.addAttribute("recentPayrolls", recentPayrolls);
        model.addAttribute("currentMonth", month);
        model.addAttribute("currentYear", year);
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("contentTemplate", "dashboard");
        return "layout/base";
    }

    @GetMapping("/employee-cost-dashboard")
    public String employeeCostDashboard(@RequestParam(required = false) Integer month,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false, defaultValue = "desc") String sort,
                                        @RequestParam(required = false, defaultValue = "month") String periodType,
                                        @RequestParam(required = false) String date,
                                        @RequestParam(required = false) Integer quarter,
                                        Model model) {
        LocalDate now = LocalDate.now();
        boolean sortDesc = !"asc".equalsIgnoreCase(sort);
        String normalizedSort = sortDesc ? "desc" : "asc";
        String normalizedPeriodType = normalizePeriodType(periodType);

        int currentMonth = month == null ? now.getMonthValue() : Math.max(1, Math.min(month, 12));
        int currentYear = year == null ? now.getYear() : year;
        int selectedQuarter = quarter == null ? monthToQuarter(currentMonth) : Math.max(1, Math.min(quarter, 4));
        LocalDate selectedDate = parseDateOrDefault(date, now);

        List<Payroll> currentPayrolls;
        List<Payroll> previousPayrolls;
        String periodSummary;
        String comparisonSummary;
        int previousMonth;
        int previousYear;

        switch (normalizedPeriodType) {
            case "day" -> {
                currentMonth = selectedDate.getMonthValue();
                currentYear = selectedDate.getYear();
                selectedQuarter = monthToQuarter(currentMonth);
                YearMonth currentPeriod = YearMonth.of(currentYear, currentMonth);
                YearMonth previousPeriod = currentPeriod.minusMonths(1);
                previousMonth = previousPeriod.getMonthValue();
                previousYear = previousPeriod.getYear();
                currentPayrolls = payrollService.findByMonthWithEmployeeAndDepartment(currentMonth, currentYear);
                previousPayrolls = payrollService.findByMonthWithEmployeeAndDepartment(previousMonth, previousYear);
                periodSummary = "Ngày " + selectedDate.format(DAY_FORMATTER) + " (dữ liệu payroll tháng " + currentMonth + "/" + currentYear + ")";
                comparisonSummary = "So sánh với tháng " + previousMonth + "/" + previousYear;
            }
            case "quarter" -> {
                List<YearMonth> quarterMonths = getQuarterMonths(currentYear, selectedQuarter);
                List<YearMonth> previousQuarterMonths = getQuarterMonths(quarterMonths.get(0).minusMonths(3).getYear(), monthToQuarter(quarterMonths.get(0).minusMonths(3).getMonthValue()));
                currentPayrolls = loadPayrollsByMonths(quarterMonths);
                previousPayrolls = loadPayrollsByMonths(previousQuarterMonths);
                previousMonth = previousQuarterMonths.get(previousQuarterMonths.size() - 1).getMonthValue();
                previousYear = previousQuarterMonths.get(previousQuarterMonths.size() - 1).getYear();
                currentMonth = quarterMonths.get(quarterMonths.size() - 1).getMonthValue();
                periodSummary = "Quý " + selectedQuarter + "/" + currentYear;
                comparisonSummary = "So sánh với quý " + monthToQuarter(previousMonth) + "/" + previousYear;
            }
            case "year" -> {
                List<YearMonth> yearMonths = getYearMonths(currentYear);
                List<YearMonth> previousYearMonths = getYearMonths(currentYear - 1);
                currentPayrolls = loadPayrollsByMonths(yearMonths);
                previousPayrolls = loadPayrollsByMonths(previousYearMonths);
                previousMonth = 12;
                previousYear = currentYear - 1;
                currentMonth = 12;
                selectedQuarter = 4;
                selectedDate = LocalDate.of(currentYear, 1, 1);
                periodSummary = "Năm " + currentYear;
                comparisonSummary = "So sánh với năm " + previousYear;
            }
            default -> {
                YearMonth currentPeriod = YearMonth.of(currentYear, currentMonth);
                YearMonth previousPeriod = currentPeriod.minusMonths(1);
                previousMonth = previousPeriod.getMonthValue();
                previousYear = previousPeriod.getYear();
                selectedDate = LocalDate.of(currentYear, currentMonth, 1);
                selectedQuarter = monthToQuarter(currentMonth);
                currentPayrolls = payrollService.findByMonthWithEmployeeAndDepartment(currentMonth, currentYear);
                previousPayrolls = payrollService.findByMonthWithEmployeeAndDepartment(previousMonth, previousYear);
                periodSummary = "Tháng " + currentMonth + "/" + currentYear;
                comparisonSummary = "So sánh với tháng " + previousMonth + "/" + previousYear;
            }
        }

        Map<String, DepartmentCostRow> currentDepartmentCostMap = aggregateDepartmentCosts(currentPayrolls);
        Map<String, DepartmentCostRow> previousDepartmentCostMap = aggregateDepartmentCosts(previousPayrolls);

        Comparator<DepartmentCostRow> costComparator = sortDesc
                ? Comparator.comparingDouble(DepartmentCostRow::getTotalCost).reversed()
                .thenComparing(DepartmentCostRow::getDepartmentName, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparingDouble(DepartmentCostRow::getTotalCost)
                .thenComparing(DepartmentCostRow::getDepartmentName, String.CASE_INSENSITIVE_ORDER);

        List<DepartmentCostRow> costByDepartment = new ArrayList<>(currentDepartmentCostMap.values());
        costByDepartment.sort(costComparator);

        double totalSalaryCost = costByDepartment.stream().mapToDouble(DepartmentCostRow::getSalaryCost).sum();
        double totalCompensation = costByDepartment.stream().mapToDouble(DepartmentCostRow::getCompensationCost).sum();
        double totalEmployeeCost = totalSalaryCost + totalCompensation;
        int totalEmployeeCount = costByDepartment.stream().mapToInt(DepartmentCostRow::getEmployeeCount).sum();
        double averageCostPerEmployee = totalEmployeeCount == 0 ? 0.0 : totalEmployeeCost / totalEmployeeCount;
        List<CostCategoryRow> costByType = buildCostTypeRows(currentPayrolls, previousPayrolls);
        double maxTypeCost = costByType.stream()
                .mapToDouble(CostCategoryRow::getCurrentCost)
                .max()
                .orElse(0.0);
        CostCategoryRow topCostType = costByType.isEmpty() ? null : costByType.get(0);
        CostCategoryRow strongestTypeIncrease = costByType.stream()
                .filter(CostCategoryRow::isStrongIncrease)
                .max(Comparator.comparingDouble(CostCategoryRow::getChangePercent))
                .orElse(null);
        double maxChartValue = costByDepartment.stream()
                .mapToDouble(row -> Math.max(row.getSalaryCost(), row.getCompensationCost()))
                .max()
                .orElse(0.0);

        double pieStartDegree = 0.0;
        List<String> pieSegments = new ArrayList<>();

        for (int index = 0; index < costByDepartment.size(); index++) {
            DepartmentCostRow row = costByDepartment.get(index);
            double percent = totalEmployeeCost == 0 ? 0.0 : (row.getTotalCost() * 100.0) / totalEmployeeCost;
            row.setPercentOfTotal(round1(percent));
            row.setDominantDepartment(row.getPercentOfTotal() > 50.0);

            DepartmentCostRow previousRow = previousDepartmentCostMap.get(row.getDepartmentName());
            double previousTotal = previousRow == null ? 0.0 : previousRow.getTotalCost();
            row.setPreviousTotalCost(previousTotal);
            if (previousTotal <= 0.0) {
                row.setChangePercent(row.getTotalCost() > 0.0 ? 100.0 : 0.0);
            } else {
                row.setChangePercent(round1(((row.getTotalCost() - previousTotal) * 100.0) / previousTotal));
            }
            row.setStrongIncrease(previousTotal > 0.0 && row.getChangePercent() >= STRONG_INCREASE_THRESHOLD);

            String rowColor = row.isDominantDepartment()
                    ? "#d64545"
                    : row.isStrongIncrease()
                    ? "#d38a11"
                    : CHART_COLORS.get(index % CHART_COLORS.size());
            row.setColor(rowColor);

            double sweepDegree = (row.getPercentOfTotal() / 100.0) * 360.0;
            double pieEndDegree = Math.min(360.0, pieStartDegree + sweepDegree);
            pieSegments.add(rowColor + " " + round2(pieStartDegree) + "deg " + round2(pieEndDegree) + "deg");
            pieStartDegree = pieEndDegree;
        }
        if (pieStartDegree < 360.0) {
            pieSegments.add("#d7dfd7 " + round2(pieStartDegree) + "deg 360deg");
        }
        String pieChartGradient = pieSegments.isEmpty()
                ? "conic-gradient(#d7dfd7 0deg 360deg)"
                : "conic-gradient(" + String.join(", ", pieSegments) + ")";

        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("currentYear", currentYear);
        model.addAttribute("previousMonth", previousMonth);
        model.addAttribute("previousYear", previousYear);
        model.addAttribute("periodType", normalizedPeriodType);
        model.addAttribute("selectedDate", selectedDate.toString());
        model.addAttribute("quarter", selectedQuarter);
        model.addAttribute("periodSummary", periodSummary);
        model.addAttribute("comparisonSummary", comparisonSummary);
        model.addAttribute("sort", normalizedSort);
        model.addAttribute("totalSalaryCost", totalSalaryCost);
        model.addAttribute("totalCompensation", totalCompensation);
        model.addAttribute("totalEmployeeCost", totalEmployeeCost);
        model.addAttribute("averageCostPerEmployee", averageCostPerEmployee);
        model.addAttribute("costByType", costByType);
        model.addAttribute("maxTypeCost", maxTypeCost);
        model.addAttribute(
                "topCostTypeInsight",
                topCostType == null
                        ? "Chưa có dữ liệu."
                        : topCostType.getTypeName() + " đang chiếm cao nhất (" + topCostType.getPercentOfTotal() + "%)."
        );
        model.addAttribute(
                "abnormalTypeInsight",
                strongestTypeIncrease == null
                        ? "Chưa có khoản tăng bất thường."
                        : strongestTypeIncrease.getTypeName() + " đang tăng " + strongestTypeIncrease.getChangePercent() + "%."
        );
        model.addAttribute("maxChartValue", maxChartValue);
        model.addAttribute("pieChartGradient", pieChartGradient);
        model.addAttribute("costByDepartment", costByDepartment);
        model.addAttribute("pageTitle", "Dashboard Chi Phí Nhân Sự");
        model.addAttribute("contentTemplate", "employee-cost-dashboard");
        return "layout/base";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Không đủ quyền");
        model.addAttribute("errorTitle", "Truy cập bị từ chối");
        model.addAttribute("errorMessage", "Tài khoản hiện tại không đủ quyền thực hiện thao tác này.");
        return "error";
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, DepartmentCostRow> aggregateDepartmentCosts(List<Payroll> payrolls) {
        Map<String, DepartmentCostRow> departmentCostMap = new HashMap<>();
        for (Payroll payroll : payrolls) {
            String departmentName = resolveDepartmentName(payroll.getEmployee());
            DepartmentCostRow row = departmentCostMap.computeIfAbsent(departmentName, DepartmentCostRow::new);
            double salaryCost = safe(payroll.getBaseSalaryAmount()) + safe(payroll.getOvertimePay());
            double compensationCost = safe(payroll.getTotalAllowance()) + safe(payroll.getTotalBonus());
            double totalCost = salaryCost + compensationCost;

            row.setEmployeeCount(row.getEmployeeCount() + 1);
            row.setSalaryCost(row.getSalaryCost() + salaryCost);
            row.setCompensationCost(row.getCompensationCost() + compensationCost);
            row.setTotalCost(row.getTotalCost() + totalCost);
        }
        return departmentCostMap;
    }

    private List<CostCategoryRow> buildCostTypeRows(List<Payroll> currentPayrolls, List<Payroll> previousPayrolls) {
        double currentSalary = 0.0;
        double currentBonus = 0.0;
        double currentAllowance = 0.0;
        double currentInsurance = 0.0;
        double currentBenefits = 0.0;
        for (Payroll payroll : currentPayrolls) {
            currentSalary += safe(payroll.getBaseSalaryAmount()) + safe(payroll.getOvertimePay());
            currentBonus += safe(payroll.getTotalBonus());
            currentAllowance += safe(payroll.getTotalAllowance());
            currentInsurance += safe(payroll.getInsuranceAmount());
            currentBenefits += safe(payroll.getOtherDeductionAmount());
        }

        double previousSalary = 0.0;
        double previousBonus = 0.0;
        double previousAllowance = 0.0;
        double previousInsurance = 0.0;
        double previousBenefits = 0.0;
        for (Payroll payroll : previousPayrolls) {
            previousSalary += safe(payroll.getBaseSalaryAmount()) + safe(payroll.getOvertimePay());
            previousBonus += safe(payroll.getTotalBonus());
            previousAllowance += safe(payroll.getTotalAllowance());
            previousInsurance += safe(payroll.getInsuranceAmount());
            previousBenefits += safe(payroll.getOtherDeductionAmount());
        }

        List<CostCategoryRow> rows = new ArrayList<>();
        CostCategoryRow salaryRow = new CostCategoryRow("Lương (salary)", "#2a7a54");
        salaryRow.setCurrentCost(currentSalary);
        salaryRow.setPreviousCost(previousSalary);
        rows.add(salaryRow);

        CostCategoryRow bonusRow = new CostCategoryRow("Thưởng (bonus)", "#cf8a3d");
        bonusRow.setCurrentCost(currentBonus);
        bonusRow.setPreviousCost(previousBonus);
        rows.add(bonusRow);

        CostCategoryRow allowanceRow = new CostCategoryRow("Phụ cấp (allowance)", "#4c9f70");
        allowanceRow.setCurrentCost(currentAllowance);
        allowanceRow.setPreviousCost(previousAllowance);
        rows.add(allowanceRow);

        CostCategoryRow insuranceRow = new CostCategoryRow("Bảo hiểm (insurance)", "#587fa9");
        insuranceRow.setCurrentCost(currentInsurance);
        insuranceRow.setPreviousCost(previousInsurance);
        rows.add(insuranceRow);

        CostCategoryRow benefitsRow = new CostCategoryRow("Phúc lợi khác (benefits)", "#7d8fa6");
        benefitsRow.setCurrentCost(currentBenefits);
        benefitsRow.setPreviousCost(previousBenefits);
        rows.add(benefitsRow);

        double totalCurrentCost = rows.stream().mapToDouble(CostCategoryRow::getCurrentCost).sum();
        for (CostCategoryRow row : rows) {
            double percent = totalCurrentCost == 0.0 ? 0.0 : (row.getCurrentCost() * 100.0) / totalCurrentCost;
            row.setPercentOfTotal(round1(percent));

            if (row.getPreviousCost() <= 0.0) {
                row.setChangePercent(row.getCurrentCost() > 0.0 ? 100.0 : 0.0);
            } else {
                row.setChangePercent(round1(((row.getCurrentCost() - row.getPreviousCost()) * 100.0) / row.getPreviousCost()));
            }
            row.setStrongIncrease(row.getPreviousCost() > 0.0 && row.getChangePercent() >= STRONG_INCREASE_THRESHOLD);
        }

        rows.sort(
                Comparator.comparingDouble(CostCategoryRow::getCurrentCost)
                        .reversed()
                        .thenComparing(CostCategoryRow::getTypeName, String.CASE_INSENSITIVE_ORDER)
        );
        return rows;
    }

    private String resolveDepartmentName(Employee employee) {
        if (employee != null && employee.getDepartment() != null && employee.getDepartment().getName() != null
                && !employee.getDepartment().getName().isBlank()) {
            return normalizeDepartmentName(employee.getDepartment().getName());
        }
        return "Chưa phân phòng ban";
    }

    private String normalizeDepartmentName(String name) {
        String compact = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (compact.isBlank()) {
            return "Chưa phân phòng ban";
        }

        String asciiKey = Normalizer.normalize(compact, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase();

        return switch (asciiKey) {
            case "nhan su" -> "Nhân sự";
            case "ke toan" -> "Kế toán";
            case "cong nghe thong tin" -> "Công nghệ thông tin";
            default -> compact;
        };
    }

    private String normalizePeriodType(String periodType) {
        if ("day".equalsIgnoreCase(periodType)) {
            return "day";
        }
        if ("quarter".equalsIgnoreCase(periodType)) {
            return "quarter";
        }
        if ("year".equalsIgnoreCase(periodType)) {
            return "year";
        }
        return "month";
    }

    private int monthToQuarter(int month) {
        return ((month - 1) / 3) + 1;
    }

    private LocalDate parseDateOrDefault(String date, LocalDate fallback) {
        if (date == null || date.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private List<YearMonth> getQuarterMonths(int year, int quarter) {
        int startMonth = ((quarter - 1) * 3) + 1;
        return List.of(
                YearMonth.of(year, startMonth),
                YearMonth.of(year, startMonth + 1),
                YearMonth.of(year, startMonth + 2)
        );
    }

    private List<YearMonth> getYearMonths(int year) {
        List<YearMonth> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            months.add(YearMonth.of(year, month));
        }
        return months;
    }

    private List<Payroll> loadPayrollsByMonths(List<YearMonth> periods) {
        List<Payroll> result = new ArrayList<>();
        for (YearMonth period : periods) {
            result.addAll(payrollService.findByMonthWithEmployeeAndDepartment(period.getMonthValue(), period.getYear()));
        }
        return result;
    }

    @Data
    public static class DepartmentCostRow {
        private final String departmentName;
        private int employeeCount;
        private double salaryCost;
        private double compensationCost;
        private double totalCost;
        private double percentOfTotal;
        private double previousTotalCost;
        private double changePercent;
        private boolean strongIncrease;
        private boolean dominantDepartment;
        private String color;
    }

    @Data
    public static class CostCategoryRow {
        private final String typeName;
        private final String color;
        private double currentCost;
        private double previousCost;
        private double percentOfTotal;
        private double changePercent;
        private boolean strongIncrease;
    }
}
