CREATE TABLE IF NOT EXISTS player_auctions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    item_data TEXT NOT NULL,
    owner_uuid BINARY(16),
    owner_name TEXT DEFAULT '',
    deposit INTEGER DEFAULT 0,
    min_bid INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    highest_bid INTEGER DEFAULT 0,
    highest_bidder_uuid BINARY(16),
    FOREIGN KEY (item_id) REFERENCES player_items(id),
    CONSTRAINT unique_item_per_auction UNIQUE (item_id, id)
);

-- Dummy insert to ensure sqlite_sequence entry is created
INSERT INTO player_auctions (item_id, item_data, owner_uuid)
VALUES (0, '{}', X'00000000000000000000000000000000');

-- Remove dummy row again
DELETE FROM player_auctions WHERE item_id = 0;

-- Set autoincrement start to 1,000,000
INSERT INTO sqlite_sequence (name, seq)
VALUES ('player_auctions', 999999)
ON CONFLICT(name) DO UPDATE SET seq = 999999;