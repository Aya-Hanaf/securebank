-- ─────────────────────────────────────────────────────────────────────────────
-- SecureBank — initial schema
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Accounts ──────────────────────────────────────────────────────────────────
CREATE TABLE accounts (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number  VARCHAR(20)    NOT NULL UNIQUE,
    type            VARCHAR(20)    NOT NULL,
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0.0000,
    currency        VARCHAR(3)     NOT NULL DEFAULT 'EUR',
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    user_id         UUID           NOT NULL REFERENCES users (id),
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Transactions ──────────────────────────────────────────────────────────────
CREATE TABLE transactions (
    id                     UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    reference              VARCHAR(50)    NOT NULL UNIQUE,
    type                   VARCHAR(20)    NOT NULL,
    amount                 NUMERIC(19, 4) NOT NULL,
    description            VARCHAR(255),
    status                 VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    account_id             UUID           NOT NULL REFERENCES accounts (id),
    target_account_number  VARCHAR(20),
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── Indexes ───────────────────────────────────────────────────────────────────
CREATE INDEX idx_accounts_user_id         ON accounts     (user_id);
CREATE INDEX idx_accounts_status          ON accounts     (status);
CREATE INDEX idx_transactions_account_id  ON transactions (account_id);
CREATE INDEX idx_transactions_reference   ON transactions (reference);
CREATE INDEX idx_transactions_status      ON transactions (status);
CREATE INDEX idx_transactions_created_at  ON transactions (created_at DESC);

-- ── Auto-update updated_at ────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
