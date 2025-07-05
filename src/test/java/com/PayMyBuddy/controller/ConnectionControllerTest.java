package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Connection;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.UserAccountService;
import com.PayMyBuddy.util.AuthenticationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConnectionControllerTest {

    @Mock
    private ConnectionService connectionService;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private AuthenticationUtils authenticationUtils;

    @Mock
    private Model model;

    @InjectMocks
    private ConnectionController connectionController;

    private UserAccount testUser;
    private UserAccount friend;
    private Connection testConnection;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        friend = new UserAccount();
        friend.setId(2L);
        friend.setEmail("friend@example.com");
        friend.setFirstName("Friend");
        friend.setLastName("User");

        testConnection = new Connection();
        testConnection.setId(1L);
        testConnection.setOwner(testUser);
        testConnection.setFriend(friend);

        when(authenticationUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void connectionsPage_shouldReturnConnectionsView() {
        // Arrange
        when(connectionService.findByOwnerId(1L)).thenReturn(Arrays.asList(testConnection));

        // Act
        String result = connectionController.connectionsPage(model);

        // Assert
        assertEquals("connections", result);
        verify(model).addAttribute("user", testUser);
        verify(model).addAttribute(eq("connections"), any());
    }

    @Test
    void addConnection_shouldRedirectWithSuccess_whenConnectionCreated() {
        // Arrange
        String friendEmail = "friend@example.com";
        when(userAccountService.findByEmail(friendEmail)).thenReturn(Optional.of(friend));
        when(connectionService.createConnection(1L, 2L)).thenReturn(testConnection);

        // Act
        String result = connectionController.addConnection(friendEmail);

        // Assert
        assertEquals("redirect:/connections?success", result);
        verify(connectionService).createConnection(1L, 2L);
    }

    @Test
    void addConnection_shouldRedirectWithError_whenTryingToAddSelf() {
        // Arrange
        String ownEmail = "test@example.com";

        // Act
        String result = connectionController.addConnection(ownEmail);

        // Assert
        assertEquals("redirect:/connections?error=self_connection", result);
        verify(connectionService, never()).createConnection(anyLong(), anyLong());
    }

    @Test
    void addConnection_shouldRedirectWithError_whenFriendNotFound() {
        // Arrange
        String friendEmail = "notfound@example.com";
        when(userAccountService.findByEmail(friendEmail)).thenReturn(Optional.empty());

        // Act
        String result = connectionController.addConnection(friendEmail);

        // Assert
        assertTrue(result.startsWith("redirect:/connections?error="));
        assertTrue(result.contains("Contact+not+found"));
        verify(connectionService, never()).createConnection(anyLong(), anyLong());
    }

    @Test
    void deleteConnection_shouldRedirectWithSuccess_whenConnectionDeleted() {
        // Arrange
        Long connectionId = 1L;
        when(connectionService.findById(connectionId)).thenReturn(Optional.of(testConnection));

        // Act
        String result = connectionController.deleteConnection(connectionId);

        // Assert
        assertEquals("redirect:/connections?success_delete", result);
        verify(connectionService).deleteConnection(connectionId);
    }

    @Test
    void deleteConnection_shouldRedirectWithError_whenConnectionNotFound() {
        // Arrange
        Long connectionId = 999L;
        when(connectionService.findById(connectionId)).thenReturn(Optional.empty());

        // Act
        String result = connectionController.deleteConnection(connectionId);

        // Assert
        assertTrue(result.startsWith("redirect:/connections?error="));
        assertTrue(result.contains("Connection+not+found"));
        verify(connectionService, never()).deleteConnection(anyLong());
    }

    @Test
    void deleteConnection_shouldRedirectWithError_whenNotAuthorized() {
        // Arrange
        Long connectionId = 1L;
        UserAccount anotherUser = new UserAccount();
        anotherUser.setId(3L);

        Connection anotherConnection = new Connection();
        anotherConnection.setId(connectionId);
        anotherConnection.setOwner(anotherUser);

        when(connectionService.findById(connectionId)).thenReturn(Optional.of(anotherConnection));

        // Act
        String result = connectionController.deleteConnection(connectionId);

        // Assert
        assertTrue(result.startsWith("redirect:/connections?error="));
        assertTrue(result.contains("not+authorized"));
        verify(connectionService, never()).deleteConnection(anyLong());
    }
}
