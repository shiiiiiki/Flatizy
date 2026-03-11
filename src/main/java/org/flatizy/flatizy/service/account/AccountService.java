package org.flatizy.flatizy.service.account;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.dto.account.AccountStatusUpdateDto;
import org.flatizy.flatizy.entity.dto.account.DebtStatusDto;
import org.flatizy.flatizy.entity.dto.account.DebtorDto;
import org.flatizy.flatizy.entity.enums.AccountStatus;
import org.flatizy.flatizy.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final RestTemplate restTemplate;

    @Value("${billing.debt.api.url}")
    private String debtApiUrl;

    public AccountService(AccountRepository accountRepository, RestTemplate restTemplate) {
        this.accountRepository = accountRepository;
        this.restTemplate = restTemplate;
    }

    public List<Account> saveAll(List<Account> accounts) {
        return accountRepository.saveAll(accounts);
    }

    @Transactional
    public Optional<Account> findById(Integer id) {
        return accountRepository.findById(id);
    }


    public void save(Account account) {
        accountRepository.save(account);
    }

    @Transactional
    public AccountStatusUpdateDto updateStatus(AccountStatusUpdateDto dto) {

        Account account = accountRepository
                .findByAccountNumber(dto.getAccountNumber())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account not found: " + dto.getAccountNumber()
                        ));

        validateStatusTransition(account.getStatus(), dto.getStatus());

        account.setStatus(dto.getStatus());

        return new AccountStatusUpdateDto(account.getAccountNumber(), account.getStatus());
    }

    @Transactional
    public void updateStatusOnFailure(Account account, AccountStatus status) {
        account.setCreatedAt(LocalDateTime.now());
        account.setStatus(status);
    }

    @Transactional(readOnly = true)
    public DebtStatusDto getDebt(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + accountNumber));

        return new DebtStatusDto(
                account.getAccountNumber(),
                account.isHasDebt(),
                account.getDebtAmount(),
                account.getDebtStartDate() != null ? account.getDebtStartDate().atStartOfDay() : null
        );
    }

    @Transactional
    public String updateDebt(DebtStatusDto dto) {
        Account account = accountRepository.findByAccountNumber(dto.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + dto.getAccountNumber()));

        account.setHasDebt(dto.isHasDebt());
        account.setDebtAmount(dto.getDebtAmount());
        account.setDebtStartDate(dto.getDebtStartDate().toLocalDate());
        account.setDebtUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
        return String.format("Debt on account %s was updated",
                account.getAccountNumber());
    }

    private void validateStatusTransition(AccountStatus current, AccountStatus next) {
        if (current == AccountStatus.COMPLETED && current != next) {
            throw new IllegalStateException("Invalid status transition");
        }
    }

    public Map<String, String> validateAccountRequest(Map<String, Object> payload) {
        String accountNumber = (String) payload.get("accountNumber");

        boolean success = Math.random() > 0.2;
        String status = success ? "COMPLETED" : "FAILED";

        log.info("Billing processed account {} with status {}", accountNumber, status);
        return Map.of(
                "accountNumber", accountNumber,
                "status", status
        );
    }

    @Transactional(readOnly = true)
    public List<DebtorDto> getAllDebtors() {
        return accountRepository.findAll().stream()
                .filter(Account::isHasDebt)
                .filter(account ->
                        account.getDebtAmount() != null
                                && account.getDebtAmount().compareTo(BigDecimal.ZERO) > 0
                )
                .map(this::mapAccountToDebtorDto)
                .collect(Collectors.toList());
    }

    private DebtorDto mapAccountToDebtorDto(Account account) {
        return new DebtorDto(
                account.getUser().getFirstName(),
                account.getUser().getLastName(),
                account.getUser().getPhone(),
                String.valueOf(account.getApartment().getApartmentNumber()),
                String.valueOf(account.getApartment().getHouseNumber()),
                account.getDebtAmount(),
                account.getAccountNumber()
        );
    }

    @Transactional
    public void updateDebtsFromExternalService() {
        try {
            DebtStatusDto[] debtArray = restTemplate.getForObject(debtApiUrl, DebtStatusDto[].class);
            if (debtArray != null) {
                Arrays.stream(debtArray).forEach(this::updateDebt);
                log.info("Updated debts for {} accounts", debtArray.length);
            }
        } catch (Exception e) {
            log.error("Failed to update debts from external service", e);
        }
    }
}
