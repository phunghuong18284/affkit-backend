CREATE TABLE email_tokens (
                              id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              token      UUID        NOT NULL DEFAULT gen_random_uuid(),
                              type       VARCHAR(30) NOT NULL,
                              is_used    BOOLEAN     NOT NULL DEFAULT false,
                              expires_at TIMESTAMPTZ NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              CONSTRAINT email_tokens_token_unique UNIQUE (token),
                              CONSTRAINT email_tokens_type_check CHECK (type IN ('EMAIL_VERIFY','PASSWORD_RESET'))
);