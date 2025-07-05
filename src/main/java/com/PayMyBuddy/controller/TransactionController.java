package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.util.AuthenticationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
public class TransactionController {

    private final TransactionService transactionService;
    private final ConnectionService connectionService;
    private final AuthenticationUtils authenticationUtils;

    @Autowired
    public TransactionController(TransactionService transactionService, ConnectionService connectionService, AuthenticationUtils authenticationUtils) {
        this.transactionService = transactionService;
        this.connectionService = connectionService;
        this.authenticationUtils = authenticationUtils;
    }

    /**
     * Displays the transactions page, optionally pre-selecting a contact.
     * @param contactId the ID of the contact to pre-select (optional)
     * @param model the model to add attributes to
     * @return the transactions view name
     */
    @GetMapping("/transactions")
    public String transactionsPage(@RequestParam(value = "contactId", required = false) Long contactId, Model model) {
        UserAccount user = authenticationUtils.getCurrentUser();

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
        UserAccount user = authenticationUtils.getCurrentUser();

        try {
            transactionService.makeTransaction(user.getId(), receiverId, new BigDecimal(amount), description);
            return "redirect:/transactions?success";
        } catch (IllegalArgumentException e) {
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/transactions?error=" + errorMessage;
        }
    }
}
