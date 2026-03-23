package com.example.QuanLyLuong.repository;

import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.common.PaymentStatus;
import com.example.QuanLyLuong.entity.Payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    Optional<Payroll> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    List<Payroll> findByMonthAndYearOrderByEmployeeFullNameAsc(Integer month, Integer year);

    List<Payroll> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("""
            select coalesce(sum(p.actualSalary), 0)
            from Payroll p
            where p.month = :month and p.year = :year
            """)
    Double sumTotalSalaryByMonthAndYear(@Param("month") Integer month, @Param("year") Integer year);

    @Query("""
            select p
            from Payroll p
            where p.employee.department.id = :departmentId
              and p.month = :month
              and p.year = :year
            order by p.employee.fullName asc
            """)
    List<Payroll> findByDepartmentAndMonthAndYear(
            @Param("departmentId") Long departmentId,
            @Param("month") Integer month,
            @Param("year") Integer year
    );
}
