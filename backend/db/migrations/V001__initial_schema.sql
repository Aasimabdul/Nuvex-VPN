CREATE TABLE IF NOT EXISTS app_config (
    config_key TEXT PRIMARY KEY,
    config_value JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'deleted'))
);

CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_fingerprint_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ,
    UNIQUE (user_id, device_fingerprint_hash)
);

CREATE TABLE IF NOT EXISTS servers (
    id TEXT PRIMARY KEY,
    provider_id TEXT NOT NULL,
    country_code CHAR(2) NOT NULL,
    country_name TEXT NOT NULL,
    city TEXT,
    protocols TEXT[] NOT NULL DEFAULT '{}',
    available BOOLEAN NOT NULL DEFAULT false,
    checked_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS vpn_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    provider_id TEXT NOT NULL,
    provider_session_id TEXT NOT NULL,
    server_id TEXT REFERENCES servers(id),
    status TEXT NOT NULL CHECK (status IN ('creating', 'active', 'revoked', 'failed', 'expired')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS monthly_usage (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    month_utc DATE NOT NULL,
    bytes_up BIGINT NOT NULL DEFAULT 0 CHECK (bytes_up >= 0),
    bytes_down BIGINT NOT NULL DEFAULT 0 CHECK (bytes_down >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, month_utc)
);

CREATE TABLE IF NOT EXISTS provider_accounts (
    id UUID PRIMARY KEY,
    provider_id TEXT NOT NULL,
    external_user_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('active', 'suspended', 'revoked', 'unknown')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (provider_id, external_user_id)
);

CREATE TABLE IF NOT EXISTS provider_billing (
    id BIGSERIAL PRIMARY KEY,
    provider_id TEXT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    currency CHAR(3),
    estimated_amount NUMERIC(12, 2),
    actual_amount NUMERIC(12, 2),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_user_status ON vpn_sessions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_sessions_expiry ON vpn_sessions(expires_at);
