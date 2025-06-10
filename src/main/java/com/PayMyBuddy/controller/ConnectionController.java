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

    @GetMapping
    public ResponseEntity<List<Connection>> getAllConnections() {
        return ResponseEntity.ok(connectionService.findAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Connection>> getConnectionsByUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(connectionService.findByOwnerId(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Connection> getConnectionById(@PathVariable Long id) {
        return connectionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
        try {
            connectionService.deleteConnection(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

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
