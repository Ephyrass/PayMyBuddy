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

    /**
     * Retrieves all billings.
     * @return a list of all billings
     */
    @GetMapping
    public ResponseEntity<List<Billing>> getAllBillings() {
        return ResponseEntity.ok(billingService.findAll());
    }

    /**
     * Retrieves a billing by its ID.
     * @param id the billing ID
     * @return the billing or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Billing> getBillingById(@PathVariable Long id) {
        return billingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves billings by processed status.
     * @param processed the processed status
     * @return a list of billings with the given processed status
     */
    @GetMapping("/processed/{processed}")
    public ResponseEntity<List<Billing>> getBillingsByProcessedStatus(@PathVariable Boolean processed) {
        return ResponseEntity.ok(billingService.findByProcessed(processed));
    }

    /**
     * Retrieves billings between two dates.
     * @param start the start date
     * @param end the end date
     * @return a list of billings between the given dates
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<Billing>> getBillingsBetweenDates(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(billingService.findByDateBetween(start, end));
    }

    /**
     * Retrieves billings by transaction ID.
     * @param transactionId the transaction ID
     * @return a list of billings for the given transaction or 404 if transaction not found
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<Billing>> getBillingsByTransaction(@PathVariable Long transactionId) {
        return transactionService.findById(transactionId)
                .map(transaction -> ResponseEntity.ok(billingService.findByTransaction(transaction)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Marks a billing as processed.
     * @param id the billing ID
     * @return the processed billing or 404 if not found
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<Billing> markBillingAsProcessed(@PathVariable Long id) {
        try {
            Billing processedBilling = billingService.markAsProcessed(id);
            return ResponseEntity.ok(processedBilling);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Processes all unprocessed billings.
     * @return a list of processed billings
     */
    @PostMapping("/process-all")
    public ResponseEntity<List<Billing>> processAllUnprocessedBillings() {
        List<Billing> processedBillings = billingService.processUnprocessedBillings();
        return ResponseEntity.ok(processedBillings);
    }
}
