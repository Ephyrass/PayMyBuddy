package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final ConnectionService connectionService;
    private final TransactionService transactionService;

    @Autowired
    public UserAccountController(UserAccountService userAccountService, ConnectionService connectionService, TransactionService transactionService) {
        this.userAccountService = userAccountService;
        this.connectionService = connectionService;
        this.transactionService = transactionService;
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
            return "redirect:/login?error=usernotfound";
        }

        UserAccount user = optionalUser.get();
        model.addAttribute("user", user);
        model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));
        model.addAttribute("transactions", transactionService.findByUser(user));

        return "dashboard";
    }

    /**
     * Displays the user's profile page.
     * @param model the model to add attributes to
     * @return the profile view name
     */
    @GetMapping("/profile")
    public String profilePage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        model.addAttribute("user", user);
        return "profile";
    }

    /**
     * Handles updating user profile information.
     * @param firstName the new first name
     * @param lastName the new last name
     * @param email the new email
     * @return redirect to profile page with success or error message
     */
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("firstName") String firstName,
                               @ModelAttribute("lastName") String lastName,
                               @ModelAttribute("email") String email) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            if (!user.getEmail().equals(email)) {
                Optional<UserAccount> existingUser = userAccountService.findByEmail(email);
                if (existingUser.isPresent()) {
                    return "redirect:/profile?error=email_exists";
                }
            }

            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            userAccountService.save(user);

            if (!auth.getName().equals(email)) {
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        email, auth.getCredentials(), auth.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }

            return "redirect:/profile?success";
        } catch (Exception e) {
            return "redirect:/profile?error=update_failed";
        }
    }

    /**
     * Handles updating user password.
     * @param currentPassword the current password
     * @param newPassword the new password
     * @param confirmPassword the password confirmation
     * @return redirect to profile page with success or error message
     */
    @PostMapping("/profile/change-password")
    public String changePassword(@ModelAttribute("currentPassword") String currentPassword,
                                @ModelAttribute("newPassword") String newPassword,
                                @ModelAttribute("confirmPassword") String confirmPassword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            if (!newPassword.equals(confirmPassword)) {
                return "redirect:/profile?error=password_mismatch";
            }

            if (!userAccountService.checkPassword(user, currentPassword)) {
                return "redirect:/profile?error=wrong_password";
            }

            user.setPassword(newPassword);
            userAccountService.save(user);

            return "redirect:/profile?success=password_changed";
        } catch (Exception e) {
            return "redirect:/profile?error=password_change_failed";
        }
    }

    /**
     * Retrieves all user accounts.
     * @return a list of all users
     */
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<UserAccount>> getAllUsers() {
        return ResponseEntity.ok(userAccountService.findAll());
    }

    /**
     * Retrieves a user account by its ID.
     * @param id the user ID
     * @return the user account or 404 if not found
     */
    @GetMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<UserAccount> getUserById(@PathVariable Long id) {
        return userAccountService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Registers a new user account.
     * @param user the user to register
     * @return the registered user or an error message
     */
    @PostMapping("/api/users/register")
    @ResponseBody
    public ResponseEntity<?> registerUser(@RequestBody UserAccount user) {
        try {
            UserAccount registeredUser = userAccountService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (DataIntegrityViolationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "A user with this email address already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An error occurred during registration: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Updates an existing user account.
     * @param id the user ID
     * @param user the user data to update
     * @return the updated user or 404 if not found
     */
    @PutMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserAccount user) {
        try {
            return userAccountService.findById(id)
                    .map(existingUser -> {
                        user.setId(id);
                        UserAccount updatedUser = userAccountService.save(user);
                        return ResponseEntity.ok(updatedUser);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (DataIntegrityViolationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "A user with this email address already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    /**
     * Deletes a user account by its ID.
     * @param id the user ID
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/api/users/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userAccountService.findById(id).isPresent()) {
            userAccountService.delete(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
