CREATE TABLE IF NOT EXISTS auction_bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    auction_id INTEGER NOT NULL,
    player_uuid BINARY(16) NOT NULL,
    bid_amount INTEGER NOT NULL,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    CONSTRAINT unique_bid UNIQUE (auction_id, player_uuid)
);
