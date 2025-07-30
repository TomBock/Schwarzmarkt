CREATE TABLE IF NOT EXISTS player_auctions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    item_data TEXT NOT NULL,
    owner_uuid BINARY(16),
    min_bid INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    highest_bid INTEGER DEFAULT 0,
    highest_bidder_uuid BINARY(16),
    FOREIGN KEY (item_id) REFERENCES player_items(id),
    CONSTRAINT unique_item_per_auction UNIQUE (item_id, id);
);