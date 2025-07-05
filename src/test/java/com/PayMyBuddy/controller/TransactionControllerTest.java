package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.util.AuthenticationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private Model model;

    @Mock
    private AuthenticationUtils authenticationUtils;

    @InjectMocks
    private TransactionController transactionController;

    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        when(authenticationUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void transactionsPage_shouldReturnTransactionsView() {
        // Arrange
        when(connectionService.findByOwnerId(1L)).thenReturn(Arrays.asList());
        when(transactionService.findByUser(testUser)).thenReturn(Arrays.asList());

        // Act
        String result = transactionController.transactionsPage(null, model);

        // Assert
        assertEquals("transactions", result);
        verify(model).addAttribute("user", testUser);
        verify(model).addAttribute(eq("connections"), any());
        verify(model).addAttribute(eq("transactions"), any());
    }

    @Test
    void transactionsPage_shouldAddContactId_whenContactIdProvided() {
        // Arrange
        Long contactId = 2L;
        when(connectionService.findByOwnerId(1L)).thenReturn(Arrays.asList());
        when(transactionService.findByUser(testUser)).thenReturn(Arrays.asList());

        // Act
        String result = transactionController.transactionsPage(contactId, model);

        // Assert
        assertEquals("transactions", result);
        verify(model).addAttribute("contactId", contactId);
    }

    @Test
    void sendMoney_shouldRedirectWithSuccess_whenTransactionSuccessful() {
        // Arrange
        Long receiverId = 2L;
        Double amount = 50.0;
        String description = "Test payment";

        // Act
        String result = transactionController.sendMoney(receiverId, amount, description);

        // Assert
        assertEquals("redirect:/transactions?success", result);
        verify(transactionService).makeTransaction(1L, receiverId, new BigDecimal(amount), description);
    }

    @Test
    void sendMoney_shouldRedirectWithError_whenTransactionFails() {
        // Arrange
        Long receiverId = 2L;
        Double amount = 50.0;
        String description = "Test payment";

        when(transactionService.makeTransaction(anyLong(), anyLong(), any(BigDecimal.class), anyString()))
                .thenThrow(new IllegalArgumentException("Insufficient funds"));

        // Act
        String result = transactionController.sendMoney(receiverId, amount, description);

        // Assert
        assertTrue(result.startsWith("redirect:/transactions?error="));
        assertTrue(result.contains("Insufficient+funds"));
    }
}
