package com.example.QuanLyLuong.service;

import org.springframework.stereotype.Service;

@Service
public class VietnamPersonalIncomeTaxService {

    private static final double[] MONTHLY_BRACKETS = {
            5_000_000d,
            10_000_000d,
            18_000_000d,
            32_000_000d,
            52_000_000d,
            80_000_000d,
            Double.MAX_VALUE
    };

    private static final double[] TAX_RATES = {
            0.05d,
            0.10d,
            0.15d,
            0.20d,
            0.25d,
            0.30d,
            0.35d
    };

    public TaxComputation calculateMonthlyTax(double taxableGrossIncome,
                                              double insuranceAmount,
                                              int dependentCount,
                                              double personalDeduction,
                                              double dependentDeductionPerPerson) {
        double safeDependentCount = Math.max(0, dependentCount);
        double safePersonalDeduction = Math.max(0.0, personalDeduction);
        double safeDependentDeductionPerPerson = Math.max(0.0, dependentDeductionPerPerson);
        double dependentDeductionAmount = safeDependentCount * safeDependentDeductionPerPerson;
        double taxableIncome = Math.max(0.0, taxableGrossIncome - Math.max(0.0, insuranceAmount) - safePersonalDeduction - dependentDeductionAmount);
        double taxAmount = calculateProgressiveTax(taxableIncome);
        return new TaxComputation(round2(taxableIncome), round2(dependentDeductionAmount), round2(taxAmount));
    }

    private double calculateProgressiveTax(double taxableIncome) {
        if (taxableIncome <= 0) {
            return 0.0;
        }

        double remainingIncome = taxableIncome;
        double previousCap = 0.0;
        double taxAmount = 0.0;

        for (int index = 0; index < MONTHLY_BRACKETS.length && remainingIncome > 0; index++) {
            double currentCap = MONTHLY_BRACKETS[index];
            double taxablePortion = Math.min(remainingIncome, currentCap - previousCap);
            taxAmount += taxablePortion * TAX_RATES[index];
            remainingIncome -= taxablePortion;
            previousCap = currentCap;
        }
        return taxAmount;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record TaxComputation(double taxableIncome, double dependentDeductionAmount, double taxAmount) {
    }
}