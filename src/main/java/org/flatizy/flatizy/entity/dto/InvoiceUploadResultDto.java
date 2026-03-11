package org.flatizy.flatizy.entity.dto;

public record InvoiceUploadResultDto(
        String accountNumber,
        boolean sent,
        String result
) {}
