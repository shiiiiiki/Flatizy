package org.flatizy.flatizy.service.account;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.dto.account.AccountStatusUpdateDto;
import org.flatizy.flatizy.entity.dto.account.DebtStatusDto;
import org.flatizy.flatizy.entity.enums.AccountStatus;
import org.flatizy.flatizy.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository, RestTemplate restTemplate) {
        this.accountRepository = accountRepository;
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
                (account.getDebtStartDate())
        );
    }

    @Transactional
    public String updateDebt(DebtStatusDto dto) {
        Account account = accountRepository.findByAccountNumber(dto.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + dto.getAccountNumber()));

        account.setHasDebt(dto.isHasDebt());
        account.setDebtStartDate(dto.getDebtStartDate());
        account.setDebtUpdatedAt(LocalDateTime.now());
        return String.format("Debt on account %s was updated",
                account.getAccountNumber());
    }

    private void validateStatusTransition(AccountStatus current, AccountStatus next) {
        if (current == AccountStatus.COMPLETED && current != next) {
            throw new IllegalStateException("Invalid status transition");
        }
    }
}
