package org.flatizy.flatizy.controller;

import lombok.AllArgsConstructor;
import org.flatizy.flatizy.entity.dto.LoginRequest;
import org.flatizy.flatizy.entity.dto.LoginResponse;
import org.flatizy.flatizy.security.AesEncryptionService;
import org.flatizy.flatizy.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AesEncryptionService aesEncryptionService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword());
    }

    @GetMapping("/auth/encrypt")
    public String encrypt(@RequestParam String value) {
        return aesEncryptionService.encrypt(value);
    }
}
