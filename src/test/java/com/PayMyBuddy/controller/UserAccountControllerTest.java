package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @InjectMocks
    private UserAccountController userAccountController;

    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password");
    }

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
    void getUserById_shouldReturnUser_whenFound() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<UserAccount> response = userAccountController.getUserById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUser, response.getBody());
    }

    @Test
    void getUserById_shouldReturnNotFound_whenUserNotFound() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<UserAccount> response = userAccountController.getUserById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void registerUser_shouldReturnCreated_whenRegistrationSuccessful() {
        // Arrange
        UserAccount newUser = new UserAccount();
        newUser.setEmail("new@example.com");
        newUser.setPassword("password");
        newUser.setFirstName("New");
        newUser.setLastName("User");

        when(userAccountService.register(any(UserAccount.class))).thenReturn(testUser);

        // Act
        ResponseEntity<?> response = userAccountController.registerUser(newUser);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testUser, response.getBody());
    }

    @Test
    void registerUser_shouldReturnConflict_whenEmailAlreadyExists() {
        // Arrange
        UserAccount newUser = new UserAccount();
        newUser.setEmail("existing@example.com");
        newUser.setPassword("password");

        when(userAccountService.register(any(UserAccount.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        // Act
        ResponseEntity<?> response = userAccountController.registerUser(newUser);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();

        assertEquals("Un utilisateur avec cette adresse e-mail existe déjà", responseBody.get("error"));
    }

    @Test
    void registerUser_shouldReturnConflict_whenValidationFails() {
        // Arrange
        UserAccount newUser = new UserAccount();
        newUser.setEmail("invalid@example.com");

        when(userAccountService.register(any(UserAccount.class)))
                .thenThrow(new IllegalArgumentException("Validation error"));

        // Act
        ResponseEntity<?> response = userAccountController.registerUser(newUser);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();

        assertEquals("Validation error", responseBody.get("error"));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser_whenUserExists() {
        // Arrange
        UserAccount updatedUser = new UserAccount();
        updatedUser.setId(1L);
        updatedUser.setEmail("updated@example.com");
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("User");

        when(userAccountService.findById(1L)).thenReturn(Optional.of(testUser));
        when(userAccountService.save(any(UserAccount.class))).thenReturn(updatedUser);

        // Act
        ResponseEntity<?> response = userAccountController.updateUser(1L, updatedUser);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedUser, response.getBody());
    }

    @Test
    void updateUser_shouldReturnNotFound_whenUserDoesNotExist() {
        // Arrange
        UserAccount updatedUser = new UserAccount();
        updatedUser.setEmail("updated@example.com");

        when(userAccountService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = userAccountController.updateUser(1L, updatedUser);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void updateUser_shouldReturnConflict_whenEmailAlreadyExists() {
        // Arrange
        UserAccount updatedUser = new UserAccount();
        updatedUser.setEmail("duplicate@example.com");

        when(userAccountService.findById(1L)).thenReturn(Optional.of(testUser));
        when(userAccountService.save(any(UserAccount.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        // Act
        ResponseEntity<?> response = userAccountController.updateUser(1L, updatedUser);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();

        assertEquals("Un utilisateur avec cette adresse e-mail existe déjà", responseBody.get("error"));
    }

    @Test
    void deleteUser_shouldReturnNoContent_whenUserExists() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<Void> response = userAccountController.deleteUser(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userAccountService, times(1)).delete(1L);
    }

    @Test
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Void> response = userAccountController.deleteUser(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userAccountService, times(0)).delete(anyLong());
    }
}
