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

            // Обработка callback для квартир (invite)
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

            // ✅ НОВОЕ: Обработка callback для заявок
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

            // Обработка /start с параметром (invite link)
            if (text.startsWith("/start")) {
                handleStart(chatId, text);
                return;
            }

            // Обработка /menu - вернуться в меню и отменить текущее действие
            if (text.equals("/menu")) {
                userSessionService.clearSession(chatId);
                Optional<User> userOpt = userService.findByTelegramId(chatId);
                if (userOpt.isPresent()) {
                    showMenuByRole(chatId, userOpt.get().getRole());
                }
                return;
            }

            // Проверяем состояние пользователя из БД
            UserSession session = userSessionService.getOrCreateSession(chatId);
            UserSessionState state = session.getState();

            // ✅ Специальные команды которые работают в любом состоянии
            // Если нажата кнопка отмены или других главных меню команд
            if (text.equals("🔗 Создать Invite") || text.equals("📋 Мои Invite") ||
                text.equals("🔔 Notifications") || text.equals("💬 Feedback") || text.equals("ℹ️ Help") ||
                text.equals("ℹ️ Помощь") || text.equals("📊 Статистика") || text.equals("⚙️ Настройки") ||
                text.equals("❌ Отмена создание инвайта")) {

                // Если мы в процессе создания инвайта и нажали отмену
                if ((state == UserSessionState.CREATING_INVITE_LINK_SELECTING_APARTMENTS ||
                     state == UserSessionState.CREATING_INVITE_LINK_USES ||
                     state == UserSessionState.CREATING_INVITE_LINK_DAYS) &&
                    text.equals("❌ Отмена создание инвайта")) {

                    Optional<User> userOpt = userService.findByTelegramId(chatId);
                    if (userOpt.isPresent()) {
                        userSessionService.clearSession(chatId);
                        sendMessageWithRemoveKeyboard(chatId, "❌ Создание invite ссылки отменено.");
                        showMenuByRole(chatId, userOpt.get().getRole());
                    }
                    return;
                }

                // Иначе обработать как обычную команду меню
                handleMenuCommand(chatId, text);
                return;
            }

            // Основная логика обработки состояний
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

        // 🔹 Если пользователь уже существует → просто показать меню
        Optional<User> existingUser = userService.findByTelegramId(chatId);
        if (existingUser.isPresent()) {
            // Сбрасываем состояние, чтобы исправить случаи когда юзер перезагрузил бота
            userSessionService.clearSession(chatId);
            sendMessage(chatId, "Вы уже авторизованы ✅");
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
        // Проверяем валидность ссылки БЕЗ использования
        Optional<InviteLink> linkOpt = inviteLinkService.findByCode(inviteCode);

        if (linkOpt.isEmpty()) {
            sendMessage(chatId, "❌ Ссылка не найдена.\n\nНажмите /start для регистрации.");
            return;
        }

        InviteLink link = linkOpt.get();

        if (!link.isValid()) {
            String reason = !link.isActive() ? "деактивирована" :
                    link.isExpired() ? "истекла" :
                            link.getUsedCount() >= link.getMaxUses() ? "превышен лимит использований" :
                                    "недействительна";
            sendMessage(chatId, "❌ Ссылка " + reason + ".\n\nНажмите /start для регистрации.");
            return;
        }

        // Сохраняем код в БД для использования после получения телефона
        userSessionService.setInviteCode(chatId, inviteCode);
        askPhoneNumber(chatId);
    }

    private void handleCreateRequest(Long chatId, User user) {
        userSessionService.updateState(chatId, UserSessionState.CREATING_REQUEST_SELECTING_TYPE);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Группируем типы заявок по 2 в ряд
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

        // Кнопка отмены
        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("REQ_CANCEL");
        rows.add(List.of(cancelBtn));

        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("📝 Выберите тип заявки:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private void handleRequestCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        // Отмена создания заявки
        if (data.equals("REQ_CANCEL")) {
            Optional<User> userOpt = userService.findByTelegramId(chatId);
            if (userOpt.isPresent()) {
                userSessionService.clearSession(chatId);
                sendMessage(chatId, "❌ Создание заявки отменено.");
                showMenuByRole(chatId, userOpt.get().getRole());
            }
            return;
        }

        // Выбран тип заявки
        if (data.startsWith("REQ_TYPE:")) {
            String typeName = data.substring(9);
            RequestType type = RequestType.valueOf(typeName);


            userSessionService.updateSessionType(chatId, type);


            // Теперь предлагаем выбрать квартиру или пропустить
            Optional<User> userOpt = userService.findByTelegramId(chatId);
            if (userOpt.isEmpty()) return;

            List<Apartment> apartments = userApartmentService.getApartmentsByUser(userOpt.get());

            if (apartments.isEmpty()) {
                // Нет квартир - сразу просим описание
                askRequestDescription(chatId);
            } else {
                // Показываем квартиры для выбора
                showRequestApartmentSelection(chatId, apartments);
            }
            return;
        }

        // Выбрана квартира
        if (data.startsWith("REQ_APT:")) {
            Integer apartmentId = Integer.parseInt(data.substring(8));

            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setRequestApartmentId(apartmentId);

            askRequestDescription(chatId);
            return;
        }

        // Пропустить выбор квартиры
        if (data.equals("REQ_SKIP_APT")) {
            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setRequestApartmentId(null);

            askRequestDescription(chatId);
            return;
        }
    }

    private void showRequestApartmentSelection(Long chatId, List<Apartment> apartments) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Показываем до 10 квартир
        for (int i = 0; i < Math.min(apartments.size(), 10); i++) {
            Apartment apt = apartments.get(i);

            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("🏠 Дом " + apt.getHouseNumber() + ", Кв " + apt.getApartmentNumber());
            btn.setCallbackData("REQ_APT:" + apt.getId());

            rows.add(List.of(btn));
        }

        // Кнопка "Пропустить" (для общих заявок)
        InlineKeyboardButton skipBtn = new InlineKeyboardButton();
        skipBtn.setText("➡️ Пропустить (общая заявка)");
        skipBtn.setCallbackData("REQ_SKIP_APT");
        rows.add(List.of(skipBtn));

        // Кнопка отмены
        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("REQ_CANCEL");
        rows.add(List.of(cancelBtn));

        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Выберите квартиру, к которой относится заявка:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private void askRequestDescription(Long chatId) {
        userSessionService.updateState(chatId, UserSessionState.CREATING_REQUEST_ENTERING_DESCRIPTION);

        sendMessageWithRemoveKeyboard(chatId,
                "📝 Опишите проблему подробно:\n\n" +
                        "Например: \"Лифт застрял на 5 этаже, не работает уже 2 часа\"\n\n" +
                        "Для отмены напишите /menu");
    }

    private void handleRequestDescriptionInput(Long chatId, String description) {
        if (description.trim().length() < 10) {
            sendMessage(chatId, "❌ Описание слишком короткое. Пожалуйста, опишите проблему подробнее (минимум 10 символов):");
            return;
        }

        UserSession session = userSessionService.getOrCreateSession(chatId);

        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Ошибка. Нажмите /start");
            return;
        }

        User user = userOpt.get();

        // Получаем квартиру если выбрана
        Apartment apartment = null;
        if (session.getRequestApartmentId() != null) {
            apartment = apartmentService.findById(session.getRequestApartmentId()).orElse(null);
        }

        try {
            // Создаем заявку
            Request request = requestService.createRequest(
                    user,
                    session.getRequestType(),
                    description,
                    apartment
            );

            String apartmentInfo = apartment != null ?
                    "\n🏠 Квартира: Дом " + apartment.getHouseNumber() + ", Кв " + apartment.getApartmentNumber() :
                    "\n🏠 Общая заявка";

            sendMessage(chatId,
                    "✅ Заявка успешно создана!\n\n" +
                            "🆔 Номер заявки: #" + request.getId() + "\n" +
                            "📋 Тип: " + request.getType().getDisplayName() +
                            apartmentInfo + "\n" +
                            "📝 Описание: " + description + "\n\n" +
                            "Мы рассмотрим вашу заявку в ближайшее время.");

            userSessionService.clearSession(chatId);
            showMenuByRole(chatId, user.getRole());

        } catch (Exception e) {
            log.error("Ошибка создания заявки", e);
            sendMessage(chatId, "❌ Ошибка при создании заявки. Попробуйте позже.");
            userSessionService.clearSession(chatId);
            showMenuByRole(chatId, user.getRole());
        }
    }

    private void handleMyRequests(Long chatId, User user) {
        List<Request> requests = requestService.getUserRequests(user);

        if (requests.isEmpty()) {
            sendMessage(chatId, "У вас пока нет созданных заявок.");
            return;
        }

        StringBuilder sb = new StringBuilder("📋 Ваши заявки:\n\n");

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
                sb.append("🏠 Квартира: Дом ").append(request.getApartment().getHouseNumber())
                        .append(", Кв ").append(request.getApartment().getApartmentNumber()).append("\n");
            }

            sb.append("📝 Описание: ").append(request.getDescription()).append("\n");
            sb.append("📅 Создана: ").append(request.getCreatedAt().toLocalDate()).append("\n");
            sb.append("📝 Комментарий: ").append(request.getFeedback()).append("\n");
            if (request.getCompletedAt() != null) {
                sb.append("✅ Выполнена: ").append(request.getCompletedAt().toLocalDate()).append("\n");
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
        msg.setText("Пожалуйста, поделитесь номером телефона:");

        KeyboardButton phoneBtn = new KeyboardButton("📱 Поделиться номером");
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
                "✅ Добро пожаловать, " + user.getFirstName() + "!");

        showMenuByRole(chatId, user.getRole());
    }

    private void handleNewUser(Long chatId, Contact contact, String phone) {
        Optional<String> inviteCodeOpt = userSessionService.getInviteCode(chatId);

        if (inviteCodeOpt.isPresent()) {
            // Есть код - регистрируем сразу
            registerUserWithInviteLink(chatId, contact, phone, inviteCodeOpt.get());
        } else {
            // ❌ НЕТ кода - СОХРАНЯЕМ телефон в сессию
            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setRegistrationPhone(phone);  // ✅ Сохраняем!

            askInviteLink(chatId);  // Просим ввести код
        }
    }

    private void registerUserWithInviteLink(Long chatId, Contact contact, String phone, String inviteCode) {
        // Валидируем и используем ссылку
        InviteLinkService.ValidationResult result = inviteLinkService.validateAndUse(inviteCode);

        if (!result.isValid()) {
            sendMessageWithRemoveKeyboard(chatId, "❌ " + result.getErrorMessage());
            askInviteLink(chatId);
            return;
        }

        InviteLink link = result.getInviteLink();

        // Создаем нового пользователя
        User newUser = new User();
        newUser.setTelegramId(contact.getUserId());
        newUser.setPhone(phone);
        newUser.setFirstName(contact.getFirstName());
        newUser.setLastName(contact.getLastName());
        newUser.setBotStarted(true);
        newUser.setRole(link.getTargetRole());

        userService.save(newUser);

        // Привязываем квартиры из инвайт-ссылки
        for (Apartment apartment : link.getApartments()) {
            userApartmentService.linkUserToApartment(newUser, apartment);
        }

        userSessionService.clearSession(chatId);

        String roleText = link.getTargetRole() == UserRole.OWNER ? "владельца" : "арендатора";
        String apartmentInfo = link.getApartments().isEmpty() ?
                "" :
                String.format("\n\n📍 Привязанные квартиры: %d", link.getApartments().size());

        sendMessageWithRemoveKeyboard(chatId,
                "✅ Регистрация успешна!\n" +
                        "Вы зарегистрированы как " + roleText + "." + apartmentInfo);

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
        msg.setText("Ваш номер не найден в системе.\n" +
                "Пожалуйста, получите invite ссылку от владельца помещения.");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    // ==========================
    // HANDLE INVITE LINK INPUT
    // ==========================

    private void handleInviteLinkInput(Long chatId, String text) {
        // Если нажата кнопка, просим ввести ссылку
        if (text.equals("🔗 Ввести invite link")) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("Введите код invite ссылки\n(это набор символов после /start в ссылке):");
            msg.setReplyMarkup(new ReplyKeyboardRemove(true));
            execute(msg);
            return;
        }

        // Пользователь ввел код - проверяем его
        String code = text.trim();

        // Если введена полная ссылка, извлекаем код
        if (code.contains("?start=")) {
            code = code.substring(code.indexOf("?start=") + 7);
        }

        // ✅ Получаем сохраненный телефон из сессии
        UserSession session = userSessionService.getOrCreateSession(chatId);
        String savedPhone = session.getRegistrationPhone();

        if (savedPhone == null) {
            // Что-то пошло не так, просим начать заново
            sendMessage(chatId, "❌ Ошибка: телефон не найден. Пожалуйста, начните заново: /start");
            userSessionService.clearSession(chatId);
            return;
        }

        // ✅ Валидируем код
        InviteLinkService.ValidationResult result = inviteLinkService.validateAndUse(code);

        if (!result.isValid()) {
            sendMessage(chatId, "❌ " + result.getErrorMessage() + "\n\nПопробуйте еще раз:");
            return;
        }

        InviteLink link = result.getInviteLink();

        // ✅ Создаем пользователя с сохраненным телефоном
        User newUser = new User();
        newUser.setTelegramId(chatId);
        newUser.setPhone(savedPhone);
        newUser.setFirstName(""); // Имя неизвестно, т.к. Contact не был передан повторно
        newUser.setLastName("");
        newUser.setBotStarted(true);
        newUser.setRole(link.getTargetRole());

        userService.save(newUser);

        // Привязываем квартиры
        for (Apartment apartment : link.getApartments()) {
            userApartmentService.linkUserToApartment(newUser, apartment);
        }

        userSessionService.clearSession(chatId);

        String roleText = link.getTargetRole() == UserRole.OWNER ? "владельца" : "арендатора";
        String apartmentInfo = link.getApartments().isEmpty() ?
                "" :
                String.format("\n\n📍 Привязанные квартиры: %d", link.getApartments().size());

        sendMessageWithRemoveKeyboard(chatId,
                "✅ Регистрация успешна!\n" +
                        "Вы зарегистрированы как " + roleText + "." + apartmentInfo);

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
        row1.add("🔗 Создать Invite");
        row1.add("📋 Мои Invite");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📊 Статистика");
        row2.add("⚙️ Настройки");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("ℹ️ Помощь");

        markup.setKeyboard(List.of(row1, row2, row3));

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("🔧 Меню администратора:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private void showOwnerMenu(Long chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🔗 Создать Invite");
        row1.add("📋 Мои Invite");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("📝 Create Request");
        row2.add("📋 My Request");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🔔 Notifications");
        row3.add("💬 Feedback");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("ℹ️ Help");

        markup.setKeyboard(List.of(row1, row2, row3, row4));

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("🏠 Меню владельца:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private void showTenantMenu(Long chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📝 Create Request");
        row1.add("📋 My Request");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔔 Notifications");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("💬 Feedback");
        row3.add("ℹ️ Help");

        markup.setKeyboard(List.of(row1, row2, row3));

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("👤 Меню арендатора:");
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    // ==========================
    // MENU COMMANDS
    // ==========================

    private void handleMenuCommand(Long chatId, String text) {
        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Произошла ошибка. Нажмите /start");
            return;
        }

        User user = userOpt.get();

        switch (text) {
            case "🔗 Создать Invite" -> handleCreateInvite(chatId, user);
            case "📋 Мои Invite" -> handleMyInvites(chatId, user);
            case "📝 Create Request" -> handleCreateRequest(chatId, user);  // ✅ НОВОЕ
            case "📋 My Request" -> handleMyRequests(chatId, user);  // ✅ НОВОЕ
            case "🔔 Notifications" -> handleNotifications(chatId);
            case "💬 Feedback" -> handleFeedback(chatId);
            case "ℹ️ Help", "ℹ️ Помощь" -> handleHelp(chatId);
            case "📊 Статистика" -> handleStatistics(chatId, user);
            case "⚙️ Настройки" -> handleSettings(chatId);
            default -> sendMessage(chatId, "Команда не распознана");
        }
    }

    // ==========================
    // CREATE INVITE LINK
    // ==========================

    private void handleCreateInvite(Long chatId, User user) {
        if (user.getRole() == UserRole.TENANT) {
            sendMessage(chatId, "❌ У вас нет прав для создания invite ссылок");
            return;
        }

        List<Apartment> apartments;
        if (user.getRole() == UserRole.ADMIN) {
            // Админ видит все квартиры
            apartments = apartmentService.getAll();
        } else {
            // Владельцы видят только свои квартиры
            apartments = userApartmentService.getApartmentsByUser(user);
            log.info("User {} role {}, apartments count: {}", user.getId(), user.getRole(), apartments.size());
        }

        if (apartments.isEmpty()) {
            sendMessage(chatId, "❌ Нет доступных квартир для создания ссылки");
            return;
        }

        userSessionService.clearApartmentSelection(chatId);

        // Определяем целевую роль для ссылки
        UserRole targetRole = user.getRole() == UserRole.ADMIN ? UserRole.OWNER : UserRole.TENANT;
        UserSession session = userSessionService.getOrCreateSession(chatId);
        session.setInviteTargetRole(targetRole);
        userSessionService.updateState(chatId, UserSessionState.CREATING_INVITE_LINK_SELECTING_APARTMENTS);

        // Если квартир меньше или равно 10 — показываем кнопки, иначе — предложим поиск по номеру
        if (apartments.size() <= 10) {
            showApartmentSelectionMenu(chatId, apartments);
        } else {
            sendApartmentSearchPrompt(chatId);
        }
    }

    private void showApartmentSelectionMenu(Long chatId, List<Apartment> apartments) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // максимум 10 квартир, по 2 в ряд
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

        // Кнопка "Готово"
        InlineKeyboardButton doneBtn = new InlineKeyboardButton();
        doneBtn.setText("✅ Готово");
        doneBtn.setCallbackData("APT_DONE");

        // Кнопка "Отмена"
        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Отмена");
        cancelBtn.setCallbackData("APT_CANCEL");

        rows.add(List.of(doneBtn, cancelBtn));

        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(
                "Выберите квартиры для этой invite ссылки:\n\n" +
                        "Нажимайте на квартиры, затем нажмите «Готово»\n\n" +
                        "💡 Чтобы отменить, нажмите «Отмена» или напишите /menu"
        );
        msg.setReplyMarkup(markup);

        execute(msg);
    }

    private InlineKeyboardButton createApartmentButton(Apartment apt) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("🏠 Дом " + apt.getHouseNumber() + ", Кв " + apt.getApartmentNumber());
        btn.setCallbackData("APT:" + apt.getId());
        return btn;
    }

    private void handleApartmentSelection(Update update) {

        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        // Нажали "Отмена"
        if (data.equals("APT_CANCEL")) {
            Optional<User> userOpt = userService.findByTelegramId(chatId);
            if (userOpt.isPresent()) {
                userSessionService.clearSession(chatId);
                sendMessageWithRemoveKeyboard(chatId, "❌ Создание invite ссылки отменено.");
                showMenuByRole(chatId, userOpt.get().getRole());
            }
            return;
        }

        // Нажали "Готово"
        if (data.equals("APT_DONE")) {

            List<Integer> selected = userSessionService.getSelectedApartments(chatId);

            if (selected.isEmpty()) {
                sendMessage(chatId, "❌ Выберите хотя бы одну квартиру!");
                return;
            }

            userSessionService.updateState(
                    chatId,
                    UserSessionState.CREATING_INVITE_LINK_USES
            );

            sendMessage(chatId,
                    "✅ Выбрано квартир: " + selected.size() + "\n\n" +
                            "Сколько раз можно использовать ссылку? (1-5)"
            );
            return;
        }

        // Нажали квартиру
        if (data.startsWith("APT:")) {

            Integer apartmentId =
                    Integer.parseInt(data.substring(4));

            userSessionService.addApartmentSelection(chatId, apartmentId);

            List<Integer> selected =
                    userSessionService.getSelectedApartments(chatId);

            sendMessage(chatId,
                    "🏠 Квартира добавлена!\n" +
                            "Выбрано: " + selected.size());
        }
    }

    private void handleInviteLinkUsesInput(Long chatId, String text) {
        try {
            int uses = Integer.parseInt(text.trim());

            if (uses < 1 || uses > 5) {
                sendMessage(chatId, "❌ Количество должно быть от 1 до 5. Попробуйте еще раз:");
                return;
            }

            // Сохраняем количество использований в БД
            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setInviteMaxUses(uses);

            userSessionService.updateState(chatId, UserSessionState.CREATING_INVITE_LINK_DAYS);
            sendMessage(chatId, "На сколько дней создать ссылку? (от 1 до 3)\n\n" +
                    "Введите число:\n\n" +
                    "💡 Чтобы отменить создание ссылки, напишите: /menu");

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Пожалуйста, введите число от 1 до 5:");
        }
    }

    private void handleApartmentSearch(Long chatId, String text) {
        // Проверяем, что текст реально число
        if (!text.matches("\\d+")) {
            // Просто игнорируем или показываем инструкцию
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
            sendMessage(chatId, "❌ Квартира с таким номером не найдена среди ваших квартир");
        } else if (matched.size() == 1) {
            userSessionService.addApartmentSelection(chatId, matched.get(0).getId());
            sendMessage(chatId, "🏠 Квартира добавлена: Дом " + matched.get(0).getHouseNumber() + ", Кв " + aptNumber);
        } else {
            StringBuilder sb = new StringBuilder("Найдено несколько квартир с таким номером:\n");
            for (Apartment apt : matched) {
                sb.append("🏠 Дом ").append(apt.getHouseNumber())
                        .append(" — ID: ").append(apt.getId()).append("\n");
            }
            sb.append("\nВведите ID квартиры для добавления:");
            sendMessage(chatId, sb.toString());
        }
    }

    private void handleInviteLinkDaysInput(Long chatId, String text) {
        try {
            int days = Integer.parseInt(text.trim());

            if (days < 1 || days > 3) {
                sendMessage(chatId, "❌ Количество дней должно быть от 1 до 3. Попробуйте еще раз:");
                return;
            }

            // Получаем данные из БД
            UserSession session = userSessionService.getOrCreateSession(chatId);
            session.setInviteExpirationDays(days);

            // Создаем ссылку
            Optional<User> userOpt = userService.findByTelegramId(chatId);
            if (userOpt.isEmpty()) {
                sendMessage(chatId, "Ошибка. Нажмите /start");
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
                String roleText = link.getTargetRole() == UserRole.OWNER ? "владельца" : "арендатора";

                sendMessage(chatId,
                        "✅ Invite ссылка создана!\n\n" +
                                "🔗 Ссылка: " + telegramLink + "\n" +
                                "👤 Роль: " + roleText + "\n" +
                                "🏠 Квартиры: " + link.getApartments().size() + "\n" +
                                "📊 Использований: 0/" + link.getMaxUses() + "\n" +
                                "⏰ Действует до: " + link.getExpiresAt().toLocalDate() + "\n\n" +
                                "Отправьте эту ссылку пользователю для регистрации.");

                userSessionService.clearSession(chatId);

                showMenuByRole(chatId, user.getRole());

            } catch (Exception e) {
                log.error("Ошибка создания invite link", e);
                sendMessage(chatId, "❌ Ошибка создания ссылки: " + e.getMessage());
                userSessionService.clearSession(chatId);
                showMenuByRole(chatId, user.getRole());
            }

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Пожалуйста, введите число от 1 до 3:");
        }
    }

    // ==========================
    // MY INVITES
    // ==========================

    private void handleMyInvites(Long chatId, User user) {

        List<InviteLink> links = inviteLinkService.getAllLinks(user);

        if (links.isEmpty()) {
            sendMessage(chatId, "У вас пока нет созданных invite ссылок.");
            return;
        }

        sendMessage(chatId, "📋 Ваши invite ссылки:");

        for (InviteLink link : links) {

            boolean valid = link.isValid();

            String apartments = link.getApartments().isEmpty()
                    ? "нет"
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
                    new InlineKeyboardButton("🗑 Удалить");
            deleteBtn.setCallbackData("invite_delete_" + link.getId());

            InlineKeyboardMarkup keyboard =
                    new InlineKeyboardMarkup(
                            List.of(List.of(deleteBtn))
                    );

            sendMessage(chatId, sb.toString(), keyboard);
        }
    }

    private String getInactiveReason(InviteLink link) {
        if (link.getExpiresAt().isBefore(LocalDateTime.now())) return "Истек срок действия";
        if (link.getUsedCount() >= link.getMaxUses()) return "Исчерпан лимит использований";
        return "Недоступна";
    }

    // ==========================
    // OTHER HANDLERS (заглушки)
    // ==========================

    private void handleInviteDelete(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        // Получаем id ссылки из callbackData
        Integer inviteId = Integer.parseInt(data.replace("invite_delete_", ""));

        Optional<InviteLink> linkOpt = inviteLinkService.getById(inviteId);

        if (linkOpt.isEmpty()) {
            // Ссылка уже удалена или не найдена
            editMessageText(chatId, messageId, "⚠ Эта ссылка уже недоступна.");
            return;
        }

        InviteLink link = linkOpt.get();

        // Удаляем ссылку
        inviteLinkService.delete(link);

        // Обновляем сообщение в боте, чтобы показать что ссылка удалена
        editMessageText(chatId, messageId, "✅ Invite ссылка удалена.");

        // Можно дополнительно обновить список всех ссылок пользователя
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
                "💬 Напишите ваш комментарий или предложение.\n\n" +
                        "Для отмены напишите /menu");
    }

    private void handleHelp(Long chatId) {
        sendMessage(chatId,
                "ℹ️ Справка по боту:\n\n" +
                        "Этот бот помогает управлять помещениями и арендаторами.\n\n" +
                        "Используйте меню для навигации.");
    }

    private void handleStatistics(Long chatId, User user) {
        InviteLinkService.LinkStatistics stats = inviteLinkService.getStatistics(user);
        sendMessage(chatId,
                "📊 Статистика:\n\n" +
                        "Активных ссылок: " + stats.activeLinks() + "\n" +
                        "Всего использований: " + stats.totalUsages());
    }

    private void handleSettings(Long chatId) {
        sendMessage(chatId, "⚙️ Настройки (в разработке)");
    }

    // ==========================
    // FEEDBACK
    // ==========================

    private void handleFeedbackInput(Long chatId, String text) {

        if (text.trim().length() < 5) {
            sendMessage(chatId, "❌ Сообщение слишком короткое. Напишите подробнее:");
            return;
        }

        Optional<User> userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Ошибка. Нажмите /start");
            return;
        }

        User user = userOpt.get();

        feedbackService.save(user, text);
        //todo maybe add notification for admin that new feedback was created

        userSessionService.clearSession(chatId);

        sendMessage(chatId,
                "✅ Спасибо! Ваш feedback сохранён и будет обработан в ближайшее время.");

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
                "🔔 Настройки уведомлений\n\n" +
                        "Нажмите на пункт чтобы включить/выключить:",
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
                "🔔 Настройки уведомлений\n\n" +
                        "Нажмите на пункт чтобы включить/выключить:"
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
                "У вас слишком много квартир для отображения кнопками.\n" +
                        "Пожалуйста, введите номер квартиры в чат, чтобы добавить её к invite ссылке.\n" +
                        "Например: 12");
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