--- Select all bids for a given auction and the item data of the auction
SELECT ab.player_uuid, ab.bid_amount, a.item_data
FROM auction_bids ab
JOIN auctions a ON ab.auction_id = a.id
ORDER BY ab.bid_amount DESC;