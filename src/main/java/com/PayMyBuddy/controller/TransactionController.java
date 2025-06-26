package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Transaction;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserAccountService userAccountService;

    @Autowired
    public TransactionController(TransactionService transactionService, UserAccountService userAccountService) {
        this.transactionService = transactionService;
        this.userAccountService = userAccountService;
    }

    /**
     * Retrieves all transactions.
     * @return a list of all transactions
     */
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    /**
     * Retrieves a transaction by its ID.
     * @param id the transaction ID
     * @return the transaction or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all transactions for a specific user.
     * @param userId the user ID
     * @return a list of transactions for the user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionsByUser(@PathVariable Long userId) {
        Optional<UserAccount> user = userAccountService.findById(userId);
        return user.map(userAccount -> ResponseEntity.ok(transactionService.findByUser(userAccount))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all transactions sent by a specific user.
     * @param userId the user ID
     * @return a list of transactions sent by the user
     */
    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionsBySender(@PathVariable Long userId) {
        Optional<UserAccount> sender = userAccountService.findById(userId);
        return sender.map(userAccount -> ResponseEntity.ok(transactionService.findBySender(userAccount))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all transactions received by a specific user.
     * @param userId the user ID
     * @return a list of transactions received by the user
     */
    @GetMapping("/received/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionsByReceiver(@PathVariable Long userId) {
        Optional<UserAccount> receiver = userAccountService.findById(userId);
        return receiver.map(userAccount -> ResponseEntity.ok(transactionService.findByReceiver(userAccount))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Creates a new transaction and transfers the specified amount from the sender to the receiver.
     * @param payload the transaction details including senderId, receiverId, amount, and description
     * @return the created transaction or an error message
     */
    @PostMapping("/transfer")
    public ResponseEntity<?> makeTransaction(@RequestBody Map<String, Object> payload) {
        try {
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            String description = (String) payload.get("description");

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Le montant doit être supérieur à zéro");
            }

            Transaction transaction = transactionService.makeTransaction(senderId, receiverId, amount, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur est survenue: " + e.getMessage());
        }
    }

}
