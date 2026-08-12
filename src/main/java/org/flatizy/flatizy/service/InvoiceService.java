package org.flatizy.flatizy.service;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.config.TelegramBot;
import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.User;
import org.flatizy.flatizy.entity.dto.InvoiceUploadResultDto;
import org.flatizy.flatizy.entity.enums.AccountStatus;
import org.flatizy.flatizy.entity.enums.UserRole;
import org.flatizy.flatizy.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class InvoiceService {

    private final AccountRepository accountRepository;
    private final FileService fileService;
    private final TelegramBot telegramBot;

    public InvoiceService(AccountRepository accountRepository,
                          FileService fileService,
                          TelegramBot telegramBot) {
        this.accountRepository = accountRepository;
        this.fileService = fileService;
        this.telegramBot = telegramBot;
    }

    public List<InvoiceUploadResultDto> processZip(MultipartFile zipFile) {
        if (zipFile.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        log.info("Processing ZIP: {}", zipFile.getOriginalFilename());

        List<InvoiceUploadResultDto> results = new ArrayList<>();

        File tempDir;
        try {
            tempDir = Files.createTempDirectory("invoices").toFile();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create temp dir", e);
        }

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {

            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                if (entry.isDirectory()) continue;
                if (!entry.getName().toLowerCase().endsWith(".pdf")) continue;

                File newFile = new File(tempDir, entry.getName());

                newFile.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    zis.transferTo(fos);
                }

                results.add(processSingleFile(newFile));
            }

        } catch (Exception e) {
            log.error("ZIP processing error", e);
            throw new RuntimeException("ZIP processing failed");
        }

        return results;
    }

    private InvoiceUploadResultDto processSingleFile(File file) {
        String accountNumber = "";
        try {
            String text = fileService.parsePdfFile(file);

            accountNumber = extractAccountNumber(text);
            if (accountNumber == null) {
                return fail(null, "ACCOUNT_NUMBER_NOT_FOUND");
            }

            Account account = accountRepository
                    .findByAccountNumber(accountNumber)
                    .orElse(null);

            if (account == null) {
                return fail(accountNumber, "ACCOUNT_NOT_FOUND");
            }

            if (account.getStatus() != AccountStatus.COMPLETED) {
                return fail(accountNumber, "ACCOUNT_STATUS_NOT_COMPLETED");
            }

            sendInvoice(account, file);

            return success(accountNumber);

        } catch (Exception e) {
            log.error("Error processing file {}", file.getName(), e);
            return fail(accountNumber, "PROCESSING_ERROR");
        }
    }

    private void sendInvoice(Account account, File file) {
        User user = account.getUser();

        if (user.getRole() != UserRole.OWNER) {
            log.info("Invoice {} not sent to user {} - only OWNER role can receive invoices",
                    file.getName(), user.getId());
            return;
        }

        if (user.getTelegramId() == null) {
            log.warn("User {} has no Telegram ID, cannot send invoice {}", user.getId(), file.getName());
            return;
        }

        try {
            String text = fileService.parsePdfFile(file);
            String monthYear = extractMonthYear(text);

            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(user.getTelegramId().toString());
            sendDocument.setDocument(new InputFile(file));
            sendDocument.setCaption("💰 Квитанція на оплату\n" +
                    (monthYear != null ? monthYear : ""));

            telegramBot.execute(sendDocument);
            log.info("Invoice {} sent to user {} (chatId: {})",
                    file.getName(), user.getId(), user.getTelegramId());

        } catch (TelegramApiException e) {
            log.error("Failed to send invoice {} to user {}: {}",
                    file.getName(), user.getId(), e.getMessage(), e);
        }
    }

    private String extractMonthYear(String text) {
        Pattern pattern = Pattern.compile("([А-Яа-яїєі]+)\\s+(\\d{4})\\s*р", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2) + " р.";
        }
        return null;
    }

    private String extractAccountNumber(String text) {
        Pattern pattern = Pattern.compile("рахунку\\s*\\(?([A-Za-z0-9\\-]+)\\)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private InvoiceUploadResultDto success(String acc) {
        return new InvoiceUploadResultDto(acc, true, "SENT");
    }

    private InvoiceUploadResultDto fail(String acc, String result) {
        return new InvoiceUploadResultDto(acc, false, result);
    }
}