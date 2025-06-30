package com.PayMyBuddy.service;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    /**
     * Saves a user account. The @Transactional annotation ensures that the operation
     * is executed within a transaction. If an exception occurs, all changes will be rolled back.
     */
    @Transactional
    public UserAccount save(UserAccount userAccount) {
        // If the password is not already encrypted (for example during an update)
        if (userAccount.getPassword() != null && !userAccount.getPassword().startsWith("$2a$")) {
            userAccount.setPassword(passwordEncoder.encode(userAccount.getPassword()));
        }
        return userAccountRepository.save(userAccount);
    }

    /**
     * Registers a new user account. The @Transactional annotation ensures that the operation
     * is executed within a transaction. If an exception occurs, all changes will be rolled back.
     */
    @Transactional
    public UserAccount register(UserAccount userAccount) {
        if (userAccountRepository.existsByEmail(userAccount.getEmail())) {
            throw new IllegalArgumentException("A user with this email address already exists");
        }

        // Encrypt the password before saving the user
        userAccount.setPassword(passwordEncoder.encode(userAccount.getPassword()));

        return userAccountRepository.save(userAccount);
    }

    /**
     * Deletes a user account by its ID. The @Transactional annotation ensures that the operation
     * is executed within a transaction. If an exception occurs, all changes will be rolled back.
     */
    @Transactional
    public void delete(Long id) {
        userAccountRepository.deleteById(id);
    }

    /**
     * Vérifie si le mot de passe fourni correspond au mot de passe de l'utilisateur.
     * @param user l'utilisateur dont vérifier le mot de passe
     * @param rawPassword le mot de passe en clair à vérifier
     * @return true si le mot de passe correspond, false sinon
     */
    public boolean checkPassword(UserAccount user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}