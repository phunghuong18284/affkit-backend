CREATE TABLE post_history (
                              id            BIGSERIAL PRIMARY KEY,
                              user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              product_url   TEXT NOT NULL,
                              product_name  TEXT,
                              product_price BIGINT,
                              product_image TEXT,
                              post_zalo     TEXT,
                              post_facebook TEXT,
                              post_telegram TEXT,
                              created_at    TIMESTAMP DEFAULT NOW()
);