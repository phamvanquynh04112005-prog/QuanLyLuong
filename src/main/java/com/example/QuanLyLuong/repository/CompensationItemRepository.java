package com.example.QuanLyLuong.repository;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.common.CompensationItemType;
import com.example.QuanLyLuong.entity.CompensationItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompensationItemRepository extends JpaRepository<CompensationItem, Long> {

    List<CompensationItem> findByEmployeeIdOrderByEffectiveDateDescNameAsc(Long employeeId);

    @Query("""
            select c
            from CompensationItem c
            where c.employee.id = :employeeId
              and c.active = true
              and c.effectiveDate <= :endDate
              and (c.endDate is null or c.endDate >= :startDate)
            order by c.componentType asc, c.name asc
            """)
    List<CompensationItem> findApplicableItems(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<CompensationItem> findByEmployeeIdAndComponentTypeOrderByEffectiveDateDescNameAsc(Long employeeId, CompensationItemType componentType);
}