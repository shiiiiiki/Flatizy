package org.flatizy.flatizy.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.flatizy.flatizy.entity.dto.LoginRequest;
import org.flatizy.flatizy.entity.dto.LoginResponse;
import org.flatizy.flatizy.security.AesEncryptionService;
import org.flatizy.flatizy.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AesEncryptionService aesEncryptionService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request,
                               HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For") != null
                ? httpRequest.getHeader("X-Forwarded-For").split(",")[0]
                : httpRequest.getRemoteAddr();
        return authService.login(request.getEmail(), request.getPassword(), ip);
    }

    @GetMapping("/auth/encrypt")
    public String encrypt(@RequestParam String value) {
        return aesEncryptionService.encrypt(value);
    }

    @PostMapping("/auth/decrypt")
    public String decrypt(@RequestBody Map<String, String> body) {
        return aesEncryptionService.decrypt(body.get("email"));
    }
}
