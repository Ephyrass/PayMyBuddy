package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Transaction;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    }

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
    void getTransactionById_shouldReturnTransaction_whenFound() {
        // Arrange
        when(transactionService.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<Transaction> response = transactionController.getTransactionById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testTransaction, response.getBody());
    }

    @Test
    void getTransactionById_shouldReturnNotFound_whenTransactionNotFound() {
        // Arrange
        when(transactionService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Transaction> response = transactionController.getTransactionById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getTransactionsByUser_shouldReturnTransactions_whenUserFound() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.of(sender));
        when(transactionService.findByUser(sender)).thenReturn(transactions);

        // Act
        ResponseEntity<List<Transaction>> response = transactionController.getTransactionsByUser(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(transactions, response.getBody());
    }

    @Test
    void getTransactionsByUser_shouldReturnNotFound_whenUserNotFound() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<List<Transaction>> response = transactionController.getTransactionsByUser(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getTransactionsBySender_shouldReturnTransactions_whenSenderFound() {
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
    void getTransactionsBySender_shouldReturnNotFound_whenSenderNotFound() {
        // Arrange
        when(userAccountService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<List<Transaction>> response = transactionController.getTransactionsBySender(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getTransactionsByReceiver_shouldReturnTransactions_whenReceiverFound() {
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
    void getTransactionsByReceiver_shouldReturnNotFound_whenReceiverNotFound() {
        // Arrange
        when(userAccountService.findById(2L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<List<Transaction>> response = transactionController.getTransactionsByReceiver(2L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void makeTransaction_shouldReturnCreated_whenTransactionSuccessful() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", 1L);
        payload.put("receiverId", 2L);
        payload.put("amount", "100.00");
        payload.put("description", "Test transaction");

        when(transactionService.makeTransaction(eq(1L), eq(2L), any(BigDecimal.class), eq("Test transaction")))
                .thenReturn(testTransaction);

        // Act
        ResponseEntity<?> response = transactionController.makeTransaction(payload);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testTransaction, response.getBody());
    }

    @Test
    void makeTransaction_shouldReturnBadRequest_whenAmountIsZeroOrNegative() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", 1L);
        payload.put("receiverId", 2L);
        payload.put("amount", "0.00");
        payload.put("description", "Test transaction");

        // Act
        ResponseEntity<?> response = transactionController.makeTransaction(payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Le montant doit être supérieur à zéro", response.getBody());
    }

    @Test
    void makeTransaction_shouldReturnBadRequest_whenIllegalArgumentException() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", 1L);
        payload.put("receiverId", 2L);
        payload.put("amount", "100.00");
        payload.put("description", "Test transaction");

        when(transactionService.makeTransaction(eq(1L), eq(2L), any(BigDecimal.class), eq("Test transaction")))
                .thenThrow(new IllegalArgumentException("Solde insuffisant"));

        // Act
        ResponseEntity<?> response = transactionController.makeTransaction(payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Solde insuffisant", response.getBody());
    }

    @Test
    void makeTransaction_shouldReturnInternalServerError_whenGenericException() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("senderId", 1L);
        payload.put("receiverId", 2L);
        payload.put("amount", "100.00");
        payload.put("description", "Test transaction");

        when(transactionService.makeTransaction(eq(1L), eq(2L), any(BigDecimal.class), eq("Test transaction")))
                .thenThrow(new RuntimeException("Erreur interne"));

        // Act
        ResponseEntity<?> response = transactionController.makeTransaction(payload);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Une erreur est survenue"));
    }
}
