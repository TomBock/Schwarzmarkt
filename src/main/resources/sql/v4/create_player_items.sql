CREATE TABLE IF NOT EXISTS player_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_uuid BINARY(16),
    item_data TEXT NOT NULL,
    amount INT DEFAULT 1
);