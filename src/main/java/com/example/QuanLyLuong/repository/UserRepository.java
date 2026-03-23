package com.example.QuanLyLuong.repository;

import java.util.List;
import java.util.Optional;

import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmployeeId(Long employeeId);

    List<User> findAllByOrderByUsernameAsc();

    long countByRole(Role role);
}
