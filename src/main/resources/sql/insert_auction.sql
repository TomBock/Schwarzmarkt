INSERT INTO auctions (item_data, highest_bid, highest_bidder_uuid)
VALUES (?, 0, NULL)
RETURNING id;
