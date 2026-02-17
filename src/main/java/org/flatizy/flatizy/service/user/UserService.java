package org.flatizy.flatizy.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.dto.user.UserDto;
import org.flatizy.flatizy.entity.mapper.UserMapper;
import org.flatizy.flatizy.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserDto> getAll() {
        return userRepository.findAll().stream().
                map(userMapper::fromEntityToUserDto).
                toList();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findByPhone(String phoneNumber) {
        return userRepository.findByPhone(phoneNumber);
    }

    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }
}
