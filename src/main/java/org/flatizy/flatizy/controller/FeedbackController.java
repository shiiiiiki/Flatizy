package org.flatizy.flatizy.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.dto.FeedbackDto;
import org.flatizy.flatizy.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/feedbacks")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Получить все feedbacks в формате (айди, имя, фамилия, номер квартиры и дома, текст, дата создания)
     */
    @GetMapping
    public ResponseEntity<List<FeedbackDto>> getAllFeedbacks() {
        log.info("Getting all feedbacks");
        List<FeedbackDto> feedbacks = feedbackService.getAllFeedbacksDto();
        return ResponseEntity.ok(feedbacks);
    }
}
