package com.PayMyBuddy.service;

import com.PayMyBuddy.model.Billing;
import com.PayMyBuddy.model.Transaction;
import com.PayMyBuddy.repository.BillingRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    private final BillingRepository billingRepository;

    @Autowired
    public BillingService(BillingRepository billingRepository) {
        this.billingRepository = billingRepository;
    }

    public List<Billing> findAll() {
        return billingRepository.findAll();
    }

    public Optional<Billing> findById(Long id) {
        return billingRepository.findById(id);
    }

    public List<Billing> findByProcessed(Boolean processed) {
        return billingRepository.findByProcessed(processed);
    }

    public List<Billing> findByDateBetween(LocalDateTime start, LocalDateTime end) {
        return billingRepository.findByDateBetween(start, end);
    }

    public List<Billing> findByTransaction(Transaction transaction) {
        return billingRepository.findByTransaction(transaction);
    }

    @Transactional
    public Billing save(Billing billing) {
        return billingRepository.save(billing);
    }

    @Transactional
    public Billing markAsProcessed(Long id) {
        Billing billing = billingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Facturation non trouvée"));

        billing.setProcessed(true);
        return billingRepository.save(billing);
    }

    @Transactional
    public void delete(Long id) {
        billingRepository.deleteById(id);
    }

    @Transactional
    public List<Billing> processUnprocessedBillings() {
        List<Billing> unprocessedBillings = billingRepository.findByProcessed(false);

        for (Billing billing : unprocessedBillings) {
            billing.setProcessed(true);
            billingRepository.save(billing);
        }

        return unprocessedBillings;
    }
}
