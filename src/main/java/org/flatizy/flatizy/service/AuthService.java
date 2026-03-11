package org.flatizy.flatizy.service;

import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.dto.user.UserDto;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.flatizy.flatizy.entity.mapper.UserMapperImpl;
import org.flatizy.flatizy.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapperImpl userMapperImpl;

    public AuthService(UserRepository userRepository, UserMapperImpl userMapperImpl) {
        this.userRepository = userRepository;
        this.userMapperImpl = userMapperImpl;
    }

    public UserDto login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(password))
            throw new RuntimeException("Wrong password");

        if (user.getRole() != UserRole.ADMIN)
            throw new RuntimeException("No CRM access");

        return userMapperImpl.fromEntityToDto(user);
    }
}