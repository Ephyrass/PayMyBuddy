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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Model model;

    @Mock
    private AuthenticationUtils authenticationUtils;

    @InjectMocks
    private ConnectionController connectionController;

    private MockMvc mockMvc;
    private Connection testConnection;
    private UserAccount testUser;
    private UserAccount testFriend;
    private List<Connection> connections;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(connectionController).build();

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("owner@example.com");
        testUser.setFirstName("Owner");
        testUser.setLastName("User");

        testFriend = new UserAccount();
        testFriend.setId(2L);
        testFriend.setEmail("friend@example.com");
        testFriend.setFirstName("Friend");
        testFriend.setLastName("User");

        testConnection = new Connection();
        testConnection.setId(1L);
        testConnection.setOwner(testUser);
        testConnection.setFriend(testFriend);

        connections = Arrays.asList(testConnection);

        // Mock SecurityContext et Authentication
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("owner@example.com");
        SecurityContextHolder.setContext(securityContext);

        // Mock AuthenticationUtils
        when(authenticationUtils.getCurrentUser()).thenReturn(testUser);
        when(authenticationUtils.getCurrentUserEmail()).thenReturn(testUser.getEmail());
    }

    // Tests pour les pages web

    @Test
    void connectionsPage_shouldReturnConnectionsView() {
        // Arrange
        when(userAccountService.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));
        when(connectionService.findByOwnerId(1L)).thenReturn(connections);

        // Act
        String result = connectionController.connectionsPage(model);

        // Assert
        assertEquals("connections", result);
        verify(model).addAttribute("user", testUser);
        verify(model).addAttribute("connections", connections);
    }

    @Test
    void addConnection_shouldRedirectWithSuccess_whenConnectionCreatedSuccessfully() {
        // Arrange
        when(userAccountService.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));
        when(userAccountService.findByEmail("friend@example.com")).thenReturn(Optional.of(testFriend));
        when(connectionService.createConnection(1L, 2L)).thenReturn(testConnection);

        // Act
        String result = connectionController.addConnection("friend@example.com");

        // Assert
        assertEquals("redirect:/connections?success", result);
        verify(connectionService).createConnection(1L, 2L);
    }

    @Test
    void addConnection_shouldRedirectWithError_whenTryingToAddSelf() {
        // Arrange
        when(userAccountService.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));

        // Act
        String result = connectionController.addConnection("owner@example.com");

        // Assert
        assertEquals("redirect:/connections?error=self_connection", result);
        verify(connectionService, never()).createConnection(anyLong(), anyLong());
    }

    @Test
    void addConnection_shouldRedirectWithError_whenFriendNotFound() {
        // Arrange
        when(userAccountService.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));
        when(userAccountService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        String result = connectionController.addConnection("nonexistent@example.com");

        // Assert
        assertTrue(result.startsWith("redirect:/connections?error="));
        verify(connectionService, never()).createConnection(anyLong(), anyLong());
    }

    @Test
    void deleteConnection_shouldRedirectWithSuccess_whenConnectionDeletedSuccessfully() {
        // Arrange
        when(userAccountService.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));
        when(connectionService.findById(1L)).thenReturn(Optional.of(testConnection));
        doNothing().when(connectionService).deleteConnection(1L);

        // Act
        String result = connectionController.deleteConnection(1L);

        // Assert
        assertEquals("redirect:/connections?success_delete", result);
        verify(connectionService).deleteConnection(1L);
    }

    @Test
    void deleteConnection_shouldRedirectWithError_whenConnectionNotFound() {
        // Arrange
        when(userAccountService.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));
        when(connectionService.findById(1L)).thenReturn(Optional.empty());

        // Act
        String result = connectionController.deleteConnection(1L);

        // Assert
        assertEquals("redirect:/connections?error=connection_not_found", result);
        verify(connectionService, never()).deleteConnection(anyLong());
    }

    @Test
    void deleteConnection_shouldRedirectWithError_whenUserNotAuthorized() {
        // Arrange
        UserAccount otherUser = new UserAccount();
        otherUser.setId(3L);

        Connection otherConnection = new Connection();
        otherConnection.setId(1L);
        otherConnection.setOwner(otherUser);
        otherConnection.setFriend(testFriend);

        when(userAccountService.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));
        when(connectionService.findById(1L)).thenReturn(Optional.of(otherConnection));

        // Act
        String result = connectionController.deleteConnection(1L);

        // Assert
        assertTrue(result.startsWith("redirect:/connections?error="));
        verify(connectionService, never()).deleteConnection(anyLong());
    }

    // Tests pour l'API

    @Test
    void getAllConnections_shouldReturnAllConnections() {
        // Arrange
        when(connectionService.findAll()).thenReturn(connections);

        // Act
        ResponseEntity<List<Connection>> response = connectionController.getAllConnections();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(connections, response.getBody());
    }

    @Test
    void getConnectionsByUser_shouldReturnUserConnections() {
        // Arrange
        when(connectionService.findByOwnerId(1L)).thenReturn(connections);

        // Act
        ResponseEntity<List<Connection>> response = connectionController.getConnectionsByUser(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(connections, response.getBody());
    }

    @Test
    void getConnectionById_shouldReturnConnection_whenConnectionExists() {
        // Arrange
        when(connectionService.findById(1L)).thenReturn(Optional.of(testConnection));

        // Act
        ResponseEntity<Connection> response = connectionController.getConnectionById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testConnection, response.getBody());
    }

    @Test
    void getConnectionById_shouldReturnNotFound_whenConnectionDoesNotExist() {
        // Arrange
        when(connectionService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Connection> response = connectionController.getConnectionById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createConnection_shouldReturnCreated_whenConnectionCreatedSuccessfully() {
        // Arrange
        Map<String, Long> payload = new HashMap<>();
        payload.put("ownerId", 1L);
        payload.put("friendId", 2L);

        when(connectionService.createConnection(1L, 2L)).thenReturn(testConnection);

        // Act
        ResponseEntity<Connection> response = connectionController.createConnection(payload);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testConnection, response.getBody());
    }

    @Test
    void createConnection_shouldReturnBadRequest_whenConnectionCreationFails() {
        // Arrange
        Map<String, Long> payload = new HashMap<>();
        payload.put("ownerId", 1L);
        payload.put("friendId", 2L);

        when(connectionService.createConnection(1L, 2L)).thenThrow(new IllegalArgumentException("Invalid connection"));

        // Act
        ResponseEntity<Connection> response = connectionController.createConnection(payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void deleteConnectionApi_shouldReturnNoContent_whenDeleteSuccessful() {
        // Arrange
        doNothing().when(connectionService).deleteConnection(1L);

        // Act
        ResponseEntity<Void> response = connectionController.deleteConnectionApi(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteConnectionApi_shouldReturnNotFound_whenConnectionNotFound() {
        // Arrange
        doThrow(new IllegalArgumentException("Connection not found")).when(connectionService).deleteConnection(1L);

        // Act
        ResponseEntity<Void> response = connectionController.deleteConnectionApi(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteConnectionBetweenUsers_shouldReturnNoContent_whenDeleteSuccessful() {
        // Arrange
        Map<String, Long> payload = new HashMap<>();
        payload.put("ownerId", 1L);
        payload.put("friendId", 2L);

        doNothing().when(connectionService).deleteConnectionBetweenUsers(1L, 2L);

        // Act
        ResponseEntity<Void> response = connectionController.deleteConnectionBetweenUsers(payload);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }


    @Test
    void deleteConnectionBetweenUsers_shouldReturnNotFound_whenConnectionNotFound() {
        // Arrange
        Map<String, Long> payload = new HashMap<>();
        payload.put("ownerId", 1L);
        payload.put("friendId", 2L);

        doThrow(new IllegalArgumentException("Connection not found")).when(connectionService).deleteConnectionBetweenUsers(1L, 2L);

        // Act
        ResponseEntity<Void> response = connectionController.deleteConnectionBetweenUsers(payload);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
