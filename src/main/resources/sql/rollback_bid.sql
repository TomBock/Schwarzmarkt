WITH updated AS (
    UPDATE auction_bids
    SET bid_amount = bid_amount - ?
    WHERE auction_id = ? AND player_uuid = ?
    RETURNING bid_amount
)
DELETE FROM auction_bids
WHERE auction_id = ? AND player_uuid = ? AND EXISTS (
    SELECT 1 FROM updated WHERE bid_amount <= 0
);