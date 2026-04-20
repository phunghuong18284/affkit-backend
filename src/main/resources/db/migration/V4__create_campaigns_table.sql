CREATE TABLE campaigns (
                           id          UUID         NOT NULL DEFAULT gen_random_uuid(),
                           user_id     UUID         NOT NULL,
                           name        VARCHAR(255) NOT NULL,
                           description TEXT,
                           start_date  DATE,
                           end_date    DATE,
                           is_archived BOOLEAN      NOT NULL DEFAULT false,
                           created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           is_deleted  BOOLEAN      NOT NULL DEFAULT false,
                           deleted_at  TIMESTAMPTZ,
                           CONSTRAINT campaigns_dates_check CHECK (
                               end_date IS NULL OR start_date IS NULL OR end_date >= start_date
                               ),
                           CONSTRAINT campaigns_name_not_empty CHECK (TRIM(name) <> ''),
                           PRIMARY KEY (id),
                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);