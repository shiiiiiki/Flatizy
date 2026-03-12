package org.flatizy.flatizy.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.dto.account.DebtorDto;
import org.flatizy.flatizy.handler.TelegramUpdateHandler;
import org.flatizy.flatizy.service.account.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/admin/accounts")
@Slf4j
@AllArgsConstructor
public class AccountAdminController {

    private final AccountService accountService;
    private final TelegramUpdateHandler telegramUpdateHandler;

    @GetMapping("/debtors")
    public ResponseEntity<List<DebtorDto>> getAllDebtors() {
        log.info("Fetching all debtors");
        List<DebtorDto> debtors = accountService.getAllDebtors();
        return ResponseEntity.ok(debtors);
    }

    @PostMapping("/notify-debtors")
    public ResponseEntity<Map<String, Object>> notifyDebtors() {
        log.info("Sending debt notification messages to debtors");
        List<DebtorDto> debtors = accountService.getAllDebtors();
        int successCount = 0;
        int failureCount = 0;

        for (DebtorDto debtor : debtors) {
            try {
                String message = String.format(
                        "💳 *Сповіщення про борг*\n\n" +
                                "Шановний(а) %s %s,\n\n" +
                                "У вас є заборгованість по квартирі №%s (дім №%s):\n" +
                                "💰 *Сума боргу: %s грн*\n\n" +
                                "Будь ласка, проведіть оплату найближчим часом.",
                        debtor.getFirstName(),
                        debtor.getLastName(),
                        debtor.getApartmentNumber(),
                        debtor.getHouseNumber(),
                        debtor.getDebtAmount()
                );
                telegramUpdateHandler.sendMessageToUser(debtor.getPhone(), message);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send debt notification to {} {}", debtor.getFirstName(), debtor.getLastName(), e);
                failureCount++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "total", debtors.size(),
                "success", successCount,
                "failures", failureCount
        ));
    }

    @PostMapping("/update-debts-external")
    public ResponseEntity<List<DebtorDto>> updateDebtsFromExternal() {
        log.info("Updating debts from external service");
        accountService.updateDebtsFromExternalService();
        return ResponseEntity.ok(accountService.getAllDebtors());
    }
}
