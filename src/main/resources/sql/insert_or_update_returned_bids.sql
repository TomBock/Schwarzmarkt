INSERT INTO returned_bids (player_uuid, amount)
VALUES (?, ?)
ON CONFLICT(player_uuid)
DO UPDATE SET amount = returned_bids.amount + EXCLUDED.amount;
