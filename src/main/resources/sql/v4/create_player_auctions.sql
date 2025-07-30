CREATE TABLE IF NOT EXISTS player_auctions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_data TEXT NOT NULL,
    owner_uuid BINARY(16),
    min_bid INTEGER DEFAULT 0,
    deposit INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    auction_start TIMESTAMP NULL,
    highest_bid INTEGER DEFAULT 0,
    highest_bidder_uuid BINARY(16)
);