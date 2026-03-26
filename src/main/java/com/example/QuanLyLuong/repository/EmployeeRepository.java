package com.example.QuanLyLuong.repository;

import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.entity.Employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @EntityGraph(attributePaths = {"department", "user"})
    List<Employee> findAllByOrderByFullNameAsc();

    List<Employee> findByDepartmentIdOrderByFullNameAsc(Long departmentId);

    List<Employee> findByStatusOrderByFullNameAsc(EmployeeStatus status);

    @EntityGraph(attributePaths = {"department", "user"})
    List<Employee> findByUserIsNullOrderByFullNameAsc();

    Optional<Employee> findByEmailIgnoreCase(String email);

    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);

    List<Employee> findByFullNameContainingIgnoreCaseOrderByFullNameAsc(String keyword);

    List<Employee> findByFullNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCaseOrderByFullNameAsc(
            String fullNameKeyword,
            String employeeCodeKeyword
    );

    long countByStatus(EmployeeStatus status);

    long countByUserIsNull();
}
