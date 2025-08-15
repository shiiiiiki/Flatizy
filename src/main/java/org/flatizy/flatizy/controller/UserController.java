package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/users")
@RestController
public class UserController {

    private final UserService userService;
    private final String CONTACTS_PATH = "src/main/resources/users.json";

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("get")
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @PostMapping("save")
    public ResponseEntity<Void> saveApartments() {
        userService.save(CONTACTS_PATH);
        return ResponseEntity.ok().build();
    }
}
