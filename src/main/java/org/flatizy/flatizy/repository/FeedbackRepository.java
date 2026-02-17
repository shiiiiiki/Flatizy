package org.flatizy.flatizy.repository;

import org.flatizy.flatizy.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
