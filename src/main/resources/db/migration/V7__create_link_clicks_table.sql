CREATE TABLE link_clicks (
                             id          BIGSERIAL    PRIMARY KEY,
                             link_id     UUID         NOT NULL REFERENCES links(id) ON DELETE CASCADE,
                             clicked_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                             source      VARCHAR(50),
                             referrer    TEXT,
                             ip_hash     VARCHAR(64),
                             user_agent  TEXT,
                             device_type VARCHAR(20),
                             country     VARCHAR(2),
                             is_bot      BOOLEAN      NOT NULL DEFAULT false,
                             CONSTRAINT link_clicks_device_check CHECK (
                                 device_type IS NULL OR device_type IN ('MOBILE','DESKTOP','TABLET')
                                 )
);