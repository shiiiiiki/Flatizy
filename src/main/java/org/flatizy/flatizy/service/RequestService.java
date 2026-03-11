package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.dto.RequestDto;
import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.Request;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.enums.RequestStatus;
import org.flatizy.flatizy.entity.enums.RequestType;
import org.flatizy.flatizy.repository.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final RequestRepository requestRepository;

    @Transactional
    public Request createRequest(User user, RequestType type, String description, Apartment apartment) {
        Request request = new Request();
        request.setUser(user);
        request.setType(type);
        request.setDescription(description);
        request.setApartment(apartment);
        request.setStatus(RequestStatus.PENDING);

        Request saved = requestRepository.save(request);

        log.info("Создана новая заявка: id={}, user={}, type={}",
                saved.getId(), user.getId(), type);

        return saved;
    }

    public List<Request> getUserRequests(User user) {
        return requestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Request> getAllRequests() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Optional<Request> updateRequestStatus(Integer requestId, RequestStatus newStatus, String feedback) {
        return requestRepository.findById(requestId).map(request -> {
            request.setStatus(newStatus);
            if (feedback != null) {
                request.setFeedback(feedback);
            }
            if (newStatus == RequestStatus.COMPLETED || newStatus == RequestStatus.REJECTED) {
                request.setCompletedAt(LocalDateTime.now());
            }
            Request updated = requestRepository.save(request);
            log.info("Статус заявки обновлен: id={}, статус={}, обратная связь={}",
                    requestId, newStatus, feedback != null ? "да" : "нет");
            return updated;
        });
    }

    public Optional<Request> findById(Integer id) {
        return requestRepository.findById(id);
    }

    public RequestDto convertToDto(Request request) {
        RequestDto dto = new RequestDto();
        dto.setId(request.getId());
        dto.setType(request.getType().getDisplayName());
        dto.setDescription(request.getDescription());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setStatus(request.getStatus().getName());
        dto.setFeedback(request.getFeedback());

        if (request.getApartment() != null) {
            dto.setApartmentNumber(request.getApartment().getApartmentNumber());
            dto.setHouseNumber(request.getApartment().getHouseNumber());
        }

        return dto;
    }

    public List<RequestDto> getAllRequestsDto() {
        return getAllRequests().stream()
                .map(this::convertToDto)
                .toList();
    }
}
