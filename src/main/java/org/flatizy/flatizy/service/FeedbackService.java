package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.dto.FeedbackDto;
import org.flatizy.flatizy.entity.Feedback;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserApartmentService userApartmentService;

    @Transactional
    public void save(User user, String message) {
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setMessage(message);
        feedbackRepository.save(feedback);
        log.info("Feedback created: userId={}", user.getId());
    }

    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }

    public FeedbackDto convertToDto(Feedback feedback) {
        FeedbackDto dto = new FeedbackDto();
        dto.setId(feedback.getId());
        dto.setMessage(feedback.getMessage());
        dto.setCreatedAt(feedback.getCreatedAt());

        if (feedback.getUser() != null) {
            User user = feedback.getUser();
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());

            List<org.flatizy.flatizy.entity.Apartment> apartments = userApartmentService.getApartmentsByUser(user);
            if (!apartments.isEmpty()) {
                var apartment = apartments.get(0);
                dto.setApartmentNumber(apartment.getApartmentNumber());
                dto.setHouseNumber(apartment.getHouseNumber());
            }
        }

        return dto;
    }

    public List<FeedbackDto> getAllFeedbacksDto() {
        return getAllFeedbacks().stream()
                .map(this::convertToDto)
                .toList();
    }
}
