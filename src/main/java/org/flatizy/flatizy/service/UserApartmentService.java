package org.flatizy.flatizy.service;

import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.UserApartment;
import org.flatizy.flatizy.repository.UserApartmentRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserApartmentService {

    private final UserApartmentRepository userApartmentRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;

    private static final Pattern TG_PATTERN = Pattern.compile("(500\\d{3}|\\d{3})");
    private static final List<String> FORBIDDEN_WORDS = Arrays.asList("корпус", "кв", "парковка");

    public UserApartmentService(UserApartmentRepository userApartmentRepository, ApartmentService apartmentService, UserService userService) {
        this.userApartmentRepository = userApartmentRepository;
        this.apartmentService = apartmentService;
        this.userService = userService;
    }

    public ManualMappingLists save() {
        List<User> users = userService.getAll();
        List<Apartment> apartments = apartmentService.getAll();

        UserMappingResult result = mapUsers(users);

        Map<String, User> mappedUsers = result.mappedUsers();

        List<User> manualUsers = result.manualList();
        Map<String, Apartment> mappedApartments = mapApartments(apartments);
        List<Apartment> manualApartments = apartments.stream()
                .filter(a -> !mappedUsers.containsKey(String.format("%d00%03d", a.getBuildingNumber(), a.getApartmentNumber())))
                .toList();

        mappedUsers.forEach((tgName, user) -> {
            Apartment apartment = mappedApartments.get(tgName);
            if (apartment != null) {
                UserApartment ua = new UserApartment();
                ua.setUser(user);
                ua.setApartment(apartment);
                ua.setTelegramName(tgName);
                userApartmentRepository.save(ua);
                System.out.println("Saved userApartment with user id: " + user.getId() + " apID : " + apartment.getId() + " tgName: " + tgName);
            }
        });
        return new ManualMappingLists(manualUsers, manualApartments);
    }

    public Map<String, Apartment> mapApartments(List<Apartment> apartments) {
        return apartments.stream()
                .collect(Collectors.toMap(
                        apartment -> String.format("%d00%03d",
                                apartment.getBuildingNumber(),
                                apartment.getApartmentNumber()
                        ),
                        apartment -> apartment
                ));
    }

    private UserMappingResult mapUsers(List<User> users) {
        Map<Boolean, List<User>> partitioned = users.stream()
                .collect(Collectors.partitioningBy(this::isTgNameValid));

        Map<String, User> mappedUsers = partitioned.get(true).stream()
                .collect(Collectors.toMap(this::extractTgName, user -> user));

        List<User> manualList = partitioned.get(false);

        return new UserMappingResult(mappedUsers, manualList);
    }

    private boolean isTgNameValid(User user) {
        String firstName = user.getFirstName().toLowerCase();
        Matcher matcher = TG_PATTERN.matcher(firstName);
        if (matcher.find()) {
            String tgName = matcher.group();
            if (tgName.startsWith("500")) return true;
            else if (tgName.length() == 3) {
                boolean hasForbidden = FORBIDDEN_WORDS.stream().anyMatch(firstName::contains);
                return !hasForbidden;
            }
        }
        return false;
    }

    private String extractTgName(User user) {
        Matcher matcher = TG_PATTERN.matcher(user.getFirstName().toLowerCase());
        matcher.find();
        String tgName = matcher.group();
        if (tgName.length() == 3 && !tgName.startsWith("500")) {
            return "600" + tgName;
        }
        return tgName;
    }

    private record UserMappingResult(Map<String, User> mappedUsers, List<User> manualList) {
    }

    public record ManualMappingLists(List<User> users, List<Apartment> apartments) {
    }
}
