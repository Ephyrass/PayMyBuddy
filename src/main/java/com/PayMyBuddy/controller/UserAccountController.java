package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import com.PayMyBuddy.util.AuthenticationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final ConnectionService connectionService;
    private final TransactionService transactionService;
    private final AuthenticationUtils authenticationUtils;

    @Autowired
    public UserAccountController(UserAccountService userAccountService, ConnectionService connectionService,
                                TransactionService transactionService, AuthenticationUtils authenticationUtils) {
        this.userAccountService = userAccountService;
        this.connectionService = connectionService;
        this.transactionService = transactionService;
        this.authenticationUtils = authenticationUtils;
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
     * Handles user registration.
     * @param user the user to register
     * @return redirect to dashboard after registration
     */
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") UserAccount user) {
        userAccountService.register(user);
        return "redirect:/dashboard";
    }

    /**
     * Displays the dashboard for the authenticated user.
     * @param model the model to add attributes to
     * @return the dashboard view name or redirect to login if not authenticated
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            UserAccount user = authenticationUtils.getCurrentUser();
            model.addAttribute("user", user);
            model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));
            model.addAttribute("transactions", transactionService.findByUser(user));
            return "dashboard";
        } catch (IllegalArgumentException e) {
            return "redirect:/login?error=usernotfound";
        }
    }

    /**
     * Displays the user's profile page.
     * @param model the model to add attributes to
     * @return the profile view name
     */
    @GetMapping("/profile")
    public String profilePage(Model model) {
        UserAccount user = authenticationUtils.getCurrentUser();
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
        UserAccount user = authenticationUtils.getCurrentUser();

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

            if (!authenticationUtils.getCurrentUserEmail().equals(email)) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
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
        UserAccount user = authenticationUtils.getCurrentUser();

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
}
