-- ===========================================
-- DATABASE CREATION
-- ===========================================
CREATE DATABASE IF NOT EXISTS digitalWalletApp;
USE digitalWalletApp;

-- ===========================================
-- TABLE 1: userDetails
-- ===========================================
CREATE TABLE IF NOT EXISTS userDetails (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,           -- Primary Key, unique user identifier
    name VARCHAR(100) NOT NULL,                     -- User's name
    email VARCHAR(150) NOT NULL,                    -- User email
    password VARCHAR(255) NOT NULL,                -- Password
    role VARCHAR(50) NOT NULL DEFAULT 'USER',      -- Default role as USER
    UNIQUE KEY idx_user_email (email)             -- Index on email (UNIQUE)
    );
-- UNIQUE KEY idx_user_email (email) → Ensures no two users can have the same email, also creates an index for faster lookup.

-- ===========================================
-- TABLE 2: walletDetails
-- ===========================================
CREATE TABLE IF NOT EXISTS walletDetails (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,          -- Wallet ID
                                             balance DOUBLE DEFAULT 0.0,                   -- Current balance
                                             dailySpent DOUBLE DEFAULT 0.0,                -- Daily spent amount
                                             frozen BOOLEAN DEFAULT FALSE,                 -- Wallet frozen status
                                             lastTransactionDate DATE,                      -- Last transaction date
                                             user_id BIGINT NOT NULL,                       -- Foreign key to userDetails
                                             CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES userDetails(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_wallet_user (user_id)
    );
-- INDEX idx_wallet_user → Creates an index on the user_id column for faster searches.

-- ===========================================
-- TABLE 3: transactionDetails
-- ===========================================
CREATE TABLE IF NOT EXISTS transactionDetails (
                                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,          -- Transaction ID
                                                  amount DOUBLE NOT NULL,                        -- Transaction amount
                                                  type VARCHAR(10) NOT NULL,                     -- Type: CREDIT or DEBIT
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,  -- Transaction timestamp
    user_id BIGINT NOT NULL,                       -- Foreign key to userDetails
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES userDetails(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_transaction_user (user_id),
    INDEX idx_transaction_timestamp (timestamp)
    );
TODO WHICH INDEX PERFORMS BETTER IN NON-PK, NON UNIQUE COLUMNS.

/*
Creates an index on user_id.
Speeds up queries like: “find all transactions for a given user.”
 */

-- ===========================================
-- DML: Insert default users (idempotent)
-- ===========================================

INSERT INTO userDetails (name, email, password)
SELECT 'John Doe', 'john@example.com', 'pass123'
    WHERE NOT EXISTS (SELECT 1 FROM userDetails WHERE email='john@example.com');

INSERT INTO userDetails (name, email, password)
SELECT 'Alice Smith', 'alice@example.com', 'alice123'
    WHERE NOT EXISTS (SELECT 1 FROM userDetails WHERE email='alice@example.com');

INSERT INTO userDetails (name, email, password)
SELECT 'Bob Johnson', 'bob@example.com', 'bob123'
    WHERE NOT EXISTS (SELECT 1 FROM userDetails WHERE email='bob@example.com');

INSERT INTO userDetails (name, email, password)
SELECT 'Charlie Brown', 'charlie@example.com', 'charlie123'
    WHERE NOT EXISTS (SELECT 1 FROM userDetails WHERE email='charlie@example.com');

INSERT INTO userDetails (name, email, password)
SELECT 'Diana Prince', 'diana@example.com', 'diana123'
    WHERE NOT EXISTS (SELECT 1 FROM userDetails WHERE email='diana@example.com');

INSERT INTO userDetails (name, email, password)
SELECT 'Ethan Hunt', 'ethan@example.com', 'ethan123'
    WHERE NOT EXISTS (SELECT 1 FROM userDetails WHERE email='ethan@example.com');

-- ===========================================
-- DML: Insert wallets for users (idempotent)
-- ===========================================
INSERT INTO walletDetails (balance, dailySpent, frozen, lastTransactionDate, user_id)
SELECT 1000.0, 0.0, FALSE, CURRENT_DATE(), id
FROM userDetails
WHERE email='john@example.com'
  AND NOT EXISTS (SELECT 1 FROM walletDetails WHERE user_id = (SELECT id FROM userDetails WHERE email='john@example.com'));

INSERT INTO walletDetails (balance, dailySpent, frozen, lastTransactionDate, user_id)
SELECT 1000.0, 0.0, FALSE, CURRENT_DATE(), id
FROM userDetails
WHERE email='alice@example.com'
  AND NOT EXISTS (SELECT 1 FROM walletDetails WHERE user_id = (SELECT id FROM userDetails WHERE email='alice@example.com'));

INSERT INTO walletDetails (balance, dailySpent, frozen, lastTransactionDate, user_id)
SELECT 1200.0, 0.0, FALSE, CURRENT_DATE(), id
FROM userDetails
WHERE email='bob@example.com'
  AND NOT EXISTS (SELECT 1 FROM walletDetails WHERE user_id = (SELECT id FROM userDetails WHERE email='bob@example.com'));

INSERT INTO walletDetails (balance, dailySpent, frozen, lastTransactionDate, user_id)
SELECT 1500.0, 0.0, FALSE, CURRENT_DATE(), id
FROM userDetails
WHERE email='charlie@example.com'
  AND NOT EXISTS (SELECT 1 FROM walletDetails WHERE user_id = (SELECT id FROM userDetails WHERE email='charlie@example.com'));

INSERT INTO walletDetails (balance, dailySpent, frozen, lastTransactionDate, user_id)
SELECT 2000.0, 0.0, FALSE, CURRENT_DATE(), id
FROM userDetails
WHERE email='diana@example.com'
  AND NOT EXISTS (SELECT 1 FROM walletDetails WHERE user_id = (SELECT id FROM userDetails WHERE email='diana@example.com'));

INSERT INTO walletDetails (balance, dailySpent, frozen, lastTransactionDate, user_id)
SELECT 1800.0, 0.0, FALSE, CURRENT_DATE(), id
FROM userDetails
WHERE email='ethan@example.com'
  AND NOT EXISTS (SELECT 1 FROM walletDetails WHERE user_id = (SELECT id FROM userDetails WHERE email='ethan@example.com'));

-- ===========================================
-- DML: Insert initial transactions (idempotent)
-- ===========================================
INSERT INTO transactionDetails (amount, type, user_id)
SELECT 500.0, 'CREDIT', id
FROM userDetails
WHERE email='john@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM transactionDetails
    WHERE user_id=(SELECT id FROM userDetails WHERE email='john@example.com')
      AND amount=500.0 AND type='CREDIT'
);

-- Repeat similarly for other users:
INSERT INTO transactionDetails (amount, type, user_id)
SELECT 500.0, 'CREDIT', id
FROM userDetails
WHERE email='alice@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM transactionDetails
    WHERE user_id=(SELECT id FROM userDetails WHERE email='alice@example.com')
      AND amount=500.0 AND type='CREDIT'
);

