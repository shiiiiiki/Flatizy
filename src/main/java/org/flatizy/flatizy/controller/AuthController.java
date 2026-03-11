package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.entity.dto.LoginRequest;
import org.flatizy.flatizy.entity.dto.user.UserDto;
import org.flatizy.flatizy.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public UserDto login(@RequestBody LoginRequest request) {
        return authService.login(
                request.getEmail(),
                request.getPassword()
        );
    }
}
