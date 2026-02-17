package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.Request;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.enums.RequestStatus;
import org.flatizy.flatizy.entity.enums.RequestType;
import org.flatizy.flatizy.repository.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
