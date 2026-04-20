CREATE TABLE links (
                       id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       campaign_id  UUID         REFERENCES campaigns(id) ON DELETE SET NULL,
                       original_url TEXT         NOT NULL,
                       short_code   VARCHAR(12)  NOT NULL,
                       title        VARCHAR(255),
                       platform     VARCHAR(20),
                       click_count  BIGINT       NOT NULL DEFAULT 0,
                       is_deleted   BOOLEAN      NOT NULL DEFAULT false,
                       deleted_at   TIMESTAMPTZ,
                       created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       CONSTRAINT links_short_code_unique UNIQUE (short_code),
                       CONSTRAINT links_platform_check CHECK (
                           platform IS NULL OR platform IN ('SHOPEE','LAZADA','TIKI','TIKTOK','OTHER')
                           )
);