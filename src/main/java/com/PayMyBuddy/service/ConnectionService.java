package com.PayMyBuddy.service;

import com.PayMyBuddy.model.Connection;
import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.repository.ConnectionRepository;
import com.PayMyBuddy.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserAccountRepository userAccountRepository;

    @Autowired
    public ConnectionService(ConnectionRepository connectionRepository, UserAccountRepository userAccountRepository) {
        this.connectionRepository = connectionRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<Connection> findAll() {
        return connectionRepository.findAll();
    }

    public List<Connection> findByOwner(UserAccount owner) {
        return connectionRepository.findByOwner(owner);
    }

    public List<Connection> findByOwnerId(Long ownerId) {
        UserAccount owner = userAccountRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found"));
        return connectionRepository.findByOwner(owner);
    }

    public Optional<Connection> findById(Long id) {
        return connectionRepository.findById(id);
    }

    @Transactional
    public Connection createConnection(Long ownerId, Long friendId) {
        // Vérifier que les deux utilisateurs existent
        UserAccount owner = userAccountRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found"));

        UserAccount friend = userAccountRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Friend user not found"));

        // Check if the connection already exists
        if (connectionRepository.existsByOwnerAndFriend(owner, friend)) {
            throw new IllegalArgumentException("This connection already exists");
        }

        // Check if the user is trying to connect to themselves
        if (ownerId.equals(friendId)) {
            throw new IllegalArgumentException("You cannot connect to yourself");
        }

        Connection connection = new Connection();
        connection.setOwner(owner);
        connection.setFriend(friend);

        return connectionRepository.save(connection);
    }

    @Transactional
    public void deleteConnection(Long id) {
        connectionRepository.deleteById(id);
    }

    @Transactional
    public void deleteConnectionBetweenUsers(Long ownerId, Long friendId) {
        UserAccount owner = userAccountRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner user not found"));

        UserAccount friend = userAccountRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Friend user not found"));

        Optional<Connection> connection = connectionRepository.findByOwnerAndFriend(owner, friend);

        if (connection.isPresent()) {
            connectionRepository.delete(connection.get());
        } else {
            throw new IllegalArgumentException("The connection does not exist");
        }
    }

}
