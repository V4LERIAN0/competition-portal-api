package com.wodnsivar.competitionportal.config;

import com.wodnsivar.competitionportal.enums.UserRole;
import com.wodnsivar.competitionportal.user.entity.UserAccount;
import com.wodnsivar.competitionportal.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createDefaultAdmin();
    }

    private void createDefaultAdmin() {
        String adminEmail = "admin@sivarfest.fit";

        if (userAccountRepository.existsByEmail(adminEmail)) {
            return;
        }

        UserAccount admin = UserAccount.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();

        userAccountRepository.save(admin);

        System.out.println("Default dev admin created:");
        System.out.println("Email: admin@sivarfest.fit");
        System.out.println("Password: Admin123!");
    }
}