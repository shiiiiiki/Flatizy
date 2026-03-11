package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.dto.BillingResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final RestTemplate restTemplate;

    @Value("${billing.api.url}")
    private String billingUrl;

    public BillingResponseDto sendAccount(Account account) {
        Map<String, Object> payload = Map.of(
                "accountNumber", account.getAccountNumber(),
                "userId", account.getUser().getId(),
                "apartmentId", account.getApartment().getId()
        );

        BillingResponseDto response = restTemplate.postForObject(
                billingUrl,
                payload,
                BillingResponseDto.class
        );

        log.info("Billing response for {}: {}", account.getAccountNumber(), response.getStatus());

        return response;
    }
}
