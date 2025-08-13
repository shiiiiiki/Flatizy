package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByTelegramId(Integer telegramId);
}
