package com.example.QuanLyLuong.service;

import java.util.List;

import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.entity.User;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.EmployeeRepository;
import com.example.QuanLyLuong.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan co ID: " + id));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay tai khoan: " + username));
    }

    public User createUser(Long employeeId, String username, String rawPassword, Role role, Boolean enabled) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Ten dang nhap da ton tai: " + username);
        }
        validateEmployeeBinding(null, employeeId);

        User user = new User();
        user.setEmployee(resolveEmployee(employeeId));
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(enabled == null ? Boolean.TRUE : enabled);
        return userRepository.save(user);
    }

    public User updateUser(Long id, Long employeeId, String username, String rawPassword, Role role, Boolean enabled) {
        User user = findById(id);
        if (!user.getUsername().equals(username) && userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Ten dang nhap da ton tai: " + username);
        }
        validateEmployeeBinding(id, employeeId);

        user.setEmployee(resolveEmployee(employeeId));
        user.setUsername(username);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        user.setRole(role);
        user.setEnabled(enabled == null ? Boolean.TRUE : enabled);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public void changePassword(String username, String newPassword) {
        User user = findByUsername(username);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getAuthenticatedUser(Authentication authentication) {
        return findByUsername(authentication.getName());
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeFromAuthentication(Authentication authentication) {
        return getAuthenticatedUser(authentication).getEmployee();
    }

    private Employee resolveEmployee(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien co ID: " + employeeId));
    }

    private void validateEmployeeBinding(Long userId, Long employeeId) {
        if (employeeId == null) {
            return;
        }
        userRepository.findByEmployeeId(employeeId)
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Nhan vien nay da duoc gan voi tai khoan khac.");
                });
    }
}
