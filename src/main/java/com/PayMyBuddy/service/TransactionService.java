package com.PayMyBuddy.service;

import com.PayMyBuddy.model.Billing;
import com.PayMyBuddy.model.Transaction;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.BillingRepository;
import com.PayMyBuddy.repository.ConnectionRepository;
import com.PayMyBuddy.repository.TransactionRepository;
import com.PayMyBuddy.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserAccountRepository userAccountRepository;
    private final ConnectionRepository connectionRepository;
    private final BillingRepository billingRepository;

    @Value("${paymybuddy.fee.percentage:0.5}")
    private BigDecimal feePercentage;

    @Autowired
    public TransactionService(
            TransactionRepository transactionRepository,
            UserAccountRepository userAccountRepository,
            ConnectionRepository connectionRepository,
            BillingRepository billingRepository) {
        this.transactionRepository = transactionRepository;
        this.userAccountRepository = userAccountRepository;
        this.connectionRepository = connectionRepository;
        this.billingRepository = billingRepository;
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> findBySender(UserAccount sender) {
        return transactionRepository.findBySender(sender);
    }

    public List<Transaction> findByReceiver(UserAccount receiver) {
        return transactionRepository.findByReceiver(receiver);
    }

    public List<Transaction> findByUser(UserAccount user) {
        return transactionRepository.findBySenderOrReceiverOrderByDateDesc(user, user);
    }

    @Transactional
    public Transaction makeTransaction(Long senderId, Long receiverId, BigDecimal amount, String description) {
        // Vérifier que le montant est positif
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro");
        }

        // Récupérer l'expéditeur et le destinataire
        UserAccount sender = userAccountRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Expéditeur non trouvé"));

        UserAccount receiver = userAccountRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Destinataire non trouvé"));

        // Vérifier que l'expéditeur et le destinataire sont connectés
        if (!connectionRepository.existsByOwnerAndFriend(sender, receiver)) {
            throw new IllegalArgumentException("Vous n'êtes pas connecté à cet utilisateur");
        }

        // Calculer les frais
        BigDecimal fee = Billing.calculateFee(amount, feePercentage);
        BigDecimal totalAmount = amount.add(fee);

        // Vérifier que l'expéditeur a assez de fonds
        if (sender.getBalance().compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException("Solde insuffisant pour cette transaction");
        }

        // Mettre à jour les soldes
        sender.setBalance(sender.getBalance().subtract(totalAmount));
        receiver.setBalance(receiver.getBalance().add(amount));

        userAccountRepository.save(sender);
        userAccountRepository.save(receiver);

        // Créer la transaction
        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setDate(LocalDateTime.now());
        transaction.setFee(fee);

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Enregistrer la facturation
        Billing billing = new Billing();
        billing.setTransaction(savedTransaction);
        billing.setAmount(fee);
        billing.setDate(LocalDateTime.now());
        billing.setProcessed(false);
        billing.setFeePercentage(feePercentage);
        billing.setDescription("Frais de transaction pour le transfert de " + amount + " à " + receiver.getEmail());

        billingRepository.save(billing);

        return savedTransaction;
    }

    @Transactional
    public Transaction depositFunds(Long userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        user.setBalance(user.getBalance().add(amount));
        userAccountRepository.save(user);

        Transaction transaction = new Transaction();
        transaction.setReceiver(user);
        transaction.setSender(user); // Pour un dépôt, on peut considérer que l'utilisateur est aussi l'expéditeur
        transaction.setAmount(amount);
        transaction.setDescription("Dépôt: " + description);
        transaction.setDate(LocalDateTime.now());
        transaction.setFee(BigDecimal.ZERO); // Pas de frais pour un dépôt

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction withdrawFunds(Long userId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro");
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (user.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Solde insuffisant pour ce retrait");
        }

        user.setBalance(user.getBalance().subtract(amount));
        userAccountRepository.save(user);

        Transaction transaction = new Transaction();
        transaction.setSender(user);
        transaction.setReceiver(user); // Pour un retrait, on peut considérer que l'utilisateur est aussi le destinataire
        transaction.setAmount(amount);
        transaction.setDescription("Retrait: " + description);
        transaction.setDate(LocalDateTime.now());
        transaction.setFee(BigDecimal.ZERO); // On pourrait appliquer des frais pour un retrait si nécessaire

        return transactionRepository.save(transaction);
    }
}
