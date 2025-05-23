# PayMyBuddy
## Physical Data Model

The physical data model below represents the database structure used by the PayMyBuddy application.

```mermaid
erDiagram
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
    
    USER ||--o{ BANK_ACCOUNT : owns
    USER ||--o{ CONNECTION : is_owner
    USER ||--o{ CONNECTION : is_friend
    USER ||--o{ TRANSACTION : sends
    USER ||--o{ TRANSACTION : receives
    USER ||--o{ BANK_TRANSFER : makes
    BANK_ACCOUNT ||--o{ BANK_TRANSFER : relates_to
    TRANSACTION ||--o{ BILLING : generates
```

### Table Descriptions

- **USER**: Stores information about users registered on the platform.
- **BANK_ACCOUNT**: Contains information about bank accounts linked to users.
- **CONNECTION**: Represents relationships between users (friends/contacts).
- **TRANSACTION**: Records all transactions made between users.
- **BANK_TRANSFER**: Tracks money transfers between users' bank accounts and the application.
- **BILLING**: Manages billing related to transactions made on the platform.

### Main Relationships

- A user can have multiple bank accounts
- A user can have multiple connections with other users
- A user can send/receive multiple transactions
- A user can make multiple bank transfers
- Each bank transfer is linked to a specific bank account
- Each transaction can generate one or more billings
