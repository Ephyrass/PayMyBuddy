package com.PayMyBuddy.service;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAccount> findAll() {
        return userAccountRepository.findAll();
    }

    public Optional<UserAccount> findById(Long id) {
        return userAccountRepository.findById(id);
    }

    public Optional<UserAccount> findByEmail(String email) {
        return userAccountRepository.findByEmail(email);
    }

    @Transactional
    public UserAccount save(UserAccount userAccount) {
        // Si le mot de passe n'est pas déjà crypté (par exemple lors d'une mise à jour)
        if (userAccount.getPassword() != null && !userAccount.getPassword().startsWith("$2a$")) {
            userAccount.setPassword(passwordEncoder.encode(userAccount.getPassword()));
        }
        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public UserAccount register(UserAccount userAccount) {
        if (userAccountRepository.existsByEmail(userAccount.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cette adresse e-mail existe déjà");
        }

        // Initialiser le solde à zéro si non défini
        if (userAccount.getBalance() == null) {
            userAccount.setBalance(BigDecimal.ZERO);
        }

        // Crypter le mot de passe avant de sauvegarder l'utilisateur
        userAccount.setPassword(passwordEncoder.encode(userAccount.getPassword()));

        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public void delete(Long id) {
        userAccountRepository.deleteById(id);
    }

    @Transactional
    public UserAccount updateBalance(Long userId, BigDecimal amount) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        BigDecimal newBalance = user.getBalance().add(amount);

        // Vérifier que le solde ne devient pas négatif
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Solde insuffisant pour cette opération");
        }

        user.setBalance(newBalance);
        return userAccountRepository.save(user);
    }
}