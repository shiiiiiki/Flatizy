package org.flatizy.flatizy.service;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.Account;
import org.flatizy.flatizy.entity.dto.InvoiceUploadResultDto;
import org.flatizy.flatizy.entity.enums.AccountStatus;
import org.flatizy.flatizy.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class InvoiceService {

    private final AccountRepository accountRepository;
    private final FileService fileService;

    public InvoiceService(AccountRepository accountRepository,
                          FileService fileService) {
        this.accountRepository = accountRepository;
        this.fileService = fileService;
    }

    public List<InvoiceUploadResultDto> processFolder(Map<String, String> request) {
        List<InvoiceUploadResultDto> results = new ArrayList<>();

        String folderPath = request.get("folderPath");
        fileService.processFiles(folderPath, file ->
                results.add(processSingleFile(file))
        );

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
        log.info("Invoice {} sent to account {}",
                file.getName(), account.getAccountNumber());
    }

    private String extractAccountNumber(String text) {
        return extractValueAfter(text, "рахунку");
    }

    private String extractValueAfter(String text, String key) {
        int index = text.toLowerCase().indexOf(key.toLowerCase());
        if (index == -1) return null;

        String after = text.substring(index + key.length()).trim();
        return after.split("\\s+")[0];
    }

    private InvoiceUploadResultDto success(String acc) {
        return new InvoiceUploadResultDto(acc, true, "SENT");
    }

    private InvoiceUploadResultDto fail(String acc, String reason) {
        return new InvoiceUploadResultDto(acc, false, reason);
    }
}