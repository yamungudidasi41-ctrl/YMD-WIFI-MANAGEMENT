package com.ymd.wifi.service;

import com.ymd.wifi.model.Admin;
import com.ymd.wifi.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner createDefaultAdmin(AdminRepository admins) {
        return args -> {
            if (admins.findByUsername("admin").isEmpty()) {
                String hash = new BCryptPasswordEncoder().encode("ChangeMe123!");
                admins.save(new Admin("admin", hash));
                System.out.println("DEFAULT ADMIN CREATED: username=admin, password=ChangeMe123! (CHANGE IT)");
            }
        };
    }
}
