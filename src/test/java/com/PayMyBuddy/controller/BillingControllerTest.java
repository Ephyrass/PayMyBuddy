package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Billing;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.BillingService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.util.AuthenticationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingControllerTest {

    @Mock
    private BillingService billingService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AuthenticationUtils authenticationUtils;

    @Mock
    private Model model;

    @InjectMocks
    private BillingController billingController;

    private UserAccount testUser;
    private Billing testBilling;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        testBilling = new Billing();
        testBilling.setId(1L);
        testBilling.setAmount(new BigDecimal("5.00"));
        testBilling.setDate(LocalDateTime.now());
        testBilling.setProcessed(false);

        when(authenticationUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void billingsPage_shouldReturnBillingsView() {
        // Arrange
        when(billingService.findAll()).thenReturn(Arrays.asList(testBilling));

        // Act
        String result = billingController.billingsPage(model);

        // Assert
        assertEquals("billings", result);
        verify(model).addAttribute("user", testUser);
        verify(model).addAttribute(eq("billings"), any());
    }

    @Test
    void markBillingAsProcessed_shouldRedirectWithSuccess_whenBillingProcessed() {
        // Arrange
        Long billingId = 1L;
        testBilling.setProcessed(true);
        when(billingService.markAsProcessed(billingId)).thenReturn(testBilling);

        // Act
        String result = billingController.markBillingAsProcessed(billingId);

        // Assert
        assertEquals("redirect:/billings?success=processed", result);
        verify(billingService).markAsProcessed(billingId);
    }

    @Test
    void markBillingAsProcessed_shouldRedirectWithError_whenBillingNotFound() {
        // Arrange
        Long billingId = 999L;
        when(billingService.markAsProcessed(billingId))
                .thenThrow(new IllegalArgumentException("Billing not found"));

        // Act
        String result = billingController.markBillingAsProcessed(billingId);

        // Assert
        assertTrue(result.startsWith("redirect:/billings?error="));
        assertTrue(result.contains("Billing+not+found"));
        verify(billingService).markAsProcessed(billingId);
    }
}
