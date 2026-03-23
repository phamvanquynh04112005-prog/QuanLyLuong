package com.example.QuanLyLuong.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.entity.SalaryConfig;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryConfigRepository extends JpaRepository<SalaryConfig, Long> {

    List<SalaryConfig> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);

    Optional<SalaryConfig> findTopByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);

    Optional<SalaryConfig> findTopByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            Long employeeId,
            LocalDate effectiveDate
    );
}
