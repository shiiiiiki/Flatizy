package org.flatizy.flatizy.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.UserApartment;
import org.flatizy.flatizy.entity.dto.user.UserRegistrationDto;
import org.flatizy.flatizy.entity.enums.AccountStatus;
import org.flatizy.flatizy.entity.mapper.UserMapper;
import org.flatizy.flatizy.event.account.AccountCreatedEvent;
import org.flatizy.flatizy.service.UserApartmentService;
import org.flatizy.flatizy.service.account.AccountService;
import org.flatizy.flatizy.service.apartment.ApartmentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final ApartmentService apartmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserApartmentService userApartmentService;
    private final AccountService accountService;

    @Transactional
    public void registerUser(UserRegistrationDto userRegistrationDto) {

        List<Apartment> apartments = validateApartments(userRegistrationDto);

        User user;

        try {
            user = userService.save(
                    userMapper.fromUserRegistrationDtoToEntity(userRegistrationDto));
        } catch (DataIntegrityViolationException ex) {
            throw new DataIntegrityViolationException("User already exists");
        }

        List<UserApartment> linkedUserApartments = apartments.stream().
                map(apartment ->
                {
                    UserApartment userApartment = new UserApartment();
                    userApartment.setApartment(apartment);
                    userApartment.setUser(user);
                    return userApartment;
                }).toList();

        userApartmentService.saveAll(linkedUserApartments);

        List<Account> accountsToSave = apartments.stream().
                map(apartment -> {
                    Account account = new Account();
                    account.setUser(user);
                    account.setApartment(apartment);
                    account.setStatus(AccountStatus.PENDING);
                    account.setAccountNumber(generateAccountNumber(user, apartment));
                                        return account;
                }).toList();

        List<Account> savedAccount = accountService.saveAll(accountsToSave);


        List<Integer> accountIds = savedAccount.stream()
                .map(account -> Math.toIntExact(account.getId()))
                .toList();

        eventPublisher.publishEvent(
                new AccountCreatedEvent(accountIds));

        log.info("User {} registered with account {}", user.getId(), accountIds);

    }

    private String generateAccountNumber(User user, Apartment apartment) {
        return "ACC-" + user.getId() + "-" + apartment.getId();
    }

    private List<Apartment> validateApartments(UserRegistrationDto userRegistrationDto) {
        List<Integer> apartmentIds = userRegistrationDto.getApartmentIds();

        if (apartmentIds == null || apartmentIds.isEmpty()) {
            throw new IllegalArgumentException("Apartment is required");
        }

        List<Apartment> apartments = apartmentService.findAllById(apartmentIds);
        if (apartments.size() != apartmentIds.size()) {
            throw new IllegalArgumentException("Some apartments were not found");
        }
        return apartments;
    }
}
