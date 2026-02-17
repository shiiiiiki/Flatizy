package org.flatizy.flatizy.entity.dto.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class DebtStatusDto {
    private String accountNumber;
    private boolean hasDebt;
    private LocalDate debtStartDate;
}

