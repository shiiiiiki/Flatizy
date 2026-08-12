package org.flatizy.flatizy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Apartment;
import org.flatizy.flatizy.entity.InviteLink;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.flatizy.flatizy.repository.ApartmentRepository;
import org.flatizy.flatizy.repository.InviteLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InviteLinkService {

    private final InviteLinkRepository inviteLinkRepository;
    private final ApartmentRepository apartmentRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    @Value("${telegram.bot.username}")
    private String BOT_USERNAME;

    @Transactional
    public InviteLink createInviteLink(User creator, UserRole targetRole, Integer maxUses,
                                       Integer expirationDays, List<Integer> apartmentIds) {

        validateCreatorPermissions(creator, targetRole);

        if (maxUses < 1 || maxUses > 5) {
            throw new IllegalArgumentException("Количество использований должно быть от 1 до 5");
        }

        if (expirationDays < 1 || expirationDays > 30) {
            throw new IllegalArgumentException("Срок действия должен быть от 1 до 30 дней");
        }

        if (apartmentIds == null || apartmentIds.isEmpty()) {
            throw new IllegalArgumentException("Необходимо выбрать хотя бы одну квартиру");
        }

        Set<Apartment> apartments = new HashSet<>(apartmentRepository.findAllById(apartmentIds));

        if (apartments.size() != apartmentIds.size()) {
            throw new IllegalArgumentException("Некоторые квартиры не найдены в системе");
        }

        String code = generateUniqueCode();

        InviteLink inviteLink = new InviteLink();
        inviteLink.setCode(code);
        inviteLink.setTargetRole(targetRole);
        inviteLink.setCreator(creator);
        inviteLink.setMaxUses(maxUses);
        inviteLink.setUsedCount(0);
        inviteLink.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        inviteLink.setActive(true);
        inviteLink.setApartments(apartments);

        InviteLink saved = inviteLinkRepository.save(inviteLink);

        log.info("Создана invite ссылка: код={}, creator={}, targetRole={}, maxUses={}, apartmentCount={}",
                code, creator.getId(), targetRole, maxUses, apartments.size());

        return saved;
    }

    private void validateCreatorPermissions(User creator, UserRole targetRole) {
        UserRole creatorRole = creator.getRole();

        if (creatorRole == UserRole.TENANT) {
            throw new IllegalArgumentException("Арендаторы не могут создавать invite ссылки");
        }

        if (creatorRole == UserRole.ADMIN && targetRole != UserRole.OWNER) {
            throw new IllegalArgumentException("Админ может создавать ссылки только для владельцев");
        }

        if (creatorRole == UserRole.OWNER && targetRole != UserRole.TENANT) {
            throw new IllegalArgumentException("Владелец может создавать ссылки только для арендаторов");
        }
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            byte[] randomBytes = new byte[24];
            secureRandom.nextBytes(randomBytes);
            code = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(randomBytes)
                    .substring(0, 16);
            attempts++;

            if (attempts > 10) {
                throw new RuntimeException("Не удалось сгенерировать уникальный код");
            }
        } while (inviteLinkRepository.existsByCodeAndActiveTrue(code));

        return code;
    }

    public String getTelegramLink(String code) {
        return String.format("https://t.me/%s?start=%s", BOT_USERNAME, code);
    }

    public Optional<InviteLink> findByCode(String code) {
        return inviteLinkRepository.findByCode(code);
    }

    @Transactional
    public ValidationResult validateAndUse(String code) {
        Optional<InviteLink> linkOpt = inviteLinkRepository.findByCode(code);

        if (linkOpt.isEmpty()) {
            log.warn("Попытка использовать несуществующую ссылку: {}", code);
            return ValidationResult.notFound();
        }

        InviteLink link = linkOpt.get();

        if (!link.isActive()) {
            log.warn("Попытка использовать неактивную ссылку: {}", code);
            return ValidationResult.inactive();
        }

        if (link.isExpired()) {
            link.setActive(false);
            inviteLinkRepository.save(link);
            log.info("Ссылка истекла и деактивирована: {}", code);
            return ValidationResult.expired();
        }

        if (link.getUsedCount() >= link.getMaxUses()) {
            link.setActive(false);
            inviteLinkRepository.save(link);
            log.info("Превышен лимит использований ссылки: {}", code);
            return ValidationResult.limitReached();
        }

        link.incrementUsage();
        inviteLinkRepository.save(link);

        log.info("Ссылка успешно использована: код={}, осталось={}/{}",
                code, link.getRemainingUses(), link.getMaxUses());

        return ValidationResult.success(link);
    }

    public List<InviteLink> getActiveLinks(User creator) {
        return inviteLinkRepository.findByCreatorAndActiveTrue(creator);
    }

    public List<InviteLink> getAllLinks(User creator) {
        return inviteLinkRepository.findByCreatorOrderByCreatedAtDesc(creator);
    }

    @Transactional
    public void deactivateLink(Integer linkId, User user) {
        InviteLink link = inviteLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Ссылка не найдена"));

        if (!link.getCreator().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Нет прав на деактивацию этой ссылки");
        }

        link.setActive(false);
        inviteLinkRepository.save(link);

        log.info("Ссылка деактивирована вручную: код={}, user={}", link.getCode(), user.getId());
    }

    public LinkStatistics getStatistics(User creator) {
        long activeCount = inviteLinkRepository.countActiveByCreator(creator);
        Long totalUsed = inviteLinkRepository.sumUsedCountByCreator(creator);

        return new LinkStatistics(activeCount, totalUsed != null ? totalUsed : 0);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deactivateExpiredLinks() {
        List<InviteLink> expiredLinks = inviteLinkRepository
                .findByActiveTrueAndExpiresAtBefore(LocalDateTime.now());

        if (!expiredLinks.isEmpty()) {
            expiredLinks.forEach(link -> link.setActive(false));
            inviteLinkRepository.saveAll(expiredLinks);
            log.info("Автоматически деактивировано {} истекших invite ссылок", expiredLinks.size());
        }
    }

    public Optional<InviteLink> getById(Integer inviteId) {
        return Optional.of(inviteLinkRepository.getReferenceById(inviteId));
    }

    public void delete(InviteLink link) {
        inviteLinkRepository.delete(link);
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final InviteLink inviteLink;

        private ValidationResult(boolean valid, String errorMessage, InviteLink inviteLink) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.inviteLink = inviteLink;
        }

        public static ValidationResult success(InviteLink link) {
            return new ValidationResult(true, null, link);
        }

        public static ValidationResult notFound() {
            return new ValidationResult(false, "Ссылка не найдена", null);
        }

        public static ValidationResult inactive() {
            return new ValidationResult(false, "Ссылка деактивирована", null);
        }

        public static ValidationResult expired() {
            return new ValidationResult(false, "Срок действия ссылки истек", null);
        }

        public static ValidationResult limitReached() {
            return new ValidationResult(false, "Превышен лимит использований", null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public InviteLink getInviteLink() {
            return inviteLink;
        }
    }

    public record LinkStatistics(long activeLinks, long totalUsages) {
    }
}
