package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.Request;
import org.flatizy.flatizy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Integer> {

    List<Request> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT r FROM Request r WHERE r.user = :user AND r.status IN ('PENDING', 'IN_PROGRESS') ORDER BY r.createdAt DESC")
    List<Request> findActiveByUser(User user);

    List<Request> findAllByOrderByCreatedAtDesc();

    @Query("SELECT r.status, COUNT(r) FROM Request r GROUP BY r.status")
    List<Object[]> countByStatus();

    @Query("SELECT COUNT(r) FROM Request r WHERE r.status IN ('PENDING', 'IN_PROGRESS')")
    long countActive();
}
