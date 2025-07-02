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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        SecurityContextHolder.setContext(securityContext);

        // Mock AuthenticationUtils
        when(authenticationUtils.getCurrentUser()).thenReturn(testUser);
        when(authenticationUtils.getCurrentUserEmail()).thenReturn(testUser.getEmail());
    }

    // Tests pour les pages web

    @Test
    void dashboard_shouldReturnDashboardView_whenUserAuthenticated() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
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
    void dashboard_shouldRedirectToLogin_whenUserNotFound() {
        // Arrange
        when(authenticationUtils.getCurrentUser()).thenThrow(new IllegalArgumentException("User not found"));

        // Act
        String result = userAccountController.dashboard(model);

        // Assert
        assertEquals("redirect:/login?error=usernotfound", result);
    }

    @Test
    void profilePage_shouldReturnProfileView() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        String result = userAccountController.profilePage(model);

        // Assert
        assertEquals("profile", result);
        verify(model).addAttribute("user", testUser);
    }

    @Test
    void updateProfile_shouldRedirectWithSuccess_whenUpdateSuccessful() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
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

        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userAccountService.findByEmail("newemail@example.com")).thenReturn(Optional.of(existingUser));

        // Act
        String result = userAccountController.updateProfile("NewFirst", "NewLast", "newemail@example.com");

        // Assert
        assertEquals("redirect:/profile?error=email_exists", result);
        verify(userAccountService, never()).save(any());
    }

    @Test
    void changePassword_shouldRedirectWithSuccess_whenPasswordChangeSuccessful() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userAccountService.checkPassword(testUser, "currentPassword")).thenReturn(true);
        when(userAccountService.save(any(UserAccount.class))).thenReturn(testUser);

        // Act
        String result = userAccountController.changePassword("currentPassword", "newPassword", "newPassword");

        // Assert
        assertEquals("redirect:/profile?success=password_changed", result);
        verify(userAccountService).save(testUser);
    }

    @Test
    void changePassword_shouldRedirectWithError_whenPasswordMismatch() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        String result = userAccountController.changePassword("currentPassword", "newPassword", "differentPassword");

        // Assert
        assertEquals("redirect:/profile?error=password_mismatch", result);
        verify(userAccountService, never()).save(any());
    }

    @Test
    void changePassword_shouldRedirectWithError_whenCurrentPasswordWrong() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userAccountService.checkPassword(testUser, "wrongPassword")).thenReturn(false);

        // Act
        String result = userAccountController.changePassword("wrongPassword", "newPassword", "newPassword");

        // Assert
        assertEquals("redirect:/profile?error=wrong_password", result);
        verify(userAccountService, never()).save(any());
    }

    // Tests pour l'API

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        // Arrange
        List<UserAccount> expectedUsers = Arrays.asList(testUser);
        when(userAccountService.findAll()).thenReturn(expectedUsers);

        // Act
        ResponseEntity<List<UserAccount>> response = userAccountController.getAllUsers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedUsers, response.getBody());
    }

    @Test
    void getUserById_shouldReturnUser_whenUserExists() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<UserAccount> response = userAccountController.getUserById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser, response.getBody());
    }

    @Test
    void getUserById_shouldReturnNotFound_whenUserDoesNotExist() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserAccount> response = userAccountController.getUserById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void registerUser_shouldReturnCreated_whenRegistrationSuccessful() {
        // Arrange
        when(userAccountService.register(testUser)).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = userAccountController.registerUser(testUser);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testUser, response.getBody());
    }

    @Test
    void registerUser_shouldReturnConflict_whenEmailAlreadyExists() {
        // Arrange
        when(userAccountService.register(testUser)).thenThrow(new DataIntegrityViolationException("Email exists"));

        // Act
        ResponseEntity<?> response = userAccountController.registerUser(testUser);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertNotNull(responseBody);
        assertEquals("A user with this email address already exists", responseBody.get("error"));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser_whenUpdateSuccessful() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.of(testUser));
        when(userAccountService.save(any(UserAccount.class))).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = userAccountController.updateUser(1L, testUser);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser, response.getBody());
    }

    @Test
    void updateUser_shouldReturnNotFound_whenUserDoesNotExist() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = userAccountController.updateUser(1L, testUser);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteUser_shouldReturnNoContent_whenDeleteSuccessful() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<Void> response = userAccountController.deleteUser(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userAccountService).delete(1L);
    }

    @Test
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Void> response = userAccountController.deleteUser(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
