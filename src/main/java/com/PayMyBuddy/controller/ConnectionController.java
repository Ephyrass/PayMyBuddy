package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.UserAccountService;
import com.PayMyBuddy.util.AuthenticationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ConnectionController {

    private final ConnectionService connectionService;
    private final UserAccountService userAccountService;
    private final AuthenticationUtils authenticationUtils;

    @Autowired
    public ConnectionController(ConnectionService connectionService, UserAccountService userAccountService,
                               AuthenticationUtils authenticationUtils) {
        this.connectionService = connectionService;
        this.userAccountService = userAccountService;
        this.authenticationUtils = authenticationUtils;
    }

    /**
     * Displays the user's connections (contacts) page.
     *
     * @param model the model to add attributes to
     * @return the connections view name
     */
    @GetMapping("/connections")
    public String connectionsPage(Model model) {
        UserAccount user = authenticationUtils.getCurrentUser();

        model.addAttribute("user", user);
        model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));

        return "connections";
    }

    /**
     * Handles adding a new connection (contact) for the authenticated user.
     *
     * @param friendEmail the email of the contact to add
     * @return redirect to connections page with success or error message
     */
    @PostMapping("/connections/add")
    public String addConnection(@ModelAttribute("email") String friendEmail) {
        UserAccount user = authenticationUtils.getCurrentUser();

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
     * Handles deleting a connection (contact) for the authenticated user.
     *
     * @param id the ID of the connection to delete
     * @return redirect to connections page with success or error message
     */
    @PostMapping("/connections/delete/{id}")
    public String deleteConnection(@PathVariable Long id) {
        UserAccount user = authenticationUtils.getCurrentUser();

        try {
            connectionService.findById(id).ifPresentOrElse(connection -> {
                if (!connection.getOwner().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("You are not authorized to delete this connection");
                }
                connectionService.deleteConnection(id);
            }, () -> {
                throw new IllegalArgumentException("Connection not found");
            });

            return "redirect:/connections?success_delete";
        } catch (IllegalArgumentException e) {
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/connections?error=" + errorMessage;
        }
    }
}
