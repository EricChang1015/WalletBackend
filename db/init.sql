-- 在 PostgreSQL 14 之後，可以直接使用 `pgcrypto` 擴展來生成 UUID
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- id: 假設使用UUID作為唯一標識能夠存儲UUID。
-- username: 保持唯一性，確保每個用戶名是唯一的。
-- password: 增加到 VARCHAR(255) 以便存儲更長的加密hash。
-- email: 加上 UNIQUE 約束，確保每個郵箱只對應一個賬戶。
-- email_verified: 用於標記該郵箱是否已經通過驗證。
-- email_verification_token: 用於存儲郵箱驗證時的token。
-- status: 用戶的狀態字段可以用來管理用戶的訪問權限或封禁狀態。
-- auth_provider 和 auth_provider_id: 如果你將來需要支持第三方身份驗證，這些字段將非常有用。
-- created_at: 紀錄用戶的註冊時間。

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- 儲存加密後的hash值
    email VARCHAR(100) UNIQUE DEFAULT NULL, -- 唯一性約束，確保每個郵箱只對應一個賬戶
    email_verified BOOLEAN DEFAULT FALSE, -- 確認郵箱是否驗證
    email_verification_token VARCHAR(255) DEFAULT NULL, -- 用於郵箱驗證的token
    status VARCHAR(20) DEFAULT 'active', -- 用戶狀態，如active, inactive, banned
    auth_provider VARCHAR(50) DEFAULT 'local', -- 認證提供者, 如 'local', 'google', 'facebook'
    auth_provider_id VARCHAR(100) DEFAULT NULL, -- 外部認證提供者的唯一ID, 不適用於本地認證
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP -- 用戶創建時間
);

-- id: 使用UUID 作為唯一標識符，確保全局唯一性。
-- user_id: 外鍵，參考 users 表中的 id 欄位。ON DELETE CASCADE 確保當用戶被刪除時，相關的帳戶也會被自動刪除。
-- currency: 支持多種貨幣，使用 VARCHAR(10) 可以支持大多數貨幣代碼。
-- balance: 帳戶的總餘額，這可能包括可用和凍結的部分。
-- available_balance: 可用餘額，即用戶可以立即使用的部分。
-- frozen_balance: 凍結餘額，即因某些原因（如待處理的提款）無法使用的部分。
-- created_at: 紀錄帳戶創建時間。
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    currency VARCHAR(10) NOT NULL, -- 例如 BTC, ETH, USD
    balance DECIMAL(18, 8) DEFAULT 0, -- 帳戶總餘額
    available_balance DECIMAL(18, 8) DEFAULT 0, -- 可用餘額
    frozen_balance DECIMAL(18, 8) DEFAULT 0, -- 凍結餘額
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- 帳戶創建時間
    UNIQUE (user_id, currency) -- 確保每個用戶對於每種貨幣只有一個帳戶
);
-- 為 user_id 和 currency 欄位設置索引以提升查詢性能
CREATE INDEX idx_user_currency ON accounts (user_id, currency);

-- id: 使用 UUID 作為唯一標識符。
-- account_id: 主要帳戶的外鍵，用於存款和取款操作。
-- from_account_id: 來源帳戶的外鍵，僅對於內部轉帳有效。
-- to_account_id: 目標帳戶的外鍵，僅對於內部轉帳有效。
-- amount: 記錄交易金額。
-- currency: 記錄交易的貨幣。
-- client_transaction_id: 存儲第三方的交易ID。如果沒有第三方ID，則使用系統生成的ID。
-- blockchain_transaction_id: 存儲區塊鍊上的交易ID（如交易哈希），主要用於存款。
-- blockchain: 記錄交易發生的區塊鍊名稱或標識符（如 Ethereum、Bitcoin），主要用於存款。
-- transaction_type: 記錄交易的類型（如存款、取款、轉賬）。
-- status: 記錄交易的狀態（如待處理、已完成、失敗）。
-- created_at: 記錄交易創建的時間。

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID REFERENCES accounts(id) ON DELETE CASCADE,
    from_account_id UUID REFERENCES accounts(id) ON DELETE CASCADE,
    to_account_id UUID REFERENCES accounts(id) ON DELETE CASCADE,
    amount DECIMAL(18, 8) NOT NULL, -- 交易金額
    currency VARCHAR(10) NOT NULL, -- 交易貨幣
    client_transaction_id VARCHAR(50), -- 第三方交易ID
    blockchain_transaction_id VARCHAR(100) DEFAULT NULL, -- 區塊鏈交易ID（對於存款）
    blockchain VARCHAR(50) DEFAULT NULL, -- 區塊鏈名稱或標識符（對於存款）
    transaction_type VARCHAR(10) CHECK (transaction_type IN ('deposit', 'withdrawal', 'transfer')), -- 交易類型
    status VARCHAR(10) CHECK (status IN ('pending', 'completed', 'failed')), -- 交易狀態
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP -- 交易創建時間
);

