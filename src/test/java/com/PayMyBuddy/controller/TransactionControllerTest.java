package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Transaction;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private Model model;

    @InjectMocks
    private TransactionController transactionController;

    private UserAccount sender;
    private UserAccount receiver;
    private Transaction testTransaction;
    private List<Transaction> transactions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sender = new UserAccount();
        sender.setId(1L);
        sender.setEmail("sender@example.com");
        sender.setFirstName("Sender");
        sender.setLastName("User");

        receiver = new UserAccount();
        receiver.setId(2L);
        receiver.setEmail("receiver@example.com");
        receiver.setFirstName("Receiver");
        receiver.setLastName("User");

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setSender(sender);
        testTransaction.setReceiver(receiver);
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setDescription("Test transaction");

        transactions = Arrays.asList(testTransaction);

        // Mock SecurityContext et Authentication
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("sender@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    // Tests pour les pages web

    @Test
    void transactionsPage_shouldReturnTransactionsView() {
        // Arrange
        when(userAccountService.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(connectionService.findByOwnerId(1L)).thenReturn(Collections.emptyList());
        when(transactionService.findByUser(sender)).thenReturn(transactions);

        // Act
        String result = transactionController.transactionsPage(null, model);

        // Assert
        assertEquals("transactions", result);
        verify(model).addAttribute("user", sender);
        verify(model).addAttribute(eq("connections"), any());
        verify(model).addAttribute("transactions", transactions);
    }

    @Test
    void transactionsPage_shouldIncludeContactId_whenContactIdProvided() {
        // Arrange
        when(userAccountService.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(connectionService.findByOwnerId(1L)).thenReturn(Collections.emptyList());
        when(transactionService.findByUser(sender)).thenReturn(transactions);

        // Act
        String result = transactionController.transactionsPage(2L, model);

        // Assert
        assertEquals("transactions", result);
        verify(model).addAttribute("user", sender);
        verify(model).addAttribute("contactId", 2L);
    }

    @Test
    void sendMoney_shouldRedirectWithSuccess_whenTransactionSuccessful() {
        // Arrange
        when(userAccountService.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(transactionService.makeTransaction(1L, 2L, new BigDecimal("50"), "Test payment"))
                .thenReturn(testTransaction);

        // Act
        String result = transactionController.sendMoney(2L, 50.0, "Test payment");

        // Assert
        assertEquals("redirect:/transactions?success", result);
        verify(transactionService).makeTransaction(1L, 2L, new BigDecimal("50"), "Test payment");
    }

    @Test
    void sendMoney_shouldRedirectWithError_whenTransactionFails() {
        // Arrange
        when(userAccountService.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));
        when(transactionService.makeTransaction(1L, 2L, new BigDecimal("50"), "Test payment"))
                .thenThrow(new IllegalArgumentException("Insufficient funds"));

        // Act
        String result = transactionController.sendMoney(2L, 50.0, "Test payment");

        // Assert
        assertTrue(result.startsWith("redirect:/transactions?error="));
    }

    // Tests pour l'API

    @Test
    void getAllTransactions_shouldReturnAllTransactions() {
        // Arrange
        when(transactionService.findAll()).thenReturn(transactions);

        // Act
        ResponseEntity<List<Transaction>> response = transactionController.getAllTransactions();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(transactions, response.getBody());
    }

    @Test
    void getTransactionById_shouldReturnTransaction_whenTransactionExists() {
        // Arrange
        when(transactionService.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<Transaction> response = transactionController.getTransactionById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testTransaction, response.getBody());
    }

    @Test
    void getTransactionById_shouldReturnNotFound_whenTransactionDoesNotExist() {
        // Arrange
        when(transactionService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Transaction> response = transactionController.getTransactionById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTransactionsBySender_shouldReturnSenderTransactions() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.of(sender));
        when(transactionService.findBySender(sender)).thenReturn(transactions);

        // Act
        ResponseEntity<List<Transaction>> response = transactionController.getTransactionsBySender(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(transactions, response.getBody());
    }

    @Test
    void getTransactionsByReceiver_shouldReturnReceiverTransactions() {
        // Arrange
        when(userAccountService.findById(2L)).thenReturn(Optional.of(receiver));
        when(transactionService.findByReceiver(receiver)).thenReturn(transactions);

        // Act
        ResponseEntity<List<Transaction>> response = transactionController.getTransactionsByReceiver(2L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(transactions, response.getBody());
    }

    @Test
    void makeTransaction_shouldReturnCreated_whenTransactionSuccessful() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", 1L);
        payload.put("receiverId", 2L);
        payload.put("amount", "100.00");
        payload.put("description", "Test transaction");

        when(transactionService.makeTransaction(1L, 2L, new BigDecimal("100.00"), "Test transaction"))
                .thenReturn(testTransaction);

        // Act
        ResponseEntity<?> response = transactionController.makeTransaction(payload);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testTransaction, response.getBody());
    }

    @Test
    void makeTransaction_shouldReturnBadRequest_whenAmountIsZero() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", 1L);
        payload.put("receiverId", 2L);
        payload.put("amount", "0");
        payload.put("description", "Test transaction");

        // Act
        ResponseEntity<?> response = transactionController.makeTransaction(payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Le montant doit être supérieur à zéro", response.getBody());
    }

    @Test
    void makeTransaction_shouldReturnBadRequest_whenTransactionFails() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", 1L);
        payload.put("receiverId", 2L);
        payload.put("amount", "100.00");
        payload.put("description", "Test transaction");

        when(transactionService.makeTransaction(1L, 2L, new BigDecimal("100.00"), "Test transaction"))
                .thenThrow(new IllegalArgumentException("Insufficient funds"));

        // Act
        ResponseEntity<?> response = transactionController.makeTransaction(payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Insufficient funds", response.getBody());
    }

    @Test
    void makeTransaction_shouldReturnInternalServerError_whenUnexpectedErrorOccurs() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", 1L);
        payload.put("receiverId", 2L);
        payload.put("amount", "100.00");
        payload.put("description", "Test transaction");

        when(transactionService.makeTransaction(1L, 2L, new BigDecimal("100.00"), "Test transaction"))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<?> response = transactionController.makeTransaction(payload);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Une erreur est survenue"));
    }
}
