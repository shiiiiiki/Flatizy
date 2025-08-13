package org.flatizy.flatizy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public void save(String path) {
        importUsersFromJson(path);
    }

    private void importUsersFromJson(String jsonPath) {
        File jsonFile = new File(jsonPath);
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonFile);
            for (JsonNode node : root) {
                User user = mapJsonNodeToUser(node);
                saveUser(user);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to import users from JSON: " + e.getMessage(), e);
        }
    }

    private User mapJsonNodeToUser(JsonNode node) {
        Integer telegramId = node.hasNonNull("id") ? node.get("id").asInt() : null;
        User user = userRepository.findByTelegramId(telegramId).orElseGet(User::new);

        if (node.hasNonNull("first_name")) user.setFirstName(node.get("first_name").asText());
        if (node.hasNonNull("last_name")) user.setLastName(node.get("last_name").asText());
        if (node.hasNonNull("username")) user.setUserName(node.get("username").asText());
        if (node.hasNonNull("phone")) user.setPhone(node.get("phone").asText());

        user.setTelegramId(telegramId);
        return user;
    }

    private void saveUser(User user) {
        userRepository.save(user);
    }
}
