package com.example.QuanLyLuong.repository;

import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.entity.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findAllByOrderByFullNameAsc();

    List<Employee> findByDepartmentIdOrderByFullNameAsc(Long departmentId);

    List<Employee> findByStatusOrderByFullNameAsc(EmployeeStatus status);

    Optional<Employee> findByEmailIgnoreCase(String email);

    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);

    List<Employee> findByFullNameContainingIgnoreCaseOrderByFullNameAsc(String keyword);

    long countByStatus(EmployeeStatus status);
}