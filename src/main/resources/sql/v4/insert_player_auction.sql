INSERT INTO auctions (item_id, item_data, owner_uuid)
VALUES (?, ?, ?)
RETURNING id;
