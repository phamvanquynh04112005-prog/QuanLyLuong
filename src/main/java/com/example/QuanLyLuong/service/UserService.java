package com.example.QuanLyLuong.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.QuanLyLuong.common.Role;
import com.example.QuanLyLuong.dto.AccountProvisionResult;
import com.example.QuanLyLuong.dto.BulkAccountProvisionResult;
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
    private final AccountCredentialMailService accountCredentialMailService;

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

    @Transactional(readOnly = true)
    public List<Employee> findEmployeesForAccountManagement(boolean missingOnly) {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        return missingOnly
                ? employeeRepository.findByUserIsNullOrderByFullNameAsc()
                : employeeRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public long countEmployeesWithoutAccount() {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        return employeeRepository.countByUserIsNull();
    }

    @Transactional(readOnly = true)
    public List<User> findStandaloneUsers() {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        return userRepository.findAllByEmployeeIsNullOrderByUsernameAsc();
    }

    public User createUser(Long employeeId, String username, String rawPassword, Role role, Boolean enabled) {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        return saveNewUser(resolveEmployee(employeeId), username, rawPassword, role, enabled);
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

    public AccountProvisionResult provisionAccountForEmployee(Long employeeId) {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        Employee employee = resolveEmployee(employeeId);
        if (userRepository.findByEmployeeId(employeeId).isPresent()) {
            throw new IllegalArgumentException("Nhân viên này đã có tài khoản đăng nhập.");
        }
        return provisionForEmployee(employee);
    }

    public BulkAccountProvisionResult provisionAccountsForEmployeesWithoutAccount() {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        List<Employee> employees = employeeRepository.findByUserIsNullOrderByFullNameAsc();
        if (employees.isEmpty()) {
            return BulkAccountProvisionResult.builder()
                    .createdCount(0)
                    .emailedCount(0)
                    .skippedCount(0)
                    .warnings(List.of())
                    .build();
        }

        int createdCount = 0;
        int emailedCount = 0;
        List<String> warnings = new ArrayList<>();

        for (Employee employee : employees) {
            AccountProvisionResult result = provisionForEmployee(employee);
            createdCount++;
            if (result.isEmailSent()) {
                emailedCount++;
            } else {
                warnings.add(employee.getFullName() + ": " + result.getEmailError());
            }
        }

        return BulkAccountProvisionResult.builder()
                .createdCount(createdCount)
                .emailedCount(emailedCount)
                .skippedCount(0)
                .warnings(warnings)
                .build();
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

    private User saveNewUser(Employee employee, String username, String rawPassword, Role role, Boolean enabled) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại: " + username);
        }
        validateEmployeeBinding(null, employee != null ? employee.getId() : null);

        User user = new User();
        user.setEmployee(employee);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(enabled == null ? Boolean.TRUE : enabled);
        return userRepository.save(user);
    }

    private AccountProvisionResult provisionForEmployee(Employee employee) {
        Role role = resolveProvisionRole(employee);
        String username = generateNextUsername(role);
        String rawPassword = username + "@";
        User user = saveNewUser(employee, username, rawPassword, role, true);

        boolean emailSent = true;
        String emailError = null;
        try {
            accountCredentialMailService.sendAccountCredentials(employee, user, rawPassword);
        } catch (IllegalStateException exception) {
            emailSent = false;
            emailError = exception.getMessage();
        }

        return AccountProvisionResult.builder()
                .employee(employee)
                .user(user)
                .rawPassword(rawPassword)
                .emailSent(emailSent)
                .emailError(emailError)
                .build();
    }

    private Role resolveProvisionRole(Employee employee) {
        String employeeText = normalizeRoleText(employee.getPosition());
        String departmentText = employee.getDepartment() == null ? "" : normalizeRoleText(employee.getDepartment().getName());
        String combined = employeeText + " " + departmentText;

        if (containsAny(combined, "nhan su", "nhansu", "human resource", "human resources", " hr ")) {
            return Role.ROLE_HR;
        }
        if (containsAny(combined, "ke toan", "ketoan", "account", "accountant", "tai chinh", "finance")) {
            return Role.ROLE_ACCOUNTANT;
        }
        return Role.ROLE_EMPLOYEE;
    }

    private String generateNextUsername(Role role) {
        String prefix = switch (role) {
            case ROLE_HR -> "hr";
            case ROLE_ACCOUNTANT -> "kt";
            case ROLE_EMPLOYEE -> "nv";
            case ROLE_SYSTEM_ADMIN -> "admin";
        };

        int width = role == Role.ROLE_EMPLOYEE ? 3 : 2;
        int sequence = 1;
        while (true) {
            String candidate = prefix + String.format("%0" + width + "d", sequence);
            if (userRepository.findByUsername(candidate).isEmpty()) {
                return candidate;
            }
            sequence++;
        }
    }

    private String normalizeRoleText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return (" " + normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim() + " ").replaceAll("\\s+", " ");
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
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
