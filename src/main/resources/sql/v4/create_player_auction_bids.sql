CREATE TABLE IF NOT EXISTS auction_bids (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_auction_id INTEGER NOT NULL,
    player_uuid BINARY(16) NOT NULL,
    bid_amount INTEGER NOT NULL,
    FOREIGN KEY (player_auction_id) REFERENCES player_auctions(id) ON DELETE CASCADE,
    CONSTRAINT unique_bid UNIQUE (player_auction_id, player_uuid)
);
