INSERT INTO player_auctions (item_id, item_data, owner_uuid, deposit)
VALUES (?, ?, ?, ?)
RETURNING id;
