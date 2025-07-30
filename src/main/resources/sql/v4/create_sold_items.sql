CREATE TABLE IF NOT EXISTS sold_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,)
    owner_uuid BINARY(16),
    price INT DEFAULT 0,
    buyer_uuid BINARY(16)
);