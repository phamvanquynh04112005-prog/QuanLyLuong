package com.example.QuanLyLuong.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(JakartaWebServlet.class)
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
public class H2ConsoleConfig {

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet(
            @Value("${spring.h2.console.path:/h2-console}") String consolePath) {
        String normalizedPath = normalizePath(consolePath);
        ServletRegistrationBean<JakartaWebServlet> registrationBean =
                new ServletRegistrationBean<>(new JakartaWebServlet(), normalizedPath, normalizedPath + "/*");
        registrationBean.setLoadOnStartup(1);

        Map<String, String> initParameters = new LinkedHashMap<>();
        initParameters.put("webAllowOthers", "false");
        initParameters.put("trace", "false");
        registrationBean.setInitParameters(initParameters);
        return registrationBean;
    }

    private String normalizePath(String consolePath) {
        if (consolePath == null || consolePath.isBlank()) {
            return "/h2-console";
        }
        String path = consolePath.startsWith("/") ? consolePath : "/" + consolePath;
        return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    }
}