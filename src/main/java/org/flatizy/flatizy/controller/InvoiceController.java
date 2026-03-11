package org.flatizy.flatizy.controller;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.dto.InvoiceUploadResultDto;
import org.flatizy.flatizy.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/process")
    public ResponseEntity<List<InvoiceUploadResultDto>> processInvoices(
            @RequestParam("file") MultipartFile file
    ) {
        List<InvoiceUploadResultDto> result =
                invoiceService.processZip(file);

        return ResponseEntity.ok(result);
    }
}
