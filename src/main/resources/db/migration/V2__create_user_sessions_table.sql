CREATE TABLE user_sessions (
                               id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               refresh_token VARCHAR(255) NOT NULL,
                               device_info   VARCHAR(500),
                               ip_address    VARCHAR(45),
                               is_revoked    BOOLEAN      NOT NULL DEFAULT false,
                               expires_at    TIMESTAMPTZ  NOT NULL,
                               last_used_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                               created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                               CONSTRAINT sessions_refresh_token_unique UNIQUE (refresh_token)
);