package org.flatizy.flatizy.entity.dto.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.flatizy.flatizy.entity.enums.AccountStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusUpdateDto {
    private String accountNumber;
    private AccountStatus status;
}
