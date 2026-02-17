package org.flatizy.flatizy.controller;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.dto.account.AccountStatusUpdateDto;
import org.flatizy.flatizy.entity.dto.account.DebtStatusDto;
import org.flatizy.flatizy.service.account.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/accounts")
@Slf4j
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/mock")
    public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload) {
        log.info("Billing received payload: {}", payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update/status")
    public ResponseEntity<AccountStatusUpdateDto> updateStatus(@RequestBody AccountStatusUpdateDto dto){
        log.info("Receive updated status: {}", dto);
        return ResponseEntity.ok(accountService.updateStatus(dto));
    }

    @GetMapping("/debt/{accountNumber}")
    public ResponseEntity<DebtStatusDto> getDebt(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getDebt(accountNumber));
    }

    @PostMapping("/update/debt")
    public ResponseEntity<String> updateDebt(@RequestBody DebtStatusDto dto) {
        return ResponseEntity.ok( accountService.updateDebt(dto));
    }
}
