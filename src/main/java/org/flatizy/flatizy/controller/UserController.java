package org.flatizy.flatizy.controller;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.dto.response.UserSaveResponse;
import org.flatizy.flatizy.entity.dto.user.UserDto;
import org.flatizy.flatizy.entity.dto.user.UserRegistrationDto;
import org.flatizy.flatizy.service.user.UserRegistrationService;
import org.flatizy.flatizy.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/api/users")
@RestController
public class UserController {

    private final UserService userService;
    private final UserRegistrationService registrationService;

    public UserController(UserService userService, UserRegistrationService registrationService) {
        this.userService = userService;
        this.registrationService = registrationService;
    }

    @GetMapping()
    public ResponseEntity<List<UserDto>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @PostMapping("/register")
    public ResponseEntity<UserSaveResponse> register(@RequestBody UserRegistrationDto request) {
        registrationService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserSaveResponse(true, "User created"));
    }
}
