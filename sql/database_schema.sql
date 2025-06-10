-- PayMyBuddy Database Schema (PostgreSQL version)

-- Drop tables if they exist to allow for clean initialization
DROP TABLE IF EXISTS billing;
DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS connection;
DROP TABLE IF EXISTS user_account;

-- Create User Account table
CREATE TABLE user_account (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0
);

-- Create Connection table
CREATE TABLE connection (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES user_account(id),
    FOREIGN KEY (friend_id) REFERENCES user_account(id),
    UNIQUE (owner_id, friend_id)
);

-- Create Transaction table
CREATE TABLE transaction (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    fee DECIMAL(19, 2) NOT NULL,
    description VARCHAR(255),
    date TIMESTAMP NOT NULL,
    FOREIGN KEY (sender_id) REFERENCES user_account(id),
    FOREIGN KEY (receiver_id) REFERENCES user_account(id)
);

-- Create Billing table
CREATE TABLE billing (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    date TIMESTAMP NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    fee_percentage DECIMAL(5, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES transaction(id)
);

-- Create indexes for performance
CREATE INDEX idx_user_email ON user_account(email);
CREATE INDEX idx_transaction_date ON transaction(date);
CREATE INDEX idx_billing_date ON billing(date);
