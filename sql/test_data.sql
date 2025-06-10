-- Test data for PayMyBuddy application (PostgreSQL version)

-- Insert test users with bcrypt encoded passwords ('password123')
INSERT INTO user_account (email, password, first_name, last_name, balance) VALUES
('john.doe@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'John', 'Doe', 1000.00),
('jane.smith@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Jane', 'Smith', 750.50),
('bob.johnson@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Bob', 'Johnson', 500.25),
('alice.williams@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Alice', 'Williams', 1200.75),
('charlie.brown@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Charlie', 'Brown', 850.30),
('emma.davis@example.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Emma', 'Davis', 425.60);

-- Insert connections between users
INSERT INTO connection (owner_id, friend_id) VALUES
(1, 2), -- John -> Jane
(1, 3), -- John -> Bob
(1, 4), -- John -> Alice
(2, 3), -- Jane -> Bob
(2, 5), -- Jane -> Charlie
(3, 4), -- Bob -> Alice
(4, 5), -- Alice -> Charlie
(4, 6), -- Alice -> Emma
(5, 6); -- Charlie -> Emma

-- Insert transactions with 0.5% fee
INSERT INTO transaction (sender_id, receiver_id, amount, fee, description, date) VALUES
(1, 2, 100.00, 0.50, 'Dinner payment', '2023-05-15 18:30:00'),
(2, 1, 50.00, 0.25, 'Movie tickets', '2023-05-20 20:15:00'),
(1, 3, 75.50, 0.38, 'Shared gift', '2023-05-25 14:45:00'),
(3, 1, 120.00, 0.60, 'Weekend trip expenses', '2023-06-02 09:20:00'),
(4, 2, 85.25, 0.43, 'Concert tickets', '2023-06-05 16:30:00'),
(1, 4, 200.00, 1.00, 'Home repairs', '2023-06-10 11:45:00'),
(5, 3, 45.80, 0.23, 'Lunch', '2023-06-15 13:10:00'),
(4, 6, 150.00, 0.75, 'Birthday gift', '2023-06-20 17:25:00'),
(2, 5, 95.30, 0.48, 'Group dinner', '2023-06-25 19:40:00'),
(6, 4, 60.75, 0.30, 'Book club subscription', '2023-06-30 14:55:00');

-- Insert billings for each transaction
INSERT INTO billing (transaction_id, amount, date, processed, fee_percentage, description) VALUES
(1, 0.50, '2023-05-15 18:30:00', true, 0.5, 'Frais de transaction - Dinner payment'),
(2, 0.25, '2023-05-20 20:15:00', true, 0.5, 'Frais de transaction - Movie tickets'),
(3, 0.38, '2023-05-25 14:45:00', true, 0.5, 'Frais de transaction - Shared gift'),
(4, 0.60, '2023-06-02 09:20:00', true, 0.5, 'Frais de transaction - Weekend trip expenses'),
(5, 0.43, '2023-06-05 16:30:00', true, 0.5, 'Frais de transaction - Concert tickets'),
(6, 1.00, '2023-06-10 11:45:00', true, 0.5, 'Frais de transaction - Home repairs'),
(7, 0.23, '2023-06-15 13:10:00', true, 0.5, 'Frais de transaction - Lunch'),
(8, 0.75, '2023-06-20 17:25:00', false, 0.5, 'Frais de transaction - Birthday gift'),
(9, 0.48, '2023-06-25 19:40:00', false, 0.5, 'Frais de transaction - Group dinner'),
(10, 0.30, '2023-06-30 14:55:00', false, 0.5, 'Frais de transaction - Book club subscription');
