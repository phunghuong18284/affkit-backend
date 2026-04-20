CREATE TABLE link_tags (
                           id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                           link_id    UUID        NOT NULL REFERENCES links(id) ON DELETE CASCADE,
                           tag        VARCHAR(50) NOT NULL,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           CONSTRAINT link_tags_unique UNIQUE (link_id, tag),
                           CONSTRAINT link_tags_not_empty CHECK (TRIM(tag) <> '')
);