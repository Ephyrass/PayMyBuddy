package com.PayMyBuddy.service;

import com.PayMyBuddy.model.Connection;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.ConnectionRepository;
import com.PayMyBuddy.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConnectionServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private ConnectionService connectionService;

    private UserAccount owner;
    private UserAccount friend;
    private Connection testConnection;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        owner = new UserAccount();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setFirstName("John");
        owner.setLastName("Doe");

        friend = new UserAccount();
        friend.setId(2L);
        friend.setEmail("friend@example.com");
        friend.setFirstName("Jane");
        friend.setLastName("Smith");

        testConnection = new Connection();
        testConnection.setId(1L);
        testConnection.setOwner(owner);
        testConnection.setFriend(friend);
    }

    @Test
    void findAll_shouldReturnAllConnections() {
        // Arrange
        List<Connection> expectedConnections = Arrays.asList(testConnection);
        when(connectionRepository.findAll()).thenReturn(expectedConnections);

        // Act
        List<Connection> actualConnections = connectionService.findAll();

        // Assert
        assertEquals(expectedConnections, actualConnections);
        verify(connectionRepository).findAll();
    }

    @Test
    void findByOwner_shouldReturnConnectionsForOwner() {
        // Arrange
        List<Connection> expectedConnections = Arrays.asList(testConnection);
        when(connectionRepository.findByOwner(owner)).thenReturn(expectedConnections);

        // Act
        List<Connection> actualConnections = connectionService.findByOwner(owner);

        // Assert
        assertEquals(expectedConnections, actualConnections);
        verify(connectionRepository).findByOwner(owner);
    }

    @Test
    void findByOwnerId_withValidId_shouldReturnConnections() {
        // Arrange
        List<Connection> expectedConnections = Arrays.asList(testConnection);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(connectionRepository.findByOwner(owner)).thenReturn(expectedConnections);

        // Act
        List<Connection> actualConnections = connectionService.findByOwnerId(1L);

        // Assert
        assertEquals(expectedConnections, actualConnections);
        verify(userAccountRepository).findById(1L);
        verify(connectionRepository).findByOwner(owner);
    }

    @Test
    void findByOwnerId_withInvalidId_shouldThrowException() {
        // Arrange
        when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connectionService.findByOwnerId(999L));
        verify(userAccountRepository).findById(999L);
    }

    @Test
    void findById_withExistingId_shouldReturnConnection() {
        // Arrange
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // Act
        Optional<Connection> result = connectionService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testConnection, result.get());
        verify(connectionRepository).findById(1L);
    }

    @Test
    void createConnection_withValidInput_shouldCreateConnection() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(friend));
        when(connectionRepository.existsByOwnerAndFriend(owner, friend)).thenReturn(false);
        when(connectionRepository.save(any(Connection.class))).thenReturn(testConnection);

        // Act
        Connection result = connectionService.createConnection(1L, 2L);

        // Assert
        assertEquals(testConnection, result);
        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).findById(2L);
        verify(connectionRepository).existsByOwnerAndFriend(owner, friend);
        verify(connectionRepository).save(any(Connection.class));
    }

    @Test
    void createConnection_withExistingConnection_shouldThrowException() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(friend));
        when(connectionRepository.existsByOwnerAndFriend(owner, friend)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connectionService.createConnection(1L, 2L));
        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).findById(2L);
        verify(connectionRepository).existsByOwnerAndFriend(owner, friend);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void createConnection_withSameUser_shouldThrowException() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(owner));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connectionService.createConnection(1L, 1L));
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void deleteConnection_shouldCallRepositoryDelete() {
        // Act
        connectionService.deleteConnection(1L);

        // Assert
        verify(connectionRepository).deleteById(1L);
    }

    @Test
    void deleteConnectionBetweenUsers_withExistingConnection_shouldDeleteConnection() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(friend));
        when(connectionRepository.findByOwnerAndFriend(owner, friend)).thenReturn(Optional.of(testConnection));

        // Act
        connectionService.deleteConnectionBetweenUsers(1L, 2L);

        // Assert
        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).findById(2L);
        verify(connectionRepository).findByOwnerAndFriend(owner, friend);
        verify(connectionRepository).delete(testConnection);
    }

    @Test
    void deleteConnectionBetweenUsers_withNonExistingConnection_shouldThrowException() {
        // Arrange
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(friend));
        when(connectionRepository.findByOwnerAndFriend(owner, friend)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> connectionService.deleteConnectionBetweenUsers(1L, 2L));
        verify(userAccountRepository).findById(1L);
        verify(userAccountRepository).findById(2L);
        verify(connectionRepository).findByOwnerAndFriend(owner, friend);
        verify(connectionRepository, never()).delete(any(Connection.class));
    }
}
