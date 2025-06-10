package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Billing;
import com.PayMyBuddy.service.BillingService;
import com.PayMyBuddy.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/billings")
public class BillingController {

    private final BillingService billingService;
    private final TransactionService transactionService;

    @Autowired
    public BillingController(BillingService billingService, TransactionService transactionService) {
        this.billingService = billingService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Billing>> getAllBillings() {
        return ResponseEntity.ok(billingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Billing> getBillingById(@PathVariable Long id) {
        return billingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/processed/{processed}")
    public ResponseEntity<List<Billing>> getBillingsByProcessedStatus(@PathVariable Boolean processed) {
        return ResponseEntity.ok(billingService.findByProcessed(processed));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<Billing>> getBillingsBetweenDates(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(billingService.findByDateBetween(start, end));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<Billing>> getBillingsByTransaction(@PathVariable Long transactionId) {
        return transactionService.findById(transactionId)
                .map(transaction -> ResponseEntity.ok(billingService.findByTransaction(transaction)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Billing> markBillingAsProcessed(@PathVariable Long id) {
        try {
            Billing processedBilling = billingService.markAsProcessed(id);
            return ResponseEntity.ok(processedBilling);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/process-all")
    public ResponseEntity<List<Billing>> processAllUnprocessedBillings() {
        List<Billing> processedBillings = billingService.processUnprocessedBillings();
        return ResponseEntity.ok(processedBillings);
    }
}
