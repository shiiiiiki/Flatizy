package org.flatizy.flatizy.event.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.enums.AccountStatus;
import org.flatizy.flatizy.service.BillingService;
import org.flatizy.flatizy.service.account.AccountService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventListener {

    private final AccountService accountService;
    private final BillingService billingService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountCreated(AccountCreatedEvent event) {
        for (Integer accountId : event.accountId()) {
            Account account = accountService.findById(accountId).orElseThrow();
            int attempts = 0;
            boolean success = false;
            while (attempts < 3 && !success) {
                try {
                    attempts++;
                    log.info("Sending account {} attempt {}", accountId, attempts);
                    var response = billingService.sendAccount(account);
                    if ("COMPLETED".equals(response.getStatus())) {
                        account.setStatus(AccountStatus.COMPLETED);
                        account.setSentAt(LocalDateTime.now());
                        success = true;
                        log.info("Account {} completed", accountId);
                    } else {
                        throw new RuntimeException("Billing failed");
                    }
                } catch (Exception e) {
                    log.error("Attempt {} failed for account {}", attempts, accountId);
                    if (attempts == 3) {
                        account.setStatus(AccountStatus.FAILED);
                        log.error("Account {} marked as FAILED", accountId);
                    }
                    try {
                        Thread.sleep(1000); // пауза
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
