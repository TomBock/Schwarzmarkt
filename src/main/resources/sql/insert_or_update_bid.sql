-- Place or update a bid
INSERT INTO auction_bids (auction_id, player_uuid, bid_amount)
VALUES (?, ?, ?)
ON CONFLICT(auction_id, player_uuid) DO UPDATE SET bid_amount = EXCLUDED.bid_amount
WHERE EXCLUDED.bid_amount > auction_bids.bid_amount;