package com.PayMyBuddy.service;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private UserAccountService userAccountService;

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
        testUser.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        // Arrange
        List<UserAccount> expectedUsers = Arrays.asList(testUser);
        when(userAccountRepository.findAll()).thenReturn(expectedUsers);

        // Act
        List<UserAccount> actualUsers = userAccountService.findAll();

        // Assert
        assertEquals(expectedUsers, actualUsers);
        verify(userAccountRepository).findAll();
    }

    @Test
    void findById_withExistingId_shouldReturnUser() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<UserAccount> result = userAccountService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userAccountRepository).findById(1L);
    }

    @Test
    void findByEmail_withExistingEmail_shouldReturnUser() {
        // Arrange
        when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<UserAccount> result = userAccountService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userAccountRepository).findByEmail("test@example.com");
    }

    @Test
    void register_withNewEmail_shouldSaveUser() {
        // Arrange
        when(userAccountRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUser);

        UserAccount newUser = new UserAccount();
        newUser.setEmail("new@example.com");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setPassword("password");

        // Act
        UserAccount savedUser = userAccountService.register(newUser);

        // Assert
        assertEquals(testUser, savedUser);
        verify(userAccountRepository).existsByEmail("new@example.com");
        verify(userAccountRepository).save(newUser);
    }

    @Test
    void register_withExistingEmail_shouldThrowException() {
        // Arrange
        when(userAccountRepository.existsByEmail("test@example.com")).thenReturn(true);

        UserAccount newUser = new UserAccount();
        newUser.setEmail("test@example.com");
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        newUser.setPassword("password");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userAccountService.register(newUser);
        });

        assertEquals("Un utilisateur avec cette adresse e-mail existe déjà", exception.getMessage());
        verify(userAccountRepository).existsByEmail("test@example.com");
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void updateBalance_withSufficientFunds_shouldUpdateBalance() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUser);

        BigDecimal amount = new BigDecimal("-50.00");

        // Act
        UserAccount updatedUser = userAccountService.updateBalance(1L, amount);

        // Assert
        assertEquals(new BigDecimal("50.00"), updatedUser.getBalance());
        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).save(testUser);
    }

    @Test
    void updateBalance_withInsufficientFunds_shouldThrowException() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));

        BigDecimal amount = new BigDecimal("-150.00");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userAccountService.updateBalance(1L, amount);
        });

        assertEquals("Solde insuffisant pour cette opération", exception.getMessage());
        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }
}
