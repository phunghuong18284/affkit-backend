CREATE TABLE campaigns (
                           id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                           user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           name        VARCHAR(255) NOT NULL,
                           description TEXT,
                           start_date  DATE,
                           end_date    DATE,
                           is_archived BOOLEAN      NOT NULL DEFAULT false,
                           created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           CONSTRAINT campaigns_name_not_empty CHECK (TRIM(name) <> ''),
                           CONSTRAINT campaigns_dates_check CHECK (
                               end_date IS NULL OR start_date IS NULL OR end_date >= start_date
                               )
);