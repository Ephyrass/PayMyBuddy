package com.PayMyBuddy.util;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationUtils {

    private final UserAccountService userAccountService;

    public AuthenticationUtils(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /**
     * Get the currently authenticated user
     * @return the authenticated user
     * @throws IllegalArgumentException if the user is not found
     */
    public UserAccount getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName().equals("anonymousUser")) {
            throw new IllegalArgumentException("User not authenticated");
        }

        return userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }


    /**
     * Get the email of the authenticated user
     * @return the email of the authenticated user
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName().equals("anonymousUser")) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return auth.getName();
    }
}
