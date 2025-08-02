SELECT
    pi.id,
    pi.owner_uuid,
    pi.item_data,
    pi.amount,
    CASE
        WHEN pa.id IS NOT NULL THEN 1
        ELSE 0
    END AS in_auction
FROM player_items pi
LEFT JOIN player_auctions pa ON pi.id = pa.item_id
WHERE pi.owner_uuid = ?
ORDER BY pi.id;