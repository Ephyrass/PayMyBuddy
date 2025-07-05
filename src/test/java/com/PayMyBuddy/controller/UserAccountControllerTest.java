package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import com.PayMyBuddy.util.AuthenticationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAccountControllerTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private Model model;

    @Mock
    private AuthenticationUtils authenticationUtils;

    @InjectMocks
    private UserAccountController userAccountController;

    private MockMvc mockMvc;
    private UserAccount testUser;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userAccountController).build();

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Mock SecurityContext et Authentication
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.setContext(securityContext);

        // Mock AuthenticationUtils
        when(authenticationUtils.getCurrentUser()).thenReturn(testUser);
        when(authenticationUtils.getCurrentUserEmail()).thenReturn(testUser.getEmail());
    }

    // Tests pour les pages d'authentification (nouvelles fonctionnalités)

    @Test
    void index_shouldReturnIndex_whenUserNotAuthenticated() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(false);

        // Act
        String result = userAccountController.index();

        // Assert
        assertEquals("index", result);
    }

    @Test
    void index_shouldRedirectToDashboard_whenUserAuthenticated() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");

        // Act
        String result = userAccountController.index();

        // Assert
        assertEquals("redirect:/dashboard", result);
    }

    @Test
    void loginPage_shouldReturnLoginView() {
        // Act
        String result = userAccountController.loginPage();

        // Assert
        assertEquals("login", result);
    }

    @Test
    void registerPage_shouldReturnRegisterView() {
        // Act
        String result = userAccountController.registerPage(model);

        // Assert
        assertEquals("register", result);
        verify(model).addAttribute(eq("user"), any(UserAccount.class));
    }

    @Test
    void registerUser_shouldRedirectToDashboard_whenRegistrationSuccessful() {
        // Arrange
        UserAccount newUser = new UserAccount();
        newUser.setEmail("newuser@example.com");
        when(userAccountService.register(newUser)).thenReturn(newUser);

        // Act
        String result = userAccountController.registerUser(newUser);

        // Assert
        assertEquals("redirect:/dashboard", result);
        verify(userAccountService).register(newUser);
    }

    // Tests pour les pages web existantes

    @Test
    void dashboard_shouldReturnDashboardView_whenUserAuthenticated() {
        // Arrange
        when(connectionService.findByOwnerId(1L)).thenReturn(Arrays.asList());
        when(transactionService.findByUser(testUser)).thenReturn(Arrays.asList());

        // Act
        String result = userAccountController.dashboard(model);

        // Assert
        assertEquals("dashboard", result);
        verify(model).addAttribute("user", testUser);
        verify(model).addAttribute(eq("connections"), any());
        verify(model).addAttribute(eq("transactions"), any());
    }

    @Test
    void dashboard_shouldRedirectToLogin_whenUserNotAuthenticated() {
        // Arrange
        when(authenticationUtils.getCurrentUser()).thenThrow(new IllegalArgumentException("User not authenticated"));

        // Act
        String result = userAccountController.dashboard(model);

        // Assert
        assertEquals("redirect:/login?error=usernotfound", result);
    }

    @Test
    void profilePage_shouldReturnProfileView() {
        // Act
        String result = userAccountController.profilePage(model);

        // Assert
        assertEquals("profile", result);
        verify(model).addAttribute("user", testUser);
    }

    @Test
    void updateProfile_shouldRedirectWithSuccess_whenUpdateSuccessful() {
        // Arrange
        when(userAccountService.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
        when(userAccountService.save(any(UserAccount.class))).thenReturn(testUser);

        // Act
        String result = userAccountController.updateProfile("NewFirst", "NewLast", "newemail@example.com");

        // Assert
        assertEquals("redirect:/profile?success", result);
        verify(userAccountService).save(testUser);
    }

    @Test
    void updateProfile_shouldRedirectWithError_whenEmailAlreadyExists() {
        // Arrange
        UserAccount existingUser = new UserAccount();
        existingUser.setId(2L);
        existingUser.setEmail("newemail@example.com");
        when(userAccountService.findByEmail("newemail@example.com")).thenReturn(Optional.of(existingUser));

        // Act
        String result = userAccountController.updateProfile("NewFirst", "NewLast", "newemail@example.com");

        // Assert
        assertEquals("redirect:/profile?error=email_exists", result);
        verify(userAccountService, never()).save(any(UserAccount.class));
    }

    @Test
    void changePassword_shouldRedirectWithSuccess_whenPasswordChangeSuccessful() {
        // Arrange
        when(userAccountService.checkPassword(testUser, "currentPassword")).thenReturn(true);
        when(userAccountService.save(any(UserAccount.class))).thenReturn(testUser);

        // Act
        String result = userAccountController.changePassword("currentPassword", "newPassword", "newPassword");

        // Assert
        assertEquals("redirect:/profile?success=password_changed", result);
        verify(userAccountService).save(testUser);
    }

    @Test
    void changePassword_shouldRedirectWithError_whenPasswordsDontMatch() {
        // Act
        String result = userAccountController.changePassword("currentPassword", "newPassword", "differentPassword");

        // Assert
        assertEquals("redirect:/profile?error=password_mismatch", result);
        verify(userAccountService, never()).save(any(UserAccount.class));
    }

    @Test
    void changePassword_shouldRedirectWithError_whenCurrentPasswordIsWrong() {
        // Arrange
        when(userAccountService.checkPassword(testUser, "wrongPassword")).thenReturn(false);

        // Act
        String result = userAccountController.changePassword("wrongPassword", "newPassword", "newPassword");

        // Assert
        assertEquals("redirect:/profile?error=wrong_password", result);
        verify(userAccountService, never()).save(any(UserAccount.class));
    }
}
