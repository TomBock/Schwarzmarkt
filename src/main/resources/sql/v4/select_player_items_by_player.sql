SELECT
    pi.item_id,
    pi.player_id,
    pi.quantity,
    pi.item_type,
    pi.item_name,
    CASE
        WHEN pa.auction_id IS NOT NULL THEN 1
        ELSE 0
    END AS in_auction
FROM player_items pi
LEFT JOIN player_auctions pa ON pi.item_id = pa.item_id
WHERE pi.player_id = ?
ORDER BY pi.item_id;