package org.flatizy.flatizy.entity.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DebtStatusDto {
    @JsonProperty("accountNumber")
    private String accountNumber;
    @JsonProperty("hasDebt")
    private boolean hasDebt;
    @JsonProperty("debtAmount")
    private BigDecimal debtAmount;
    @JsonProperty("debtStartDate")
    private LocalDateTime debtStartDate;
}

