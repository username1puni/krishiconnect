package com.krishiconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.krishiconnect.entity.Role;
import com.krishiconnect.entity.User;
import com.krishiconnect.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD so no password lives in the code. */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Value("${app.admin.phone:9000000001}")
    private String phone;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.info("ADMIN_EMAIL / ADMIN_PASSWORD not set, skipping admin creation");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User admin = new User("Admin", phone, email.toLowerCase(), passwordEncoder.encode(password), Role.ADMIN);
        admin.setVerified(true);
        userRepository.save(admin);
        log.info("Created admin account {}", email);
    }
}
