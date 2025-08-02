-- Update the highest bid in an auction if the new bid is higher
UPDATE player_auctions SET highest_bid = ?, highest_bidder_uuid = ? WHERE id = ? AND ? > highest_bid;
