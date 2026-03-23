package com.example.QuanLyLuong.repository;

import java.util.Optional;

import com.example.QuanLyLuong.entity.Department;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByNameIgnoreCase(String name);
}
