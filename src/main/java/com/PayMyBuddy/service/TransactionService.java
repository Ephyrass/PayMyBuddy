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

        // Créer la transaction (sans modification des soldes)
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
}
