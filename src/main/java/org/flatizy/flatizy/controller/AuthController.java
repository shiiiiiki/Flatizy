package org.flatizy.flatizy.controller;

import lombok.AllArgsConstructor;
import org.flatizy.flatizy.entity.dto.LoginRequest;
import org.flatizy.flatizy.entity.dto.LoginResponse;
import org.flatizy.flatizy.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword());
    }
}
