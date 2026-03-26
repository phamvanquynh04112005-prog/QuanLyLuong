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
    private final AccountLifecycleService accountLifecycleService;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        return userRepository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản có ID: " + id));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
    }

    public User createUser(Long employeeId, String username, String rawPassword, Role role, Boolean enabled) {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại: " + username);
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
        accountLifecycleService.purgeExpiredInactiveAccounts();
        User user = findById(id);
        if (!user.getUsername().equals(username) && userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại: " + username);
        }
        validateEmployeeBinding(id, employeeId);
        validateSystemAdminRetention(user, role, enabled == null ? Boolean.TRUE : enabled);

        user.setEmployee(resolveEmployee(employeeId));
        user.setUsername(username);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        user.setRole(role);
        user.setEnabled(enabled == null ? Boolean.TRUE : enabled);
        return userRepository.save(user);
    }

    public User toggleEnabled(Long id) {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        User user = findById(id);
        boolean nextEnabled = !Boolean.TRUE.equals(user.getEnabled());
        validateSystemAdminRetention(user, user.getRole(), nextEnabled);
        user.setEnabled(nextEnabled);
        return userRepository.save(user);
    }

    public void delete(Long id) {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        User user = findById(id);
        validateSystemAdminRemoval(user);
        userRepository.delete(user);
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên có ID: " + employeeId));
    }

    private void validateEmployeeBinding(Long userId, Long employeeId) {
        if (employeeId == null) {
            return;
        }
        userRepository.findByEmployeeId(employeeId)
                .filter(existingUser -> !existingUser.getId().equals(userId))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("Nhân viên này đã được gán với tài khoản khác.");
                });
    }

    private void validateSystemAdminRetention(User existingUser, Role targetRole, boolean targetEnabled) {
        boolean currentlyEnabledSystemAdmin = existingUser.getRole() == Role.ROLE_SYSTEM_ADMIN && Boolean.TRUE.equals(existingUser.getEnabled());
        boolean remainsEnabledSystemAdmin = targetRole == Role.ROLE_SYSTEM_ADMIN && targetEnabled;
        if (currentlyEnabledSystemAdmin && !remainsEnabledSystemAdmin
                && userRepository.countByRoleAndEnabledTrue(Role.ROLE_SYSTEM_ADMIN) <= 1) {
            throw new IllegalArgumentException("Hệ thống phải còn ít nhất một System Admin đang hoạt động.");
        }
    }

    private void validateSystemAdminRemoval(User user) {
        if (user.getRole() == Role.ROLE_SYSTEM_ADMIN && Boolean.TRUE.equals(user.getEnabled())
                && userRepository.countByRoleAndEnabledTrue(Role.ROLE_SYSTEM_ADMIN) <= 1) {
            throw new IllegalArgumentException("Không thể xóa System Admin cuối cùng đang hoạt động.");
        }
    }
}