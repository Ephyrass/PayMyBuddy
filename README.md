# PayMyBuddy
## Physical Data Model

The physical data model below represents the database structure used by the PayMyBuddy application.

```mermaid
erDiagram
    USER_ACCOUNT {
        long id PK
        string email
        string password
        string firstName
        string lastName
    }
    
    CONNECTION {
        long id PK
        long owner_id FK
        long friend_id FK
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
    
    BILLING {
        long id PK
        long transaction_id FK
        decimal amount
        datetime date
        boolean processed
        decimal feePercentage
        string description
    }
    
    USER_ACCOUNT ||--o{ CONNECTION : is_owner
    USER_ACCOUNT ||--o{ CONNECTION : is_friend
    USER_ACCOUNT ||--o{ TRANSACTION : sends
    USER_ACCOUNT ||--o{ TRANSACTION : receives
    TRANSACTION ||--o{ BILLING : generates
```

### Table Descriptions

- **USER_ACCOUNT**: Stores information about users registered on the platform.
- **CONNECTION**: Represents relationships between users (friends/contacts).
- **TRANSACTION**: Records all transactions made between users.
- **BILLING**: Manages billing related to transactions made on the platform.

### Main Relationships

- A user can have multiple connections with other users
- A user can send/receive multiple transactions
- Each transaction can generate one or more billings
