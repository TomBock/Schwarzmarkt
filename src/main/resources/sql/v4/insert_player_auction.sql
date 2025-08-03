INSERT INTO player_auctions (item_id, item_data, owner_uuid, min_bid, deposit)
VALUES (?, ?, ?, ?, ?)
RETURNING id;
