package com.PayMyBuddy.repository;

import com.PayMyBuddy.model.Connection;
import com.PayMyBuddy.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    List<Connection> findByOwner(UserAccount owner);
    Optional<Connection> findByOwnerAndFriend(UserAccount owner, UserAccount friend);
    boolean existsByOwnerAndFriend(UserAccount owner, UserAccount friend);
}
