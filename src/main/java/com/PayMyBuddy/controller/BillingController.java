package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Billing;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.BillingService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.util.AuthenticationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class BillingController {

    private final BillingService billingService;
    private final TransactionService transactionService;
    private final AuthenticationUtils authenticationUtils;

    @Autowired
    public BillingController(BillingService billingService, TransactionService transactionService, AuthenticationUtils authenticationUtils) {
        this.billingService = billingService;
        this.transactionService = transactionService;
        this.authenticationUtils = authenticationUtils;
    }

    /**
     * Displays the billings page for the authenticated user.
     * @param model the model to add attributes to
     * @return the billings view name
     */
    @GetMapping("/billings")
    public String billingsPage(Model model) {
        UserAccount user = authenticationUtils.getCurrentUser();

        // Get all billings (for now, we'll show all billings)
        List<Billing> allBillings = billingService.findAll();

        model.addAttribute("user", user);
        model.addAttribute("billings", allBillings);

        return "billings";
    }

    /**
     * Handles marking a billing as processed.
     * @param id the billing ID
     * @return redirect to billings page with success or error message
     */
    @PostMapping("/billings/{id}/process")
    public String markBillingAsProcessed(@PathVariable Long id) {
        try {
            billingService.markAsProcessed(id);
            return "redirect:/billings?success=processed";
        } catch (IllegalArgumentException e) {
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/billings?error=" + errorMessage;
        }
    }
}
