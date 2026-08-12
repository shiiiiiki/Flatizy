package org.flatizy.flatizy.handler;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.*;
import org.flatizy.flatizy.entity.enums.RequestType;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.flatizy.flatizy.entity.enums.UserSessionState;
import org.flatizy.flatizy.entity.mapper.ApartmentMapper;
import org.flatizy.flatizy.service.*;
import org.flatizy.flatizy.service.apartment.ApartmentService;
import org.flatizy.flatizy.service.user.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramUpdateHandler {

    private final UserService userService;
    private final InviteLinkService inviteLinkService;
    private final UserSessionService userSessionService;
    private final ApartmentService apartmentService;
    private final UserApartmentService userApartmentService;
    private final ApartmentMapper apartmentMapper;
    private final RequestService requestService;
    private final FeedbackService feedbackService;
    private final NotificationService notificationService;

    @Setter
    private TelegramWebhookBot bot;


    public void handle(Update update) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();

            if (data.startsWith("APT:") || data.equals("APT_DONE") || data.equals("APT_CANCEL")) {
                handleApartmentSelection(update);
                return;
            }

            if (data.startsWith("NOTIF_TOGGLE:")) {
                handleNotificationToggle(update);
                return;
            }

            if (data.startsWith("invite_delete_")) {
                handleInviteDelete(update);
                return;
            }

            if (data.startsWith("REQ_") || data.startsWith("REQ_TYPE:") || data.startsWith("REQ_APT:")) {
                handleRequestCallback(update);
                return;
            }

            return;
        }

        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        Long chatId = message.getChatId();

        if (message.hasContact()) {
            handleContact(chatId, message.getContact());
            return;
        }

        if (message.hasText()) {
            String text = message.getText();

            if (text.startsWith("/start")) {
                handleStart(chatId, text);
                return;
            }

            if (text.equals("/menu")) {
                userSessionService.clearSession(chatId);
                Optional<User> userOpt = userService.findByTelegramId(chatId);
                if (userOpt.isPresent()) {
                    showMenuByRole(chatId, userOpt.get().getRole());
                }
                return;
            }

            UserSession session = userSessionService.getOrCreateSession(chatId);
            UserSessionState state = session.getState();

            if (text.equals("🔗 Створити запрошення") || text.equals("📋 Мої запрошення") ||
                    text.equals("🔔 Сповіщення") || text.equals("💬 Відгук") || text.equals("ℹ️ Допомога") ||
                    text.equals("📊 Статистика") || text.equals("⚙️ Налаштування") ||
                    text.equals("❌ Скасувати створення запрошення") ||
                    text.equals("🔗 Создать Invite") || text.equals("📋 Мои Invite") ||
                    text.equals("🔔 Notifications") || text.equals("💬 Feedback") ||
                    text.equals("ℹ️ Help") || text.equals("ℹ️ Помощь") ||
                    text.equals("📝 Create Request") || text.equals("📋 My Request") ||
                    text.equals("📝 Створити заявку") || text.equals("📋 Мої заявки")) {

                if ((state == UserSessionState.CREATING_INVITE_LINK_SELECTING_APARTMENTS ||
                        state == UserSessionState.CREATING_INVITE_LINK_USES ||
                        state == UserSessionState.CREATING_INVITE_LINK_DAYS) &&
                        (text.equals("❌ Скасувати створення запрошення") || text.equals("❌ Отмена создание инвайта") || text.equals("❌ Відміна"))) {

                    Optional<User> userOpt = userService.findByTelegramId(chatId);
                    if (userOpt.isPresent()) {
                        userSessionService.clearSession(chatId);
                        sendMessageWithRemoveKeyboard(chatId, "❌ Створення invite-посилання скасовано.");
                        showMenuByRole(chatId, userOpt.get().getRole());
                    }
                    return;
                }

                handleMenuCommand(chatId, text);
                return;
            }

            switch (state) {
                case WAITING_FOR_INVITE_LINK -> handleInviteLinkInput(chatId, text);
                case CREATING_INVITE_LINK_USES -> handleInviteLinkUsesInput(chatId, text);
                case CREATING_INVITE_LINK_DAYS -> handleInviteLinkDaysInput(chatId, text);
                case CREATING_INVITE_LINK_SELECTING_APARTMENTS -> handleApartmentSearch(chatId, text);
                case CREATING_REQUEST_ENTERING_DESCRIPTION -> handleRequestDescriptionInput(chatId, text);
                case CREATING_FEEDBACK -> handleFeedbackInput(chatId, text);
                default -> handleMenuCommand(chatId, text);
            }
        }
    }

    // ==========================
    // START COMMAND
    // ==========================

    private void handleStart(Long chatId, String text) {
        Optional<User> existingUser = userService.findByTelegramId(chatId);
        if (existingUser.isPresent()) {
            userSessionService.clearSession(chatId);
            sendMessage(chatId, "Ви вже авторизовані ✅");
            showMenuByRole(chatId, existingUser.get().getRole());
            return;
        }

        userSessionService.clearSession(chatId);

        String[] parts = text.split(" ");
        if (parts.length > 1) {
            String inviteCode = parts[1];
            handleStartWithInviteLink(chatId, inviteCode);
        } else {
            askPhoneNumber(chatId);
        }
    }

    private void handleStartWithInviteLink(Long chatId, String inviteCode) {
        Optional<InviteLink> linkOpt = inviteLinkService.findByCode(inviteCode);

        if (linkOpt.isEmpty()) {
            sendMessage(chatId, "❌ Посилання не знайдено.\n\nНатисніть /start для реєстрації.");
            return;
        }

        InviteLink link = linkOpt.get();

        if (!link.isValid()) {
            String reason = !link.isActive() ? "деактивована" :
                    link.isExpired() ? "истекла" :
                            link.getUsedCount() >= link.getMaxUses() ? "превышен лимит использований" :
                                    "недействительна";
            sendMessage(chatId, "❌ Посилання " + reason + ".\n\nНатисніть /start для реєстрації.");
            return;
        }

        userSessionService.setInviteCode(chatId, inviteCode);
        askPhoneNumber(chatId);
    }

    private void handleCreateRequest(Long chatId, User user) {
        userSessionService.updateState(chatId, UserSessionState.CREATING_REQUEST_SELECTING_TYPE);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        RequestType[] types = RequestType.values();
        for (int i = 0; i < types.length; i += 2) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton btn1 = new InlineKeyboardButton();
            btn1.setText(types[i].getDisplayName());
            btn1.setCallbackData("REQ_TYPE:" + types[i].name());
            row.add(btn1);

            if (i + 1 < types.length) {
                InlineKeyboardButton btn2 = new InlineKeyboardButton();
                btn2.setText(types[i + 1].getDisplayName());
                btn2.setCallbackData("REQ_TYPE:" + types[i + 1].name());
                row.add(btn2);
            }

            rows.add(row);
        }

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Відміна");
        cancelBtn.setCallbackData("REQ_CANCEL");
        rows.add(List.of(cancelBtn));

        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("📝 Виберіть тип заявки:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private void handleRequestCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.equals("REQ_CANCEL")) {
            Optional<User> userOpt = userService.findByTelegramId(chatId);
            if (userOpt.isPresent()) {
                userSessionService.clearSession(chatId);
                sendMessage(chatId, "❌ Створення заявки скасовано.");
                showMenuByRole(chatId, userOpt.get().getRole());
            }
            return;
        }

        if (data.startsWith("REQ_TYPE:")) {
            String typeName = data.substring(9);
            RequestType type = RequestType.valueOf(typeName);


            userSessionService.updateSessionType(chatId, type);


            Optional<User> userOpt = userService.findByTelegramId(chatId);
            if (userOpt.isEmpty()) return;

            List<Apartment> apartments = userApartmentService.getApartmentsByUser(userOpt.get());

            if (apartments.isEmpty()) {
                askRequestDescription(chatId);
            } else {
                showRequestApartmentSelection(chatId, apartments);
            }
            return;
        }

        if (data.startsWith("REQ_APT:")) {
            Integer apartmentId = Integer.parseInt(data.substring(8));

            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setRequestApartmentId(apartmentId);

            askRequestDescription(chatId);
            return;
        }

        if (data.equals("REQ_SKIP_APT")) {
            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setRequestApartmentId(null);

            askRequestDescription(chatId);
        }
    }

    private void showRequestApartmentSelection(Long chatId, List<Apartment> apartments) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < Math.min(apartments.size(), 10); i++) {
            Apartment apt = apartments.get(i);

            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("🏠 Будинок " + apt.getHouseNumber() + ", Кв. " + apt.getApartmentNumber());
            btn.setCallbackData("REQ_APT:" + apt.getId());

            rows.add(List.of(btn));
        }

        InlineKeyboardButton skipBtn = new InlineKeyboardButton();
        skipBtn.setText("➡️ Пропустити (загальна заявка)");
        skipBtn.setCallbackData("REQ_SKIP_APT");
        rows.add(List.of(skipBtn));

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Відміна");
        cancelBtn.setCallbackData("REQ_CANCEL");
        rows.add(List.of(cancelBtn));

        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Виберіть квартиру, до якої відноситься заявка:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private void askRequestDescription(Long chatId) {
        userSessionService.updateState(chatId, UserSessionState.CREATING_REQUEST_ENTERING_DESCRIPTION);

        sendMessageWithRemoveKeyboard(chatId,
                "📝 Опишіть проблему детально:\n\n" +
                        "Наприклад: \"Ліфт застряг на 5 поверсі, не працює вже 2 години\"\n\n" +
                        "Для скасування напишіть /menu");
    }

    private void handleRequestDescriptionInput(Long chatId, String description) {
        if (description.trim().length() < 10) {
            sendMessage(chatId, "❌ Опис занадто короткий. Будь ласка, опишіть проблему докладніше (мінімум 10 символів):");
            return;
        }

        UserSession session = userSessionService.getOrCreateSession(chatId);

        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Помилка. Натисніть /start");
            return;
        }

        User user = userOpt.get();

        Apartment apartment = null;
        if (session.getRequestApartmentId() != null) {
            apartment = apartmentService.findById(session.getRequestApartmentId()).orElse(null);
        }

        try {
            Request request = requestService.createRequest(
                    user,
                    session.getRequestType(),
                    description,
                    apartment
            );

            String apartmentInfo = apartment != null ?
                    "\n🏠 Квартира: Будинок " + apartment.getHouseNumber() + ", Кв " + apartment.getApartmentNumber() :
                    "\n🏠 Загальна заявка";

            sendMessage(chatId,
                    "✅ Заявку успішно створено!\n\n" +
                            "🆔 Номер заявки: #" + request.getId() + "\n" +
                            "📋 Тип: " + request.getType().getDisplayName() +
                            apartmentInfo + "\n" +
                            "📝 Опис: " + description + "\n\n" +
                            "Ми розглянемо вашу заявку найближчим часом.");

            userSessionService.clearSession(chatId);
            showMenuByRole(chatId, user.getRole());

        } catch (Exception e) {
            log.error("Error creating request", e);
            sendMessage(chatId, "❌ Помилка при створенні заявки. Спробуйте пізніше.");
            userSessionService.clearSession(chatId);
            showMenuByRole(chatId, user.getRole());
        }
    }

    private void handleMyRequests(Long chatId, User user) {
        List<Request> requests = requestService.getUserRequests(user);

        if (requests.isEmpty()) {
            sendMessage(chatId, "У вас поки що немає створених заявок.");
            return;
        }

        StringBuilder sb = new StringBuilder("📋 Ваші заявки:\n\n");

        for (Request request : requests) {
            String statusEmoji = switch (request.getStatus()) {
                case PENDING -> "🕐";
                case IN_PROGRESS -> "🔄";
                case COMPLETED -> "✅";
                case REJECTED -> "❌";
            };

            sb.append("━━━━━━━━━━━━━━━━━━\n");
            sb.append("🆔 Заявка #").append(request.getId()).append("\n");
            sb.append("📋 Тип: ").append(request.getType().getDisplayName()).append("\n");
            sb.append("📊 Статус: ").append(statusEmoji).append(" ").append(request.getStatus().getDisplayName()).append("\n");

            if (request.getApartment() != null) {
                sb.append("🏠 Квартира: Будинок ").append(request.getApartment().getHouseNumber())
                        .append(", Кв ").append(request.getApartment().getApartmentNumber()).append("\n");
            }

            sb.append("📝 Опис: ").append(request.getDescription()).append("\n");
            sb.append("📅 Створена: ").append(request.getCreatedAt().toLocalDate()).append("\n");
            if (request.getFeedback() != null) {
                sb.append("📝 Коментар: ").append(request.getFeedback()).append("\n");
            }
            if (request.getCompletedAt() != null) {
                sb.append("✅ Виконана: ").append(request.getCompletedAt().toLocalDate()).append("\n");
            }

            sb.append("\n");
        }

        sendMessage(chatId, sb.toString());
    }

    // ==========================
    // ASK PHONE
    // ==========================

    private void askPhoneNumber(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Будь ласка, поділіться номером телефону:");

        KeyboardButton phoneBtn = new KeyboardButton("📱 Поділитися номером");
        phoneBtn.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(phoneBtn);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        markup.setKeyboard(List.of(row));

        msg.setReplyMarkup(markup);
        execute(msg);
    }

    // ==========================
    // HANDLE CONTACT
    // ==========================

    private void handleContact(Long chatId, Contact contact) {
        String phone = contact.getPhoneNumber();
        Optional<User> userOpt = userService.findByPhone(phone);

        if (userOpt.isPresent()) {
            handleExistingUser(chatId, contact, userOpt.get());
        } else {
            handleNewUser(chatId, contact, phone);
        }
    }

    private void handleExistingUser(Long chatId, Contact contact, User user) {
        user.setTelegramId(contact.getUserId());
        user.setBotStarted(true);
        userService.save(user);

        sendMessageWithRemoveKeyboard(chatId,
                "✅ Ласкаво просимо, " + user.getFirstName() + "!");

        showMenuByRole(chatId, user.getRole());
    }

    private void handleNewUser(Long chatId, Contact contact, String phone) {
        Optional<String> inviteCodeOpt = userSessionService.getInviteCode(chatId);

        if (inviteCodeOpt.isPresent()) {
            registerUserWithInviteLink(chatId, contact, phone, inviteCodeOpt.get());
        } else {
            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setRegistrationPhone(phone);

            askInviteLink(chatId);
        }
    }

    private void registerUserWithInviteLink(Long chatId, Contact contact, String phone, String inviteCode) {
        InviteLinkService.ValidationResult result = inviteLinkService.validateAndUse(inviteCode);

        if (!result.isValid()) {
            sendMessageWithRemoveKeyboard(chatId, "❌ " + result.getErrorMessage());
            askInviteLink(chatId);
            return;
        }

        InviteLink link = result.getInviteLink();

        User newUser = new User();
        newUser.setTelegramId(contact.getUserId());
        newUser.setPhone(phone);
        newUser.setFirstName(contact.getFirstName());
        newUser.setLastName(contact.getLastName());
        newUser.setBotStarted(true);
        newUser.setRole(link.getTargetRole());

        userService.save(newUser);

        for (Apartment apartment : link.getApartments()) {
            userApartmentService.linkUserToApartment(newUser, apartment);
        }

        userSessionService.clearSession(chatId);

        String roleText = link.getTargetRole() == UserRole.OWNER ? "владельця" : "арендатора";
        String apartmentInfo = link.getApartments().isEmpty() ?
                "" :
                String.format("\n\n📍 Прив'язані квартири: %d", link.getApartments().size());

        sendMessageWithRemoveKeyboard(chatId,
                "✅ Реєстрація успішна!\n" +
                        "Ви зареєстровані як " + roleText + "." + apartmentInfo);

        showMenuByRole(chatId, newUser.getRole());
    }


    // ==========================
    // ASK INVITE LINK
    // ==========================

    private void askInviteLink(Long chatId) {
        userSessionService.updateState(chatId, UserSessionState.WAITING_FOR_INVITE_LINK);

        KeyboardButton inviteBtn = new KeyboardButton("🔗 Ввести invite link");

        KeyboardRow row = new KeyboardRow();
        row.add(inviteBtn);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);
        markup.setKeyboard(List.of(row));

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Ваш номер не знайдено в системі.\n" +
                "Будь ласка, отримайте invite посилання від власника приміщення.");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    // ==========================
    // HANDLE INVITE LINK INPUT
    // ==========================

    private void handleInviteLinkInput(Long chatId, String text) {
        if (text.equals("🔗 Ввести invite link")) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("Введіть код invite посилання\n(це набір символів після /start в посиланні):");
            msg.setReplyMarkup(new ReplyKeyboardRemove(true));
            execute(msg);
            return;
        }

        String code = text.trim();

        if (code.contains("?start=")) {
            code = code.substring(code.indexOf("?start=") + 7);
        }

        UserSession session = userSessionService.getOrCreateSession(chatId);
        String savedPhone = session.getRegistrationPhone();

        if (savedPhone == null) {
            sendMessage(chatId, "❌ Помилка: телефон не знайдено. Будь ласка, почніть заново: /start");
            userSessionService.clearSession(chatId);
            return;
        }

        InviteLinkService.ValidationResult result = inviteLinkService.validateAndUse(code);

        if (!result.isValid()) {
            sendMessage(chatId, "❌ " + result.getErrorMessage() + "\n\nСпробуйте ще раз:");
            return;
        }

        InviteLink link = result.getInviteLink();

        User newUser = new User();
        newUser.setTelegramId(chatId);
        newUser.setPhone(savedPhone);
        newUser.setFirstName("");
        newUser.setLastName("");
        newUser.setBotStarted(true);
        newUser.setRole(link.getTargetRole());

        userService.save(newUser);

        for (Apartment apartment : link.getApartments()) {
            userApartmentService.linkUserToApartment(newUser, apartment);
        }

        userSessionService.clearSession(chatId);

        String roleText = link.getTargetRole() == UserRole.OWNER ? "владельця" : "арендатора";
        String apartmentInfo = link.getApartments().isEmpty() ?
                "" :
                String.format("\n\n📍 Прив'язані квартири: %d", link.getApartments().size());

        sendMessageWithRemoveKeyboard(chatId,
                "✅ Реєстрація успішна!\n" +
                        "Ви зареєстровані як " + roleText + "." + apartmentInfo);

        showMenuByRole(chatId, newUser.getRole());
    }

    // ==========================
    // SHOW MENU BY ROLE
    // ==========================

    private void showMenuByRole(Long chatId, UserRole role) {
        switch (role) {
            case ADMIN -> showAdminMenu(chatId);
            case OWNER -> showOwnerMenu(chatId);
            case TENANT -> showTenantMenu(chatId);
        }
    }

    private void showAdminMenu(Long chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔗 Створити Invite");
        row1.add("📋 Мої Invite");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📊 Статистика");
        row2.add("⚙️ Налаштування");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("ℹ️ Допомога");

        markup.setKeyboard(List.of(row1, row2, row3));

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("🔧 Меню адміністратора:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private void showOwnerMenu(Long chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔗 Створити запрошення");
        row1.add("📋 Мої запрошення");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📝 Створити заявку");
        row2.add("📋 Мої заявки");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🔔 Сповіщення");
        row3.add("💬 Відгук");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("ℹ️ Допомога");

        markup.setKeyboard(List.of(row1, row2, row3, row4));

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("🏠 Меню власника:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private void showTenantMenu(Long chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📝 Створити заявку");
        row1.add("📋 Мої заявки");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔔 Сповіщення");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("💬 Відгук");
        row3.add("ℹ️ Допомога");

        markup.setKeyboard(List.of(row1, row2, row3));

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("👤 Меню орендаря:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    // ==========================
    // MENU COMMANDS
    // ==========================

    private void handleMenuCommand(Long chatId, String text) {
        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Сталась помилка. Натисніть /start");
            return;
        }

        User user = userOpt.get();

        switch (text) {
            case "🔗 Створити запрошення", "🔗 Создать Invite" -> handleCreateInvite(chatId, user);
            case "📋 Мої запрошення", "📋 Мои Invite" -> handleMyInvites(chatId, user);
            case "📝 Створити заявку", "📝 Create Request" -> handleCreateRequest(chatId, user);
            case "📋 Мої заявки", "📋 My Request" -> handleMyRequests(chatId, user);
            case "🔔 Сповіщення", "🔔 Notifications" -> handleNotifications(chatId);
            case "💬 Відгук", "💬 Feedback" -> handleFeedback(chatId);
            case "ℹ️ Допомога", "ℹ️ Help", "ℹ️ Помощь" -> handleHelp(chatId);
            case "📊 Статистика" -> handleStatistics(chatId, user);
            case "⚙️ Налаштування", "⚙️ Настройки" -> handleSettings(chatId);
            default -> sendMessage(chatId, "Команду не розпізнано");
        }
    }

    // ==========================
    // CREATE INVITE LINK
    // ==========================

    private void handleCreateInvite(Long chatId, User user) {
        if (user.getRole() == UserRole.TENANT) {
            sendMessage(chatId, "❌ У вас немає прав для створення invite-посилань");
            return;
        }

        List<Apartment> apartments;
        if (user.getRole() == UserRole.ADMIN) {
            apartments = apartmentService.getAll();
        } else {
            apartments = userApartmentService.getApartmentsByUser(user);
            log.info("User {} role {}, apartments count: {}", user.getId(), user.getRole(), apartments.size());
        }

        if (apartments.isEmpty()) {
            sendMessage(chatId, "❌ Немає доступних квартир для створення посилання");
            return;
        }

        userSessionService.clearApartmentSelection(chatId);

        UserRole targetRole = user.getRole() == UserRole.ADMIN ? UserRole.OWNER : UserRole.TENANT;
        UserSession session = userSessionService.getOrCreateSession(chatId);
        session.setInviteTargetRole(targetRole);
        userSessionService.updateState(chatId, UserSessionState.CREATING_INVITE_LINK_SELECTING_APARTMENTS);

        if (apartments.size() <= 10) {
            showApartmentSelectionMenu(chatId, apartments);
        } else {
            sendApartmentSearchPrompt(chatId);
        }
    }

    private void showApartmentSelectionMenu(Long chatId, List<Apartment> apartments) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < Math.min(apartments.size(), 10); i += 2) {

            List<InlineKeyboardButton> row = new ArrayList<>();

            Apartment apt1 = apartments.get(i);
            row.add(createApartmentButton(apt1));

            if (i + 1 < apartments.size()) {
                Apartment apt2 = apartments.get(i + 1);
                row.add(createApartmentButton(apt2));
            }

            rows.add(row);
        }

        InlineKeyboardButton doneBtn = new InlineKeyboardButton();
        doneBtn.setText("✅ Готово");
        doneBtn.setCallbackData("APT_DONE");

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Відміна");
        cancelBtn.setCallbackData("APT_CANCEL");

        rows.add(List.of(doneBtn, cancelBtn));

        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(
                "Виберіть квартири для цього invite посилання:\n\n" +
                        "Натискайте на квартири, потім натисніть «Готово" +
                        "\n\n" +
                        "💡 Щоб скасувати, натисніть «Відміна» або напишіть /menu"
        );
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private InlineKeyboardButton createApartmentButton(Apartment apt) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("🏠 Будинок " + apt.getHouseNumber() + ", Кв. " + apt.getApartmentNumber());
        btn.setCallbackData("APT:" + apt.getId());
        return btn;
    }

    private void handleApartmentSelection(Update update) {

        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (data.equals("APT_CANCEL")) {
            Optional<User> userOpt = userService.findByTelegramId(chatId);
            if (userOpt.isPresent()) {
                userSessionService.clearSession(chatId);
                sendMessageWithRemoveKeyboard(chatId, "❌ Створення invite-посилання скасовано.");
                showMenuByRole(chatId, userOpt.get().getRole());
            }
            return;
        }

        if (data.equals("APT_DONE")) {

            List<Integer> selected = userSessionService.getSelectedApartments(chatId);

            if (selected.isEmpty()) {
                sendMessage(chatId, "❌ Оберіть принаймні одну квартиру!");
                return;
            }

            userSessionService.updateState(
                    chatId,
                    UserSessionState.CREATING_INVITE_LINK_USES
            );

            sendMessage(chatId,
                    "✅ Вибрано квартир: " + selected.size() + "\n\n" +
                            "Скільки разів можна використовувати посилання? (1-5)"
            );
            return;
        }

        if (data.startsWith("APT:")) {

            Integer apartmentId =
                    Integer.parseInt(data.substring(4));

            userSessionService.addApartmentSelection(chatId, apartmentId);

            List<Integer> selected =
                    userSessionService.getSelectedApartments(chatId);

            sendMessage(chatId,
                    "🏠 Квартира додана!\n" +
                            "Вибрано: " + selected.size());
        }
    }

    private void handleInviteLinkUsesInput(Long chatId, String text) {
        try {
            int uses = Integer.parseInt(text.trim());

            if (uses < 1 || uses > 5) {
                sendMessage(chatId, "❌ Кількість повинна бути від 1 до 5. Спробуйте ще раз:");
                return;
            }

            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setInviteMaxUses(uses);

            userSessionService.updateState(chatId, UserSessionState.CREATING_INVITE_LINK_DAYS);
            sendMessage(chatId, "На скільки днів створити посилання? (від 1 до 3)\n\n" +
                    "Введіть число:\n\n" +
                    "💡 Щоб скасувати створення посилання, напишіть: /menu");

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Будь ласка, введіть число від 1 до 5:");
        }
    }

    private void handleApartmentSearch(Long chatId, String text) {
        if (!text.matches("\\d+")) {
            sendApartmentSearchPrompt(chatId);
            return;
        }

        int aptNumber = Integer.parseInt(text.trim());

        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();

        List<Apartment> available;
        if (user.getRole() == UserRole.ADMIN) {
            available = apartmentService.getAll();
        } else {
            available = userApartmentService.getApartmentsByUser(user);
            log.info("User {} role {}, apartments count: {}", user.getId(), user.getRole(), available.size());
        }

        List<Apartment> matched = available.stream()
                .filter(a -> a.getApartmentNumber() == aptNumber)
                .toList();

        if (matched.isEmpty()) {
            sendMessage(chatId, "❌ Квартира з таким номером не знайдена серед ваших квартир");
        } else if (matched.size() == 1) {
            userSessionService.addApartmentSelection(chatId, matched.get(0).getId());
            sendMessage(chatId, "🏠 Квартира додана: Будинок " + matched.get(0).getHouseNumber() + ", Кв " + aptNumber);
        } else {
            StringBuilder sb = new StringBuilder("Знайдено кілька квартир з таким номером:\n");
            for (Apartment apt : matched) {
                sb.append("🏠 Будинок ").append(apt.getHouseNumber())
                        .append(" — ID: ").append(apt.getId()).append("\n");
            }
            sb.append("\nВведіть ID квартири для додавання:");
            sendMessage(chatId, sb.toString());
        }
    }

    private void handleInviteLinkDaysInput(Long chatId, String text) {
        try {
            int days = Integer.parseInt(text.trim());

            if (days < 1 || days > 3) {
                sendMessage(chatId, "❌ Кількість днів повинна бути від 1 до 3. Спробуйте ще раз:");
                return;
            }

            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setInviteExpirationDays(days);


            Optional<User> userOpt = userService.findByTelegramId(chatId);
            if (userOpt.isEmpty()) {
                sendMessage(chatId, "Помилка. Натисніть /start");
                return;
            }

            User user = userOpt.get();
            List<Integer> apartmentIds = userSessionService.getSelectedApartments(chatId);

            try {
                InviteLink link = inviteLinkService.createInviteLink(
                        user,
                        session.getInviteTargetRole(),
                        session.getInviteMaxUses(),
                        days,
                        apartmentIds
                );

                String telegramLink = inviteLinkService.getTelegramLink(link.getCode());
                String roleText = link.getTargetRole() == UserRole.OWNER ? "владельця" : "арендатора";

                sendMessage(chatId,
                        "✅ Invite посилання створено!\n\n" +
                                "🔗 Посилання: " + telegramLink + "\n" +
                                "👤 Роль: " + roleText + "\n" +
                                "🏠 Квартири: " + link.getApartments().size() + "\n" +
                                "📊 Використань: 0/" + link.getMaxUses() + "\n" +
                                "⏰ Діє до: " + link.getExpiresAt().toLocalDate() + "\n\n" +
                                "Відправте це посилання користувачу для реєстрації.");

                userSessionService.clearSession(chatId);

                showMenuByRole(chatId, user.getRole());

            } catch (Exception e) {
                log.error("Ошибка создания invite link", e);
                sendMessage(chatId, "❌ Помилка створення посилання: " + e.getMessage());
                userSessionService.clearSession(chatId);
                showMenuByRole(chatId, user.getRole());
            }

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Будь ласка, введіть число від 1 до 3:");
        }
    }

    // ==========================
    // MY INVITES
    // ==========================

    private void handleMyInvites(Long chatId, User user) {

        List<InviteLink> links = inviteLinkService.getAllLinks(user);

        if (links.isEmpty()) {
            sendMessage(chatId, "У вас поки що немає створених invite посилань.");
            return;
        }

        sendMessage(chatId, "📋 Ваші invite посилання:");

        for (InviteLink link : links) {

            boolean valid = link.isValid();

            String apartments = link.getApartments().isEmpty()
                    ? "немає"
                    : link.getApartments().stream()
                    .map(a -> String.valueOf(a.getApartmentNumber()))
                    .collect(Collectors.joining(", "));

            StringBuilder sb = new StringBuilder();

            sb.append(valid ? "✅ Активна" : "❌ Неактивна")
                    .append(" | кв. ").append(apartments)
                    .append(" | ").append(link.getUsedCount())
                    .append("/").append(link.getMaxUses())
                    .append(" | до ").append(link.getExpiresAt().toLocalDate())
                    .append("\n");

            if (valid) {
                sb.append("🔗 ")
                        .append(inviteLinkService.getTelegramLink(link.getCode()));
            } else {
                sb.append("⚠ ")
                        .append(getInactiveReason(link));
            }

            InlineKeyboardButton deleteBtn =
                    new InlineKeyboardButton("🗑 Видалити");
            deleteBtn.setCallbackData("invite_delete_" + link.getId());

            InlineKeyboardMarkup keyboard =
                    new InlineKeyboardMarkup(
                            List.of(List.of(deleteBtn))
                    );

            sendMessage(chatId, sb.toString(), keyboard);
        }
    }

    private String getInactiveReason(InviteLink link) {
        if (link.getExpiresAt().isBefore(LocalDateTime.now())) return "Термін дії минув";
        if (link.getUsedCount() >= link.getMaxUses()) return "Вичерпано ліміт використань";
        return "Недоступна";
    }

    // ==========================
    // OTHER HANDLERS
    // ==========================

    private void handleInviteDelete(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        Integer inviteId = Integer.parseInt(data.replace("invite_delete_", ""));

        Optional<InviteLink> linkOpt = inviteLinkService.getById(inviteId);

        if (linkOpt.isEmpty()) {
            editMessageText(chatId, messageId, "⚠ Це посилання вже недоступне.");
            return;
        }

        InviteLink link = linkOpt.get();

        inviteLinkService.delete(link);

        editMessageText(chatId, messageId, "✅ Invite посилання видалено.");

        Optional<User> userOpt = userService.findByTelegramId(chatId);
        userOpt.ifPresent(user -> handleMyInvites(chatId, user));
    }

    private void editMessageText(Long chatId, Integer messageId, String text) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(text);
        execute(editMessage);
    }

    private void execute(EditMessageText edit) {
        try {
            bot.execute(edit);
        } catch (TelegramApiException e) {
            log.error("Telegram error", e);
        }
    }

    private void handleFeedback(Long chatId) {
        userSessionService.updateState(chatId, UserSessionState.CREATING_FEEDBACK);

        sendMessageWithRemoveKeyboard(chatId,
                "💬 Напишіть ваш коментар або пропозицію.\n\n" +
                        "Для скасування напишіть /menu");
    }

    private void handleHelp(Long chatId) {
        sendMessage(chatId,
                "ℹ️ Справка по боту:\n\n" +
                        "Цей бот допомагає управляти приміщеннями та орендарями.\n\n" +
                        "Використовуйте меню для навігації.");
    }

    private void handleStatistics(Long chatId, User user) {
        InviteLinkService.LinkStatistics stats = inviteLinkService.getStatistics(user);
        sendMessage(chatId,
                "📊 Статистика:\n\n" +
                        "Активних посилань: " + stats.activeLinks() + "\n" +
                        "Всього використань: " + stats.totalUsages());
    }

    private void handleSettings(Long chatId) {
        sendMessage(chatId, "⚙️ Налаштування (в розробці)");
    }

    // ==========================
    // FEEDBACK
    // ==========================

    private void handleFeedbackInput(Long chatId, String text) {

        if (text.trim().length() < 5) {
            sendMessage(chatId, "❌ Повідомлення занадто коротке. Напишіть докладніше:");
            return;
        }

        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Помилка. Натисніть /start");
            return;
        }

        User user = userOpt.get();

        feedbackService.save(user, text);

        userSessionService.clearSession(chatId);

        sendMessage(chatId,
                "✅ Дякуємо! Ваш feedback збережено і буде оброблено найближчим часом.");

        showMenuByRole(chatId, user.getRole());
    }

    // ==========================
    // NOTIFICATIONS
    // ==========================

    private void handleNotifications(Long chatId) {

        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        List<NotificationType> types =
                notificationService.getAllTypes();

        InlineKeyboardMarkup markup = buildNotificationKeyboard(user, types);

        sendMessage(chatId,
                "🔔 Налаштування сповіщень\n\n" +
                        "Натисніть на пункт щоб включити/вимкнути:",
                markup);
    }

    private InlineKeyboardMarkup buildNotificationKeyboard(
            User user,
            List<NotificationType> types
    ) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (NotificationType type : types) {

            boolean enabled =
                    notificationService.isEnabled(user, type);

            String icon = enabled ? "✅ " : "❌ ";

            InlineKeyboardButton btn =
                    new InlineKeyboardButton(
                            icon + type.getTitle()
                    );

            btn.setCallbackData(
                    "NOTIF_TOGGLE:" + type.getId()
            );

            rows.add(List.of(btn));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    private void handleNotificationToggle(Update update) {

        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId =
                update.getCallbackQuery().getMessage().getMessageId();

        Long typeId =
                Long.parseLong(data.replace("NOTIF_TOGGLE:", ""));

        Optional<User> userOpt =
                userService.findByTelegramId(chatId);

        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        Optional<NotificationType> typeOpt =
                notificationService.getTypeById(typeId);

        if (typeOpt.isEmpty()) return;

        NotificationType type = typeOpt.get();

        notificationService.toggle(user, type);

        List<NotificationType> types =
                notificationService.getAllTypes();

        InlineKeyboardMarkup markup =
                buildNotificationKeyboard(user, types);

        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId.toString());
        edit.setMessageId(messageId);
        edit.setText(
                "🔔 Налаштування сповіщень\n\n" +
                        "Натисніть на пункт щоб включити/вимкнути:"
        );
        edit.setReplyMarkup(markup);

        execute(edit);
    }


    // ==========================
    // UTILS
    // ==========================

    private void sendMessage(Long chatId, String text) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        execute(msg);
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        msg.setReplyMarkup(keyboard);
        execute(msg);
    }

    private void sendMessageWithRemoveKeyboard(Long chatId, String text) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        msg.setReplyMarkup(new ReplyKeyboardRemove(true));
        execute(msg);
    }

    private void execute(SendMessage msg) {
        try {
            bot.execute(msg);
        } catch (TelegramApiException e) {
            log.error("Telegram error", e);
        }
    }

    private void sendApartmentSearchPrompt(Long chatId) {
        sendMessage(chatId,
                "У вас занадто багато квартир для відображення кнопками.\n" +
                        "Будь ласка, введіть номер квартири в чат, щоб додати її до invite-посилання.\n" +
                        "Наприклад: 12");
    }

    public void sendMessageToUser(String phoneNumber, String message) {
        Optional<User> userOpt = userService.findByPhone(phoneNumber);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getTelegramId() != null) {
                sendMessage(user.getTelegramId(), message);
            }
        }
    }

    public void sendMessageByChatId(Long chatId, String message) {
        sendMessage(chatId, message);
    }
}
