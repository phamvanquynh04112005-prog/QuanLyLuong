package com.example.QuanLyLuong.config;

import com.example.QuanLyLuong.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/images/**", "/login", "/h2-console/**").permitAll()
                        .requestMatchers("/admin/**").hasAuthority("ROLE_SYSTEM_ADMIN")
                        .requestMatchers("/employees/**", "/salary-configs/**", "/timesheets/**", "/compensation-items/**")
                        .hasAuthority("ROLE_HR")
                        .requestMatchers("/payrolls/my", "/export/pdf/payslip/**")
                        .hasAnyAuthority("ROLE_HR", "ROLE_ACCOUNTANT", "ROLE_EMPLOYEE")
                        .requestMatchers("/employee-cost-dashboard", "/employee-cost-dashboard/**")
                        .hasAuthority("ROLE_ACCOUNTANT")
                        .requestMatchers("/reports/**", "/payrolls/**", "/export/excel/**", "/export/pdf/payroll/**")
                        .hasAuthority("ROLE_ACCOUNTANT")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("quan-ly-luong-remember-me")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                        .userDetailsService(customUserDetailsService)
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .userDetailsService(customUserDetailsService)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}