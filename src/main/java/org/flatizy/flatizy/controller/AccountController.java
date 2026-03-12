package org.flatizy.flatizy.controller;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.dto.account.AccountStatusUpdateDto;
import org.flatizy.flatizy.entity.dto.account.DebtStatusDto;
import org.flatizy.flatizy.handler.TelegramUpdateHandler;
import org.flatizy.flatizy.service.account.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
}
