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
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur propriétaire non trouvé"));
        return connectionRepository.findByOwner(owner);
    }

    public Optional<Connection> findById(Long id) {
        return connectionRepository.findById(id);
    }

    @Transactional
    public Connection createConnection(Long ownerId, Long friendId) {
        // Vérifier que les deux utilisateurs existent
        UserAccount owner = userAccountRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur propriétaire non trouvé"));

        UserAccount friend = userAccountRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur ami non trouvé"));

        // Vérifier que la connexion n'existe pas déjà
        if (connectionRepository.existsByOwnerAndFriend(owner, friend)) {
            throw new IllegalArgumentException("Cette connexion existe déjà");
        }

        // Vérifier que l'utilisateur n'essaie pas de se connecter à lui-même
        if (ownerId.equals(friendId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous connecter à vous-même");
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
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur propriétaire non trouvé"));

        UserAccount friend = userAccountRepository.findById(friendId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur ami non trouvé"));

        Optional<Connection> connection = connectionRepository.findByOwnerAndFriend(owner, friend);

        if (connection.isPresent()) {
            connectionRepository.delete(connection.get());
        } else {
            throw new IllegalArgumentException("La connexion n'existe pas");
        }
    }
}
