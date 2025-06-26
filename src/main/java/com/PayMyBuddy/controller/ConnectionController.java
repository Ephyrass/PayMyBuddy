package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.Connection;
import com.PayMyBuddy.service.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final ConnectionService connectionService;

    @Autowired
    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Retrieves all connections.
     * @return a list of all connections
     */
    @GetMapping
    public ResponseEntity<List<Connection>> getAllConnections() {
        return ResponseEntity.ok(connectionService.findAll());
    }

    /**
     * Retrieves all connections for a specific user.
     * @param userId the user ID
     * @return a list of connections for the user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Connection>> getConnectionsByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(connectionService.findByOwnerId(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves a connection by its ID.
     * @param id the connection ID
     * @return the connection or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Connection> getConnectionById(@PathVariable Long id) {
        return connectionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new connection between two users.
     * @param payload a map containing ownerId and friendId
     * @return the created connection or an error message
     */
    @PostMapping
    public ResponseEntity<Connection> createConnection(@RequestBody Map<String, Long> payload) {
        try {
            Long ownerId = payload.get("ownerId");
            Long friendId = payload.get("friendId");

            if (ownerId == null || friendId == null) {
                return ResponseEntity.badRequest().build();
            }

            Connection connection = connectionService.createConnection(ownerId, friendId);
            return ResponseEntity.status(HttpStatus.CREATED).body(connection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Deletes a connection by its ID.
     * @param id the connection ID
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        try {
            connectionService.deleteConnection(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes a connection between two users.
     * @param payload a map containing ownerId and friendId
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteConnectionBetweenUsers(@RequestBody Map<String, Long> payload) {
        try {
            Long ownerId = payload.get("ownerId");
            Long friendId = payload.get("friendId");

            if (ownerId == null || friendId == null) {
                return ResponseEntity.badRequest().build();
            }

            connectionService.deleteConnectionBetweenUsers(ownerId, friendId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
