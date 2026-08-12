package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.dto.LoginResponse;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.flatizy.flatizy.repository.UserRepository;
import org.flatizy.flatizy.security.AesEncryptionService;
import org.flatizy.flatizy.security.JwtService;
import org.flatizy.flatizy.security.SecurityEventLogger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventLogger securityEventLogger;
    private final AesEncryptionService aesEncryptionService;

    public LoginResponse login(String email, String password, String ip) {
        try {
            User user = userRepository.findAll().stream()
                    .filter(u -> {
                        try {
                            return email.equals(aesEncryptionService.decrypt(u.getEmail()));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!passwordEncoder.matches(password, user.getPassword())) {
                securityEventLogger.logLoginAttempt(email, ip, false);
                throw new RuntimeException("Wrong password");
            }

            if (user.getRole() != UserRole.ADMIN) {
                securityEventLogger.logLoginAttempt(email, ip, false);
                throw new RuntimeException("No CRM access");
            }

            securityEventLogger.logLoginAttempt(email, ip, true);
            String token = jwtService.generateToken(user);
            return new LoginResponse(token, user.getRole().getValue());

        } catch (RuntimeException e) {
            throw e;
        }
    }
}