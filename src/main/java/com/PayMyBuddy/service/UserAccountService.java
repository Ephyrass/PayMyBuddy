package com.PayMyBuddy.service;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    @Autowired
    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
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
