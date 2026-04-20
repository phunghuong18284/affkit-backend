CREATE TABLE users (
                       id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email           VARCHAR(255) NOT NULL,
                       full_name       VARCHAR(255) NOT NULL,
                       password_hash   VARCHAR(255) NOT NULL,
                       plan            VARCHAR(20)  NOT NULL DEFAULT 'FREE',
                       is_verified     BOOLEAN      NOT NULL DEFAULT false,
                       is_locked       BOOLEAN      NOT NULL DEFAULT false,
                       locked_until    TIMESTAMPTZ,
                       failed_attempts SMALLINT     NOT NULL DEFAULT 0,
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                       CONSTRAINT users_email_unique UNIQUE (email),
                       CONSTRAINT users_plan_check
                           CHECK (plan IN ('FREE', 'PRO', 'BUSINESS', 'ENTERPRISE'))
);