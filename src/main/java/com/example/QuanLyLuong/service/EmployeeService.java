package com.example.QuanLyLuong.service;

import java.util.List;

import com.example.QuanLyLuong.entity.Department;
import com.example.QuanLyLuong.entity.Employee;
import com.example.QuanLyLuong.exception.ResourceNotFoundException;
import com.example.QuanLyLuong.repository.DepartmentRepository;
import com.example.QuanLyLuong.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien co ID: " + id));
    }

    public Employee save(Employee employee) {
        employee.setDepartment(resolveDepartment(employee));
        employee.setEmployeeCode(normalizeCode(employee.getEmployeeCode()));
        employee.setEmail(normalizeText(employee.getEmail()));
        employee.setFullName(normalizeText(employee.getFullName()));
        employee.setPosition(normalizeText(employee.getPosition()));
        employee.setDependentCount(Math.max(0, employee.getDependentCount() == null ? 0 : employee.getDependentCount()));
        Employee savedEmployee = employeeRepository.save(employee);
        return ensureGeneratedCode(savedEmployee);
    }

    public Employee update(Long id, Employee updatedEmployee) {
        Employee existing = findById(id);
        existing.setFullName(normalizeText(updatedEmployee.getFullName()));
        existing.setEmail(normalizeText(updatedEmployee.getEmail()));
        existing.setPosition(normalizeText(updatedEmployee.getPosition()));
        existing.setBaseSalary(updatedEmployee.getBaseSalary());
        existing.setJoinDate(updatedEmployee.getJoinDate());
        existing.setDependentCount(Math.max(0, updatedEmployee.getDependentCount() == null ? 0 : updatedEmployee.getDependentCount()));
        existing.setStatus(updatedEmployee.getStatus());
        existing.setDepartment(resolveDepartment(updatedEmployee));
        String requestedCode = normalizeCode(updatedEmployee.getEmployeeCode());
        if (requestedCode != null) {
            existing.setEmployeeCode(requestedCode);
        }
        Employee savedEmployee = employeeRepository.save(existing);
        return ensureGeneratedCode(savedEmployee);
    }

    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Employee> searchByName(String keyword) {
        return employeeRepository.findByFullNameContainingIgnoreCaseOrderByFullNameAsc(keyword);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByDepartment(Long departmentId) {
        return employeeRepository.findByDepartmentIdOrderByFullNameAsc(departmentId);
    }

    private Department resolveDepartment(Employee employee) {
        if (employee.getDepartment() == null || employee.getDepartment().getId() == null) {
            return null;
        }
        return departmentRepository.findById(employee.getDepartment().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay phong ban co ID: " + employee.getDepartment().getId()));
    }

    private Employee ensureGeneratedCode(Employee employee) {
        if (employee.getEmployeeCode() == null || employee.getEmployeeCode().isBlank()) {
            employee.setEmployeeCode(String.format("EMP%04d", employee.getId()));
            return employeeRepository.save(employee);
        }
        return employee;
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}