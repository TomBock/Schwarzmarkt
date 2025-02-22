CREATE TABLE IF NOT EXISTS auctions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_data TEXT NOT NULL,
    highest_bid INTEGER DEFAULT 0,
    highest_bidder_uuid BINARY(16)
);