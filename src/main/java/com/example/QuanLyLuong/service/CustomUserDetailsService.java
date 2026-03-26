package com.example.QuanLyLuong.service;

import com.example.QuanLyLuong.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AccountLifecycleService accountLifecycleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        accountLifecycleService.purgeExpiredInactiveAccounts();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));
    }
}