package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.Request;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Integer> {

    /**
     * Найти все заявки пользователя
     */
    List<Request> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Найти активные заявки пользователя (PENDING и IN_PROGRESS)
     */
    @Query("SELECT r FROM Request r WHERE r.user = :user AND r.status IN ('PENDING', 'IN_PROGRESS') ORDER BY r.createdAt DESC")
    List<Request> findActiveByUser(User user);

    /**
     * Найти все заявки по статусу
     */
    List<Request> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /**
     * Найти все заявки (для админа)
     */
    List<Request> findAllByOrderByCreatedAtDesc();

    /**
     * Найти заявки за период
     */
    List<Request> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Статистика: количество заявок по статусам
     */
    @Query("SELECT r.status, COUNT(r) FROM Request r GROUP BY r.status")
    List<Object[]> countByStatus();

    /**
     * Количество активных заявок
     */
    @Query("SELECT COUNT(r) FROM Request r WHERE r.status IN ('PENDING', 'IN_PROGRESS')")
    long countActive();
}
