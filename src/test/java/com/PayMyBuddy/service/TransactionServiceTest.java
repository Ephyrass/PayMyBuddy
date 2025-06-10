package com.PayMyBuddy.service;

import com.PayMyBuddy.model.Connection;
import com.PayMyBuddy.model.Transaction;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.BillingRepository;
import com.PayMyBuddy.repository.ConnectionRepository;
import com.PayMyBuddy.repository.TransactionRepository;
import com.PayMyBuddy.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private BillingRepository billingRepository;

    @InjectMocks
    private TransactionService transactionService;

    private UserAccount sender;
    private UserAccount receiver;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure le pourcentage de frais
        ReflectionTestUtils.setField(transactionService, "feePercentage", new BigDecimal("0.5"));

        // Création d'un expéditeur avec solde de 1000
        sender = new UserAccount();
        sender.setId(1L);
        sender.setEmail("sender@example.com");
        sender.setFirstName("John");
        sender.setLastName("Sender");
        sender.setPassword("password");
        sender.setBalance(new BigDecimal("1000.00"));

        // Création d'un destinataire avec solde de 500
        receiver = new UserAccount();
        receiver.setId(2L);
        receiver.setEmail("receiver@example.com");
        receiver.setFirstName("Jane");
        receiver.setLastName("Receiver");
        receiver.setPassword("password");
        receiver.setBalance(new BigDecimal("500.00"));

        // Création d'une transaction de test
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setSender(sender);
        testTransaction.setReceiver(receiver);
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setFee(new BigDecimal("0.50"));
        testTransaction.setDate(LocalDateTime.now());
        testTransaction.setDescription("Test transaction");
    }

    @Test
    void findAll_shouldReturnAllTransactions() {
        // Arrange
        List<Transaction> expectedTransactions = Arrays.asList(testTransaction);
        when(transactionRepository.findAll()).thenReturn(expectedTransactions);

        // Act
        List<Transaction> actualTransactions = transactionService.findAll();

        // Assert
        assertEquals(expectedTransactions, actualTransactions);
        verify(transactionRepository).findAll();
    }

    @Test
    void findById_withExistingId_shouldReturnTransaction() {
        // Arrange
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransaction));

        // Act
        Optional<Transaction> result = transactionService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testTransaction, result.get());
        verify(transactionRepository).findById(1L);
    }

    @Test
    void makeTransaction_withValidData_shouldCreateTransaction() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(connectionRepository.existsByOwnerAndFriend(sender, receiver)).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal amount = new BigDecimal("100.00");

        // Act
        Transaction result = transactionService.makeTransaction(1L, 2L, amount, "Test transaction");

        // Assert
        assertEquals(testTransaction, result);
        assertEquals(new BigDecimal("899.50"), sender.getBalance()); // 1000 - 100 - 0.5 (frais)
        assertEquals(new BigDecimal("600.00"), receiver.getBalance()); // 500 + 100

        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).findById(2L);
        verify(connectionRepository).existsByOwnerAndFriend(sender, receiver);
        verify(userAccountRepository).save(sender);
        verify(userAccountRepository).save(receiver);
        verify(transactionRepository).save(any(Transaction.class));
        verify(billingRepository).save(any());
    }

    @Test
    void makeTransaction_withInsufficientFunds_shouldThrowException() {
        // Arrange
        sender.setBalance(new BigDecimal("50.00")); // Solde insuffisant

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(connectionRepository.existsByOwnerAndFriend(sender, receiver)).thenReturn(true);

        BigDecimal amount = new BigDecimal("100.00");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.makeTransaction(1L, 2L, amount, "Test transaction");
        });

        assertEquals("Solde insuffisant pour cette transaction", exception.getMessage());

        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).findById(2L);
        verify(connectionRepository).existsByOwnerAndFriend(sender, receiver);
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void makeTransaction_withNoConnection_shouldThrowException() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(connectionRepository.existsByOwnerAndFriend(sender, receiver)).thenReturn(false);

        BigDecimal amount = new BigDecimal("100.00");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.makeTransaction(1L, 2L, amount, "Test transaction");
        });

        assertEquals("Vous n'êtes pas connecté à cet utilisateur", exception.getMessage());

        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).findById(2L);
        verify(connectionRepository).existsByOwnerAndFriend(sender, receiver);
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void depositFunds_withValidAmount_shouldIncreaseBalance() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        BigDecimal initialBalance = sender.getBalance();
        BigDecimal depositAmount = new BigDecimal("200.00");

        // Act
        Transaction result = transactionService.depositFunds(1L, depositAmount, "Test deposit");

        // Assert
        assertEquals(testTransaction, result);
        assertEquals(initialBalance.add(depositAmount), sender.getBalance());

        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).save(sender);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdrawFunds_withValidAmount_shouldDecreaseBalance() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        BigDecimal initialBalance = sender.getBalance();
        BigDecimal withdrawAmount = new BigDecimal("200.00");

        // Act
        Transaction result = transactionService.withdrawFunds(1L, withdrawAmount, "Test withdrawal");

        // Assert
        assertEquals(testTransaction, result);
        assertEquals(initialBalance.subtract(withdrawAmount), sender.getBalance());

        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).save(sender);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdrawFunds_withInsufficientFunds_shouldThrowException() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(sender));

        BigDecimal withdrawAmount = new BigDecimal("1200.00"); // Plus que le solde disponible

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.withdrawFunds(1L, withdrawAmount, "Test withdrawal");
        });

        assertEquals("Solde insuffisant pour ce retrait", exception.getMessage());

        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
