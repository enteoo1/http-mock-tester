package com.example.mocktester.service;

import com.example.mocktester.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates admin/admin on the very first start. The password must be changed at first login. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() == 0) {
            users.insert("admin", encoder.encode("admin"), "ADMIN", "UTC", true, TimeUtil.nowDb());
            log.warn("Created default administrator 'admin' with password 'admin'. "
                    + "It must be changed at first login.");
        }
    }
}
