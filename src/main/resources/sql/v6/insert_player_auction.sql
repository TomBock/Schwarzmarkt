INSERT INTO player_auctions (item_id, item_data, owner_uuid, owner_name, min_bid, deposit)
VALUES (?, ?, ?, ?, ?, ?)
RETURNING id;
