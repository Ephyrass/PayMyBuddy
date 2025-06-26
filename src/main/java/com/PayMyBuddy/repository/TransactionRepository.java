package com.PayMyBuddy.repository;

import com.PayMyBuddy.model.Transaction;
import com.PayMyBuddy.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySender(UserAccount sender);
    List<Transaction> findByReceiver(UserAccount receiver);
    List<Transaction> findBySenderOrReceiverOrderByDateDesc(UserAccount sender, UserAccount receiver);
}
