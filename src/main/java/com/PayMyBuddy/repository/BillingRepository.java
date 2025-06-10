package com.PayMyBuddy.repository;

import com.PayMyBuddy.model.Billing;
import com.PayMyBuddy.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {
    List<Billing> findByProcessed(Boolean processed);
    List<Billing> findByDateBetween(LocalDateTime start, LocalDateTime end);
    List<Billing> findByTransaction(Transaction transaction);
}
