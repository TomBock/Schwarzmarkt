CREATE TABLE IF NOT EXISTS player_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_uuid BINARY(16),
    item_data TEXT NOT NULL,
    deposit INT DEFAULT 1,
    min_bid INT DEFAULT 0
);