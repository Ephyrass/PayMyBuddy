# PayMyBuddy
## Modèle Physique de Données

Le modèle physique de données ci-dessous représente la structure de la base de données utilisée par l'application PayMyBuddy.

```mermaid
MDP
    USER {
        long id PK
        string email
        string password
        string firstName
        string lastName
        decimal balance
        datetime createdDate
    }
    
    BANK_ACCOUNT {
        long id PK
        long user_id FK
        string accountName
        string iban
        datetime createdDate
    }
    
    CONNECTION {
        long id PK
        long owner_id FK
        long friend_id FK
        datetime createdDate
    }
    
    TRANSACTION {
        long id PK
        long sender_id FK
        long receiver_id FK
        decimal amount
        decimal fee
        string description
        datetime date
    }
    
    BANK_TRANSFER {
        long id PK
        long user_id FK
        long bank_account_id FK
        decimal amount
        string type
        datetime date
    }
    
    BILLING {
        long id PK
        long transaction_id FK
        decimal amount
        datetime date
        boolean processed
    }
    
    USER ||--o{ BANK_ACCOUNT : possède
    USER ||--o{ CONNECTION : est_propriétaire
    USER ||--o{ CONNECTION : est_ami
    USER ||--o{ TRANSACTION : envoie
    USER ||--o{ TRANSACTION : reçoit
    USER ||--o{ BANK_TRANSFER : effectue
    BANK_ACCOUNT ||--o{ BANK_TRANSFER : concerne
    TRANSACTION ||--o{ BILLING : génère
```

### Description des tables

- **USER**: Stocke les informations des utilisateurs inscrits sur la plateforme.
- **BANK_ACCOUNT**: Contient les informations des comptes bancaires reliés aux utilisateurs.
- **CONNECTION**: Représente les relations entre utilisateurs (amis/contacts).
- **TRANSACTION**: Enregistre toutes les transactions effectuées entre utilisateurs.
- **BANK_TRANSFER**: Trace les transferts d'argent entre les comptes bancaires des utilisateurs et l'application.
- **BILLING**: Gère la facturation liée aux transactions effectuées sur la plateforme.

### Relations principales

- Un utilisateur peut avoir plusieurs comptes bancaires
- Un utilisateur peut avoir plusieurs connexions avec d'autres utilisateurs
- Un utilisateur peut effectuer/recevoir plusieurs transactions
- Un utilisateur peut réaliser plusieurs transferts bancaires
- Chaque transfert bancaire est lié à un compte bancaire spécifique
- Chaque transaction peut générer une ou plusieurs facturations
