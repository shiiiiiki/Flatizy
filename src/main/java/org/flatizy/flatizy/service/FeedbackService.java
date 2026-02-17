package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import org.flatizy.flatizy.entity.Feedback;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public void save(User user, String message) {
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setMessage(message);

        feedbackRepository.save(feedback);
    }
}
