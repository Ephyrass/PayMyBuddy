package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebControllerTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private Model model;

    @InjectMocks
    private WebController webController;

    private MockMvc mockMvc;
    private UserAccount testUser;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(webController).build();

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password");

        // Mock pour SecurityContext et Authentication
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Configuration par défaut du mock Authentication
        when(authentication.getName()).thenReturn("test@example.com");
        when(authentication.isAuthenticated()).thenReturn(true);
    }

    @Test
    void index_shouldRedirectToDashboard_whenUserAuthenticated() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test@example.com");

        // Act
        String result = webController.index();

        // Assert
        assertEquals("redirect:/dashboard", result);
    }

    @Test
    void index_shouldReturnIndexPage_whenUserNotAuthenticated() {
        // Arrange
        when(authentication.getName()).thenReturn("anonymousUser");

        // Act
        String result = webController.index();

        // Assert
        assertEquals("index", result);
    }

    @Test
    void registerUser_shouldAuthenticateAndRedirectToDashboard() {
        // Arrange
        when(userAccountService.save(any(UserAccount.class))).thenReturn(testUser);

        // Act
        String result = webController.registerUser(testUser);

        // Assert
        verify(userAccountService, times(1)).save(testUser);
        assertEquals("redirect:/dashboard", result);
    }

    @Test
    void dashboard_shouldReturnDashboardPage_whenUserAuthenticated() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(connectionService.findByOwnerId(1L)).thenReturn(new ArrayList<>());
        when(transactionService.findByUser(testUser)).thenReturn(new ArrayList<>());

        // Act
        String result = webController.dashboard(model);

        // Assert
        assertEquals("dashboard", result);
        verify(model).addAttribute(eq("user"), eq(testUser));
        verify(model).addAttribute(eq("connections"), anyList());
        verify(model).addAttribute(eq("transactions"), anyList());
    }

    @Test
    void dashboard_shouldRedirectToLogin_whenUserNotAuthenticated() {
        // Arrange
        when(authentication.getName()).thenReturn("anonymousUser");

        // Act
        String result = webController.dashboard(model);

        // Assert
        assertEquals("redirect:/login", result);
    }

    @Test
    void dashboard_shouldRedirectToLogin_whenUserNotFound() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act
        String result = webController.dashboard(model);

        // Assert
        assertEquals("redirect:/login?error=usernotfound", result);
    }

    @Test
    void connectionsPage_shouldReturnConnectionsPage_whenUserAuthenticated() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(connectionService.findByOwnerId(1L)).thenReturn(new ArrayList<>());

        // Act
        String result = webController.connectionsPage(model);

        // Assert
        assertEquals("connections", result);
        verify(model).addAttribute(eq("user"), eq(testUser));
        verify(model).addAttribute(eq("connections"), anyList());
    }

    @Test
    void transactionsPage_shouldReturnTransactionsPage_whenUserAuthenticated() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(connectionService.findByOwnerId(1L)).thenReturn(new ArrayList<>());
        when(transactionService.findByUser(testUser)).thenReturn(new ArrayList<>());

        // Act
        String result = webController.transactionsPage(null, model);

        // Assert
        assertEquals("transactions", result);
        verify(model).addAttribute(eq("user"), eq(testUser));
        verify(model).addAttribute(eq("connections"), anyList());
        verify(model).addAttribute(eq("transactions"), anyList());
    }

    @Test
    void addConnection_shouldRedirectWithError_whenAddingSelf() {
        // Arrange
        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        String result = webController.addConnection("test@example.com");

        // Assert
        assertEquals("redirect:/connections?error=self_connection", result);
    }

    @Test
    void addConnection_shouldRedirectWithSuccess_whenConnectionAdded() {
        // Arrange
        UserAccount friend = new UserAccount();
        friend.setId(2L);
        friend.setEmail("friend@example.com");

        when(userAccountService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userAccountService.findByEmail("friend@example.com")).thenReturn(Optional.of(friend));

        // Act
        String result = webController.addConnection("friend@example.com");

        // Assert
        verify(connectionService).createConnection(1L, 2L);
        assertEquals("redirect:/connections?success", result);
    }
}
