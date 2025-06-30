package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Connection;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ConnectionController {

    private final ConnectionService connectionService;
    private final UserAccountService userAccountService;

    @Autowired
    public ConnectionController(ConnectionService connectionService, UserAccountService userAccountService) {
        this.connectionService = connectionService;
        this.userAccountService = userAccountService;
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

    /**
     * Retrieves all connections.
     * @return a list of all connections
     */
    @GetMapping("/api/connections")
    @ResponseBody
    public ResponseEntity<List<Connection>> getAllConnections() {
        return ResponseEntity.ok(connectionService.findAll());
    }

    /**
     * Retrieves all connections for a specific user.
     * @param userId the user ID
     * @return a list of connections for the user
     */
    @GetMapping("/api/connections/user/{userId}")
    @ResponseBody
    public ResponseEntity<List<Connection>> getConnectionsByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(connectionService.findByOwnerId(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves a connection by its ID.
     * @param id the connection ID
     * @return the connection or 404 if not found
     */
    @GetMapping("/api/connections/{id}")
    @ResponseBody
    public ResponseEntity<Connection> getConnectionById(@PathVariable Long id) {
        return connectionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new connection between two users.
     * @param payload a map containing ownerId and friendId
     * @return the created connection or an error message
     */
    @PostMapping("/api/connections")
    @ResponseBody
    public ResponseEntity<Connection> createConnection(@RequestBody Map<String, Long> payload) {
        try {
            Long ownerId = payload.get("ownerId");
            Long friendId = payload.get("friendId");

            if (ownerId == null || friendId == null) {
                return ResponseEntity.badRequest().build();
            }

            Connection connection = connectionService.createConnection(ownerId, friendId);
            return ResponseEntity.status(HttpStatus.CREATED).body(connection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Deletes a connection by its ID.
     * @param id the connection ID
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/api/connections/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteConnectionApi(@PathVariable Long id) {
        try {
            connectionService.deleteConnection(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes a connection between two users.
     * @param payload a map containing ownerId and friendId
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/api/connections")
    @ResponseBody
    public ResponseEntity<Void> deleteConnectionBetweenUsers(@RequestBody Map<String, Long> payload) {
        try {
            Long ownerId = payload.get("ownerId");
            Long friendId = payload.get("friendId");

            if (ownerId == null || friendId == null) {
                return ResponseEntity.badRequest().build();
            }

            connectionService.deleteConnectionBetweenUsers(ownerId, friendId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
