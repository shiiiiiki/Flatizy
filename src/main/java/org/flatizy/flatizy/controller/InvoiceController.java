package org.flatizy.flatizy.controller;

import lombok.extern.slf4j.Slf4j;
import org.flatizy.flatizy.entity.dto.InvoiceUploadResultDto;
import org.flatizy.flatizy.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
            @RequestBody Map<String, String> request
    ) {
        List<InvoiceUploadResultDto> result =
                invoiceService.processFolder(request);

        return ResponseEntity.ok(result);
    }
}
