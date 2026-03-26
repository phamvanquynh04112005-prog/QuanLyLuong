package com.example.QuanLyLuong.service;

import java.time.LocalDate;
import java.util.List;

import com.example.QuanLyLuong.common.EmployeeStatus;
import com.example.QuanLyLuong.entity.User;
import com.example.QuanLyLuong.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountLifecycleService {

    private final UserRepository userRepository;

    public int purgeExpiredInactiveAccounts() {
        LocalDate cutoffDate = LocalDate.now().minusMonths(6);
        List<User> expiredUsers = userRepository.findAllByEmployeeStatusAndEmployeeInactiveSinceBefore(
                EmployeeStatus.INACTIVE,
                cutoffDate
        );
        if (expiredUsers.isEmpty()) {
            return 0;
        }
        userRepository.deleteAllInBatch(expiredUsers);
        return expiredUsers.size();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledPurgeExpiredInactiveAccounts() {
        purgeExpiredInactiveAccounts();
    }
}