-- 創建觸發器，當插入新記錄時，如果 client_transaction_id 為空，則使用 id 的值
CREATE OR REPLACE FUNCTION set_default_client_transaction_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.client_transaction_id IS NULL THEN
        NEW.client_transaction_id := NEW.id::text;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_default_client_transaction_id
BEFORE INSERT ON transactions
FOR EACH ROW
EXECUTE FUNCTION set_default_client_transaction_id();

-- 創建索引來支持查詢性能
CREATE UNIQUE INDEX idx_transactions_client_transaction ON transactions(account_id, client_transaction_id)
WHERE client_transaction_id IS NOT NULL;

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_from_account_id ON transactions(from_account_id);
CREATE INDEX idx_transactions_to_account_id ON transactions(to_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_blockchain ON transactions(blockchain); -- 索引區塊鍊名稱

-- 關於時序問題：
-- 在多重操作下，確實可能會出現時序問題，尤其是在更新餘額時。為了避免這些問題，我們可以使用數據庫的事務（Transaction）和鎖（Lock）機制。
-- 以下是一個原子操作函數的例子，用於更新帳戶餘額：

CREATE OR REPLACE FUNCTION update_account_balance(
    p_account_id UUID,
    p_amount DECIMAL(18, 8),
    p_transaction_type VARCHAR(10)
)
RETURNS VOID AS $$
BEGIN
    -- 開始事務
    BEGIN
        -- 鎖定帳戶行，防止其他事務同時修改
        PERFORM * FROM accounts WHERE id = p_account_id FOR UPDATE;

        -- 更新餘額
        IF p_transaction_type = 'deposit' THEN
            UPDATE accounts
            SET balance = balance + p_amount,
                available_balance = available_balance + p_amount
            WHERE id = p_account_id;
        ELSIF p_transaction_type = 'withdrawal' THEN
            UPDATE accounts
            SET balance = balance - p_amount,
                available_balance = available_balance - p_amount
            WHERE id = p_account_id AND available_balance >= p_amount;

            IF NOT FOUND THEN
                RAISE EXCEPTION 'Insufficient funds for withdrawal';
            END IF;
        ELSE
            RAISE EXCEPTION 'Invalid transaction type';
        END IF;

        -- 如果沒有異常，事務將自動提交
    EXCEPTION
        WHEN OTHERS THEN
            -- 如果發生異常，回滾事務
            RAISE;
    END;
END;
$$ LANGUAGE plpgsql;

-- 透過這個function來處理transfer
CREATE OR REPLACE FUNCTION transfer_funds(
    p_from_account_id UUID,
    p_to_account_id UUID,
    p_amount DECIMAL(18, 8)
)
RETURNS VOID AS $$
BEGIN
    -- 開始事務
    BEGIN
        -- 鎖定來源帳戶和目標帳戶行
        PERFORM * FROM accounts WHERE id IN (p_from_account_id, p_to_account_id) FOR UPDATE;

        -- 更新來源帳戶的餘額
        UPDATE accounts
        SET balance = balance - p_amount,
            available_balance = available_balance - p_amount
        WHERE id = p_from_account_id AND available_balance >= p_amount;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Insufficient funds in source account';
        END IF;

        -- 更新目標帳戶的餘額
        UPDATE accounts
        SET balance = balance + p_amount,
            available_balance = available_balance + p_amount
        WHERE id = p_to_account_id;

        -- 如果沒有異常，事務將自動提交
    EXCEPTION
        WHEN OTHERS THEN
            -- 如果發生異常，回滾事務
            RAISE;
    END;
END;
$$ LANGUAGE plpgsql;
