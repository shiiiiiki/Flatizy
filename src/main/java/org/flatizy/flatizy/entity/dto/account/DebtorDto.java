package org.flatizy.flatizy.entity.dto.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class DebtorDto {
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("lastName")
    private String lastName;
    @JsonProperty("phone")
    private String phone;
    @JsonProperty("apartmentNumber")
    private String apartmentNumber;
    @JsonProperty("houseNumber")
    private String houseNumber;
    @JsonProperty("debtAmount")
    private BigDecimal debtAmount;
    @JsonProperty("accountNumber")
    private String accountNumber;
}