INSERT INTO transactionDetails (amount, type, user_id)
SELECT 1200.0, 'CREDIT', id
FROM userDetails
WHERE email='bob@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM transactionDetails
    WHERE user_id=(SELECT id FROM userDetails WHERE email='bob@example.com')
      AND amount=1200.0 AND type='CREDIT'
);

INSERT INTO transactionDetails (amount, type, user_id)
SELECT 1500.0, 'CREDIT', id
FROM userDetails
WHERE email='charlie@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM transactionDetails
    WHERE user_id=(SELECT id FROM userDetails WHERE email='charlie@example.com')
      AND amount=1500.0 AND type='CREDIT'
);

INSERT INTO transactionDetails (amount, type, user_id)
SELECT 2000.0, 'CREDIT', id
FROM userDetails
WHERE email='diana@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM transactionDetails
    WHERE user_id=(SELECT id FROM userDetails WHERE email='diana@example.com')
      AND amount=2000.0 AND type='CREDIT'
);

INSERT INTO transactionDetails (amount, type, user_id)
SELECT 1800.0, 'CREDIT', id
FROM userDetails
WHERE email='ethan@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM transactionDetails
    WHERE user_id=(SELECT id FROM userDetails WHERE email='ethan@example.com')
      AND amount=1800.0 AND type='CREDIT'
);

