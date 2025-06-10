package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {

    private final UserAccountService userAccountService;

    @Autowired
    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ResponseEntity<List<UserAccount>> getAllUsers() {
        return ResponseEntity.ok(userAccountService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getUserById(@PathVariable Long id) {
        return userAccountService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserAccount user) {
        try {
            UserAccount registeredUser = userAccountService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (DataIntegrityViolationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Un utilisateur avec cette adresse e-mail existe déjà");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Une erreur est survenue lors de l'inscription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<?> depositFunds(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());
            return ResponseEntity.ok(userAccountService.updateBalance(id, amount));
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdrawFunds(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            BigDecimal amount = new BigDecimal(payload.get("amount").toString()).negate(); // Négation pour retrait
            return ResponseEntity.ok(userAccountService.updateBalance(id, amount));
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserAccount user) {
        try {
            return userAccountService.findById(id)
                    .map(existingUser -> {
                        user.setId(id);
                        UserAccount updatedUser = userAccountService.save(user);
                        return ResponseEntity.ok(updatedUser);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (DataIntegrityViolationException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Un utilisateur avec cette adresse e-mail existe déjà");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userAccountService.findById(id).isPresent()) {
            userAccountService.delete(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
