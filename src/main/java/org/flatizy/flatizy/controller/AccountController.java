package org.flatizy.flatizy.controller;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.dto.account.AccountStatusUpdateDto;
import org.flatizy.flatizy.entity.dto.account.DebtStatusDto;
import org.flatizy.flatizy.entity.dto.account.DebtorDto;
import org.flatizy.flatizy.handler.TelegramUpdateHandler;
import org.flatizy.flatizy.service.account.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/accounts")
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final TelegramUpdateHandler telegramUpdateHandler;

    public AccountController(AccountService accountService, TelegramUpdateHandler telegramUpdateHandler) {
        this.accountService = accountService;
        this.telegramUpdateHandler = telegramUpdateHandler;
    }

    @PostMapping("/mock")
    public ResponseEntity<Map<String, String>> receive(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(accountService.validateAccountRequest(payload));
    }

    //todo maybe remove because not necessary
    @PostMapping("/update/status")
    public ResponseEntity<AccountStatusUpdateDto> updateStatus(@RequestBody AccountStatusUpdateDto dto){
        log.info("Receive updated status: {}", dto);
        return ResponseEntity.ok(accountService.updateStatus(dto));
    }

    //todo maybe remove because not necessary
    @GetMapping("/debt/{accountNumber}")
    public ResponseEntity<DebtStatusDto> getDebt(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getDebt(accountNumber));
    }

    //todo реализовать обновление списка должников из внешнего сервиса и добавить сумму долга, просто для информативности
    @PostMapping("/update/debt")
    public ResponseEntity<String> updateDebt(@RequestBody DebtStatusDto dto) {
        return ResponseEntity.ok(accountService.updateDebt(dto));
    }

    @GetMapping("/debtors")
    public ResponseEntity<List<DebtorDto>> getAllDebtors() {
        log.info("Fetching all debtors");
        List<DebtorDto> debtors = accountService.getAllDebtors();
        return ResponseEntity.ok(debtors);
    }

    @PostMapping("/update-debts-external")
    public ResponseEntity<List<DebtorDto>> updateDebtsFromExternal() {
        log.info("Updating debts from external service");
        accountService.updateDebtsFromExternalService();
        return ResponseEntity.ok(accountService.getAllDebtors());
    }

    @GetMapping("/debts-mock")
    public ResponseEntity<DebtStatusDto[]> getDebtsMock() {
        log.info("Returning mock debt data");
        DebtStatusDto[] debts = new DebtStatusDto[]{
                new DebtStatusDto("ACC-12-6", true, new BigDecimal("2667.4"), java.time.LocalDateTime.now()),
                new DebtStatusDto("ACC-19-7", true, new BigDecimal("1500.0"), java.time.LocalDateTime.now().minusDays(10)),
                new DebtStatusDto("ACC-16-6", true, new BigDecimal("1877.0"), java.time.LocalDateTime.now())
        };
        return ResponseEntity.ok(debts);
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
}
