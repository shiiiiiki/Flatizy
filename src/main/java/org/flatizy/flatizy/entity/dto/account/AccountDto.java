package org.flatizy.flatizy.entity.dto.account;

import lombok.Builder;
import lombok.Data;
import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.enums.AccountStatus;

@Builder
@Data
public class AccountDto {
    private String accountNumber;
    private boolean hasDebt;
    private AccountStatus status;
    private Apartment apartment;
    private User user;
}
