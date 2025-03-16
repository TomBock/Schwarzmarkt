INSERT INTO return_bids (player_uuid, amount)
VALUES (?, ?)
ON CONFLICT(player_uuid)
DO UPDATE SET amount = return_bids.amount + EXCLUDED.amount;
