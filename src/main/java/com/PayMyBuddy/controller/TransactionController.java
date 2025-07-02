package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Transaction;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Controller
public class TransactionController {

    private final TransactionService transactionService;
    private final UserAccountService userAccountService;
    private final ConnectionService connectionService;

    @Autowired
    public TransactionController(TransactionService transactionService, UserAccountService userAccountService, ConnectionService connectionService) {
        this.transactionService = transactionService;
        this.userAccountService = userAccountService;
        this.connectionService = connectionService;
    }

    /**
     * Displays the transactions page, optionally pre-selecting a contact.
     * @param contactId the ID of the contact to pre-select (optional)
     * @param model the model to add attributes to
     * @return the transactions view name
     */
    @GetMapping("/transactions")
    public String transactionsPage(@RequestParam(value = "contactId", required = false) Long contactId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));
        model.addAttribute("transactions", transactionService.findByUser(user));
        if (contactId != null) {
            model.addAttribute("contactId", contactId);
        }
        return "transactions";
    }

    /**
     * Handles sending money to a contact.
     * @param receiverId the ID of the contact to send money to
     * @param amount the amount to send
     * @param description the description of the transaction
     * @return redirect to transactions page with success or error message
     */
    @PostMapping("/transactions/send")
    public String sendMoney(@ModelAttribute("receiverId") Long receiverId,
                           @ModelAttribute("amount") Double amount,
                           @ModelAttribute("description") String description) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            transactionService.makeTransaction(user.getId(), receiverId, new BigDecimal(amount), description);
            return "redirect:/transactions?success";
        } catch (IllegalArgumentException e) {
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/transactions?error=" + errorMessage;
        }
    }

    /**
     * Retrieves all transactions.
     * @return a list of all transactions
     */
    @GetMapping("/api/transactions")
    @ResponseBody
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    /**
     * Retrieves a transaction by its ID.
     * @param id the transaction ID
     * @return the transaction or 404 if not found
     */
    @GetMapping("/api/transactions/{id}")
    @ResponseBody
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all transactions sent by a specific user.
     * @param userId the user ID
     * @return a list of transactions sent by the user
     */
    @GetMapping("/api/transactions/sent/{userId}")
    @ResponseBody
    public ResponseEntity<List<Transaction>> getTransactionsBySender(@PathVariable Long userId) {
        return userAccountService.findById(userId)
                .map(user -> ResponseEntity.ok(transactionService.findBySender(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all transactions received by a specific user.
     * @param userId the user ID
     * @return a list of transactions received by the user
     */
    @GetMapping("/api/transactions/received/{userId}")
    @ResponseBody
    public ResponseEntity<List<Transaction>> getTransactionsByReceiver(@PathVariable Long userId) {
        return userAccountService.findById(userId)
                .map(user -> ResponseEntity.ok(transactionService.findByReceiver(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new transaction and transfers the specified amount from the sender to the receiver.
     * @param payload the transaction details including senderId, receiverId, amount, and description
     * @return the created transaction or an error message
     */
    @PostMapping("/api/transactions/transfer")
    @ResponseBody
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
