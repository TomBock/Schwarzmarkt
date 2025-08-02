-- Place or update a bid
INSERT INTO player_auction_bids (player_auction_id, player_uuid, bid_amount)
VALUES (?, ?, ?)
ON CONFLICT(player_auction_id, player_uuid)
DO UPDATE SET bid_amount = player_auction_bids.bid_amount + EXCLUDED.bid_amount;