-- ===========================================
-- DML: CRUD Examples
-- ===========================================

-- READ
SELECT u.id, u.name, u.email, w.balance
FROM userDetails u
         JOIN walletDetails w ON u.id = w.user_id
WHERE u.email = 'john@example.com';

-- UPDATE
UPDATE walletDetails SET balance = balance + 200
WHERE user_id = (SELECT id FROM userDetails WHERE email='john@example.com');

-- DELETE
-- DELETE FROM transactionDetails WHERE id = 1;

-- ===========================================
-- ACID Transaction Example
-- ===========================================
START TRANSACTION;
/*
Starts a new transaction block.
All the queries after this line are treated as a single unit of work.
Nothing is permanently saved until you run COMMIT.
ACID aspect: Atomicity — either all operations succeed or none.
 */
UPDATE walletDetails SET balance = balance - 100
WHERE user_id = (SELECT id FROM userDetails WHERE email='john@example.com');
/*
Deducts 100 from John Doe’s wallet.
Uses a subquery to fetch the user_id based on email.
Part of the transaction — this will not be permanently applied until committed.
ACID aspect: Consistency — ensures wallet balance is updated properly in relation to the transaction.
 */
INSERT INTO transactionDetails (amount, type, user_id)
SELECT 100.0, 'DEBIT', id
FROM userDetails
WHERE email='john@example.com';
/*
Records the deduction as a DEBIT transaction in transactionDetails.
Links it to the same user using their id.
ACID aspect: Isolation — ensures this transaction is applied independently and will not conflict with other concurrent operations.
*/
COMMIT;
/*
Finalizes the transaction.
All changes (wallet deduction + transaction record) are now permanently saved to the database.
ACID aspect: Durability — once committed, even if the database crashes, these changes persist.
*/

    TODO, CHECK THE -VE CONDITION FOR ACID


-- ===========================================
-- DCL: Create user & grant privileges
-- ===========================================
CREATE USER IF NOT EXISTS 'walletUser'@'localhost' IDENTIFIED BY 'walletPass';
GRANT SELECT, INSERT, UPDATE, DELETE ON digitalWalletApp.* TO 'walletUser'@'localhost';
FLUSH PRIVILEGES;

      TODO, GO MORE IN DETAIL ABOUT FLUSH PRIVILEGES.


-- ===========================================
-- VERIFICATION QUERIES
-- ===========================================

-- 1️⃣ Show all users
SELECT * FROM userDetails;

-- 2️⃣ Show all wallets with user names and balances
SELECT u.id AS user_id, u.name, w.balance, w.dailySpent, w.frozen, w.lastTransactionDate
FROM walletDetails w
         JOIN userDetails u ON u.id = w.user_id;

-- 3️⃣ Show all transactions with user names
SELECT t.id AS transaction_id, u.name AS user_name, t.amount, t.type, t.timestamp
FROM transactionDetails t
         JOIN userDetails u ON u.id = t.user_id
ORDER BY t.timestamp ASC;

-- 4️⃣ Example: Check balance for a specific user (John Doe)
SELECT u.id, u.name, u.email, w.balance
FROM userDetails u
         JOIN walletDetails w ON u.id = w.user_id
WHERE u.email = 'alice@example.com';

-- 5️⃣ Count total number of users, wallets, and transactions
SELECT
    (SELECT COUNT(*) FROM userDetails) AS total_users,
    (SELECT COUNT(*) FROM walletDetails) AS total_wallets,
    (SELECT COUNT(*) FROM transactionDetails) AS total_transactions;


