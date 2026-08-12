package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.InviteLink;
import org.flatizy.flatizy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InviteLinkRepository extends JpaRepository<InviteLink, Integer> {

    Optional<InviteLink> findByCode(String code);

    List<InviteLink> findByCreatorAndActiveTrue(User creator);

    List<InviteLink> findByCreatorOrderByCreatedAtDesc(User creator);

    List<InviteLink> findByActiveTrueAndExpiresAtBefore(LocalDateTime dateTime);

    boolean existsByCodeAndActiveTrue(String code);

    @Query("SELECT COUNT(il) FROM InviteLink il WHERE il.creator = :creator AND il.active = true")
    long countActiveByCreator(User creator);

    @Query("SELECT SUM(il.usedCount) FROM InviteLink il WHERE il.creator = :creator")
    Long sumUsedCountByCreator(User creator);
}
