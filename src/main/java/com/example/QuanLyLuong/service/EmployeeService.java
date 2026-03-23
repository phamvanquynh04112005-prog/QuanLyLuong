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
        return employeeRepository.save(employee);
    }

    public Employee update(Long id, Employee updatedEmployee) {
        Employee existing = findById(id);
        existing.setFullName(updatedEmployee.getFullName());
        existing.setEmail(updatedEmployee.getEmail());
        existing.setPosition(updatedEmployee.getPosition());
        existing.setBaseSalary(updatedEmployee.getBaseSalary());
        existing.setJoinDate(updatedEmployee.getJoinDate());
        existing.setStatus(updatedEmployee.getStatus());
        existing.setDepartment(resolveDepartment(updatedEmployee));
        return employeeRepository.save(existing);
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
}
