package org.flatizy.flatizy.service;

import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.repository.AccountRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    protected void saveAccount(Integer accountNumber, Apartment apartment) {
        try {
            Account account = new Account();
            account.setAccountNumber(String.valueOf(accountNumber));
            account.setApartment(apartment);
            accountRepository.save(account);
            System.out.println("Account saved");
        } catch (DataAccessException e) {
            throw new RuntimeException("Exception during saving acc: " + e);
        }
    }
}
