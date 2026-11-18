package com.swiftdrop.auth.config;

import com.swiftdrop.auth.domain.Role;
import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "wslrperera@gmail.com";
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    if (!Role.ADMIN.equals(user.getRole()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
                        user.setRole(Role.ADMIN);
                        user.setStatus("ACTIVE");
                        user.setEnabled(true);
                        userRepository.save(user);
                        log.info("Elevated existing user {} to ROLE_ADMIN", adminEmail);
                    }
                },
                () -> {
                    User adminUser = User.builder()
                            .email(adminEmail)
                            .fullName("Sadeepa Perera")
                            .role(Role.ADMIN)
                            .status("ACTIVE")
                            .enabled(true)
                            .provider("GOOGLE")
                            .passwordHash(passwordEncoder.encode("Admin@123"))
                            .build();
                    userRepository.save(adminUser);
                    log.info("Initialized default admin user: {}", adminEmail);
                }
        );

        // Seed representative demo users across all roles for development & evaluation
        createIfMissing("customer@swiftdrop.lk", "Kasun Silva", "0771234567", Role.CUSTOMER, "ACTIVE", true, "Customer@123");
        createIfMissing("merchant@swiftdrop.lk", "TechZone Store", "0719876543", Role.MERCHANT, "ACTIVE", true, "Merchant@123");
        createIfMissing("courier@swiftdrop.lk", "Kamal Perera", "0751122334", Role.COURIER, "ACTIVE", true, "Courier@123");
        createIfMissing("courier.pending@swiftdrop.lk", "Nuwan Pradeep (Pending)", "0765544332", Role.COURIER, "PENDING_APPROVAL", false, "Courier@123");
        createIfMissing("dispatcher@swiftdrop.lk", "Suneth Jayasinghe", "0789988776", Role.DISPATCHER, "ACTIVE", true, "Dispatcher@123");
    }

    private void createIfMissing(String email, String fullName, String phone, String role, String status, boolean enabled, String password) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .phoneNumber(phone)
                    .role(role)
                    .status(status)
                    .enabled(enabled)
                    .provider("LOCAL")
                    .passwordHash(passwordEncoder.encode(password))
                    .build();
            userRepository.save(user);
            log.info("Initialized demo user: {} ({})", email, role);
        }
    }
}
