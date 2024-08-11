-- 修改用戶表
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 修改錢包表，改名為accounts
CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) REFERENCES users(id),
    currency VARCHAR(10) NOT NULL,
    balance DECIMAL(18, 8) DEFAULT 0,
    available_balance DECIMAL(18, 8) DEFAULT 0,
    frozen_balance DECIMAL(18, 8) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_currency ON accounts(currency);

-- 交易表
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(50) PRIMARY KEY,
    account_id VARCHAR(50) REFERENCES accounts(id),
    amount DECIMAL(18, 8) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    client_transaction_id VARCHAR(50),
    transaction_type VARCHAR(10) CHECK (transaction_type IN ('deposit', 'withdrawal', 'transfer')),
    status VARCHAR(10) CHECK (status IN ('pending', 'completed', 'failed')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 創建一個觸發器，在插入新記錄時，如果 client_transaction_id 為空，則使用 id 的值
CREATE OR REPLACE FUNCTION set_default_client_transaction_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.client_transaction_id IS NULL THEN
        NEW.client_transaction_id := NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_default_client_transaction_id
BEFORE INSERT ON transactions
FOR EACH ROW
EXECUTE FUNCTION set_default_client_transaction_id();

-- 創建複合索引來確保 client_transaction_id 在每個賬戶中是唯一的（當它不為空時）
CREATE UNIQUE INDEX idx_account_client_transaction ON transactions(account_id, client_transaction_id) 
WHERE client_transaction_id IS NOT NULL;

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_client_transaction_id ON transactions(client_transaction_id);

-- 新增內部轉帳表
CREATE TABLE IF NOT EXISTS transfers (
    id VARCHAR(50) PRIMARY KEY,
    from_account_id VARCHAR(50) REFERENCES accounts(id),
    to_account_id VARCHAR(50) REFERENCES accounts(id),
    amount DECIMAL(18, 8) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(10) CHECK (status IN ('pending', 'completed', 'failed')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transfers_from_account_id ON transfers(from_account_id);
CREATE INDEX idx_transfers_to_account_id ON transfers(to_account_id);
CREATE INDEX idx_transfers_created_at ON transfers(created_at);

-- 新增充值地址表
CREATE TABLE IF NOT EXISTS deposit_addresses (
    id VARCHAR(50) PRIMARY KEY,
    account_id VARCHAR(50) REFERENCES accounts(id),
    currency VARCHAR(10) NOT NULL,
    address VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_deposit_addresses_account_id ON deposit_addresses(account_id);
CREATE INDEX idx_deposit_addresses_address ON deposit_addresses(address);

-- 新增黑名單表
CREATE TABLE IF NOT EXISTS blacklist (
    id VARCHAR(50) PRIMARY KEY,
    account_id VARCHAR(50) REFERENCES accounts(id),
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_blacklist_account_id ON blacklist(account_id);

-- 關於時序問題：
-- 在多重操作下，確實可能會出現時序問題，尤其是在更新餘額時。為了避免這些問題，我們可以使用數據庫的事務（Transaction）和鎖（Lock）機制。
-- 以下是一個原子操作函數的例子，用於更新帳戶餘額：

CREATE OR REPLACE FUNCTION update_account_balance(
    p_account_id VARCHAR(50),
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
                RAISE EXCEPTION 'Insufficient funds';
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
