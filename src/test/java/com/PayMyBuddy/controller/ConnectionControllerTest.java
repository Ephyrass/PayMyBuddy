package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Connection;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @InjectMocks
    private ConnectionController connectionController;

    private Connection testConnection;
    private List<Connection> connections;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        UserAccount owner = new UserAccount();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        UserAccount friend = new UserAccount();
        friend.setId(2L);
        friend.setEmail("friend@example.com");

        testConnection = new Connection();
        testConnection.setId(1L);
        testConnection.setOwner(owner);
        testConnection.setFriend(friend);

        connections = Arrays.asList(testConnection);
    }

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
    void getConnectionsByUser_shouldReturnConnections_whenUserFound() {
        // Arrange
        when(connectionService.findByOwnerId(1L)).thenReturn(connections);

        // Act
        ResponseEntity<List<Connection>> response = connectionController.getConnectionsByUser(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(connections, response.getBody());
    }

    @Test
    void getConnectionsByUser_shouldReturnNotFound_whenUserNotFound() {
        // Arrange
        when(connectionService.findByOwnerId(1L)).thenThrow(new IllegalArgumentException("User not found"));

        // Act
        ResponseEntity<List<Connection>> response = connectionController.getConnectionsByUser(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getConnectionById_shouldReturnConnection_whenFound() {
        // Arrange
        when(connectionService.findById(1L)).thenReturn(Optional.of(testConnection));

        // Act
        ResponseEntity<Connection> response = connectionController.getConnectionById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testConnection, response.getBody());
    }

    @Test
    void getConnectionById_shouldReturnNotFound_whenConnectionNotFound() {
        // Arrange
        when(connectionService.findById(1L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Connection> response = connectionController.getConnectionById(1L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void createConnection_shouldReturnCreated_whenConnectionSuccessful() {
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
    void createConnection_shouldReturnBadRequest_whenMissingParameters() {
        // Arrange
        Map<String, Long> payload = new HashMap<>();
        payload.put("ownerId", 1L);
        // Missing friendId

        // Act
        ResponseEntity<Connection> response = connectionController.createConnection(payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void createConnection_shouldReturnBadRequest_whenIllegalArgumentException() {
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
    void deleteConnection_shouldReturnNoContent_whenDeleteSuccessful() {
        // Arrange
        doNothing().when(connectionService).deleteConnection(1L);

        // Act
        ResponseEntity<Void> response = connectionController.deleteConnection(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteConnection_shouldReturnNotFound_whenConnectionNotFound() {
        // Arrange
        doThrow(new IllegalArgumentException("Connection not found")).when(connectionService).deleteConnection(1L);

        // Act
        ResponseEntity<Void> response = connectionController.deleteConnection(1L);

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
    void deleteConnectionBetweenUsers_shouldReturnBadRequest_whenMissingParameters() {
        // Arrange
        Map<String, Long> payload = new HashMap<>();
        payload.put("ownerId", 1L);
        // Missing friendId

        // Act
        ResponseEntity<Void> response = connectionController.deleteConnectionBetweenUsers(payload);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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
