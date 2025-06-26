package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Collections;
import java.util.Optional;

/**
 * WebController handles all web page requests for the PayMyBuddy application.
 * It manages user authentication, registration, dashboard, connections, and transactions views.
 */
@Controller
public class WebController {

    private final UserAccountService userAccountService;
    private final ConnectionService connectionService;
    private final TransactionService transactionService;

    public WebController(UserAccountService userAccountService,
                        ConnectionService connectionService,
                        TransactionService transactionService) {
        this.userAccountService = userAccountService;
        this.connectionService = connectionService;
        this.transactionService = transactionService;
    }

    /**
     * Displays the home page or redirects authenticated users to the dashboard.
     * @return the name of the view to display
     */
    @GetMapping("/")
    public String index() {
        // If the user is already authenticated, redirect them to the dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        // Otherwise, display the home page
        return "index";
    }

    /**
     * Displays the login page.
     * @return the login view name
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * Displays the registration page.
     * @param model the model to add attributes to
     * @return the register view name
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserAccount());
        return "register";
    }

    /**
     * Handles user registration and authenticates the new user.
     * @param user the user to register
     * @return redirect to dashboard after registration
     */
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") UserAccount user) {
        userAccountService.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), user.getPassword(),
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return "redirect:/dashboard";
    }

    /**
     * Displays the dashboard for the authenticated user.
     * @param model the model to add attributes to
     * @return the dashboard view name or redirect to login if not authenticated
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is authenticated but is not the anonymous user
        if (auth == null || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        // Find the user by email and redirect if not found
        Optional<UserAccount> optionalUser = userAccountService.findByEmail(auth.getName());
        if (optionalUser.isEmpty()) {
            // The email of the authenticated user does not exist in the database
            return "redirect:/login?error=usernotfound";
        }

        UserAccount user = optionalUser.get();
        model.addAttribute("user", user);
        model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));
        model.addAttribute("transactions", transactionService.findByUser(user));

        return "dashboard";
    }

    /**
     * Displays the user's connections (contacts) page.
     * @param model the model to add attributes to
     * @return the connections view name
     */
    @GetMapping("/connections")
    public String connectionsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));

        return "connections";
    }

    /**
     * Displays the transactions page, optionally pre-selecting a contact.
     * @param contactId the ID of the contact to pre-select (optional)
     * @param model the model to add attributes to
     * @return the transactions view name
     */
    @GetMapping("/transactions")
    public String transactionsPage(@org.springframework.web.bind.annotation.RequestParam(value = "contactId", required = false) Long contactId, Model model) {
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
     * Handles adding a new connection (contact) for the authenticated user.
     * @param friendEmail the email of the contact to add
     * @return redirect to connections page with success or error message
     */
    @PostMapping("/connections/add")
    public String addConnection(@ModelAttribute("email") String friendEmail) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if the user is trying to add their own email address
        if (user.getEmail().equals(friendEmail)) {
            return "redirect:/connections?error=self_connection";
        }

        try {
            UserAccount friend = userAccountService.findByEmail(friendEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Contact not found"));

            connectionService.createConnection(user.getId(), friend.getId());
            return "redirect:/connections?success";
        } catch (IllegalArgumentException e) {
            // Encode the error message for the URL
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/connections?error=" + errorMessage;
        }
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
            transactionService.makeTransaction(user.getId(), receiverId, new java.math.BigDecimal(amount), description);
            return "redirect:/transactions?success";
        } catch (IllegalArgumentException e) {
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/transactions?error=" + errorMessage;
        }
    }

    /**
     * Handles deleting a connection (contact) for the authenticated user.
     * @param id the ID of the connection to delete
     * @return redirect to connections page with success or error message
     */
    @PostMapping("/connections/delete/{id}")
    public String deleteConnection(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            // Check if the connection exists
            if (connectionService.findById(id).isEmpty()) {
                return "redirect:/connections?error=connection_not_found";
            }

            // Check if the connection belongs to the current user
            connectionService.findById(id).ifPresent(connection -> {
                if (!connection.getOwner().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("You are not authorized to delete this connection");
                }
                connectionService.deleteConnection(id);
            });

            return "redirect:/connections?success_delete";
        } catch (IllegalArgumentException e) {
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/connections?error=" + errorMessage;
        }
    }
}
