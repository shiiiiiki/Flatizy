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
            try {
                Account account = accountService.findById(accountId)
                        .orElseThrow();

                account.setSentAt(LocalDateTime.now());

                billingService.sendAccount(account);
                account.setStatus(AccountStatus.SENT);

            } catch (Exception e) {
                log.error("Billing failed for account {}", accountId, e);
                accountService.findById(accountId).ifPresent(acc -> accountService.updateStatusOnFailure(acc,AccountStatus.FAILED));
            }
        }
    }
}
