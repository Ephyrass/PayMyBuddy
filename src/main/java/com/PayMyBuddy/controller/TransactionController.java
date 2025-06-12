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

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionsByUser(@PathVariable Long userId) {
        Optional<UserAccount> user = userAccountService.findById(userId);
        if (user.isPresent()) {
            return ResponseEntity.ok(transactionService.findByUser(user.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/sent/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionsBySender(@PathVariable Long userId) {
        Optional<UserAccount> sender = userAccountService.findById(userId);
        if (sender.isPresent()) {
            return ResponseEntity.ok(transactionService.findBySender(sender.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/received/{userId}")
    public ResponseEntity<List<Transaction>> getTransactionsByReceiver(@PathVariable Long userId) {
        Optional<UserAccount> receiver = userAccountService.findById(userId);
        if (receiver.isPresent()) {
            return ResponseEntity.ok(transactionService.findByReceiver(receiver.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

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
