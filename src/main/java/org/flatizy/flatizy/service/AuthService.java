package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.dto.LoginResponse;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.flatizy.flatizy.repository.UserRepository;
import org.flatizy.flatizy.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Input password: '{}'", password);
        log.info("Stored hash: '{}'", user.getPassword());
        log.info("Matches: {}", passwordEncoder.matches(password, user.getPassword()));

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new RuntimeException("Wrong password");

        if (user.getRole() != UserRole.ADMIN)
            throw new RuntimeException("No CRM access");

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getRole().getValue());
    }
}