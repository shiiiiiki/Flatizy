package org.flatizy.flatizy.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.dto.RequestDto;
import org.flatizy.flatizy.dto.UpdateRequestStatusDto;
import org.flatizy.flatizy.entity.enums.RequestStatus;
import org.flatizy.flatizy.service.RequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private final RequestService requestService;

    /**
     * Получить все заявки в формате (айди, тип, описание, когда создан, номер квартиры и дома)
     */
    @GetMapping
    public ResponseEntity<List<RequestDto>> getAllRequests() {
        log.info("Получение всех заявок");
        List<RequestDto> requests = requestService.getAllRequestsDto();
        return ResponseEntity.ok(requests);
    }

    /**
     * Обновить статус заявки с обязательным feedback при закрытии (COMPLETED/REJECTED)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Object> updateRequestStatus(
            @PathVariable Integer id,
            @RequestBody UpdateRequestStatusDto dto) {
        log.info("Обновление статуса заявки: id={}, статус={}", id, dto.getStatus());

        try {
            RequestStatus newStatus = RequestStatus.valueOf(dto.getStatus().toUpperCase());

            // При закрытии заявки feedback обязателен
            if ((newStatus == RequestStatus.COMPLETED || newStatus == RequestStatus.REJECTED) &&
                (dto.getFeedback() == null || dto.getFeedback().isBlank())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Feedback обязателен при закрытии заявки"
                ));
            }

            return requestService.updateRequestStatus(id, newStatus, dto.getFeedback())
                    .map(request -> {
                        log.info("Заявка успешно обновлена: id={}", id);
                        return ResponseEntity.ok((Object) requestService.convertToDto(request));
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            log.error("Неверный статус: {}", dto.getStatus());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Неверный статус заявки"
            ));
        }
    }
}
