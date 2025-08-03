--- remove column amount from player_items table and add deposit and min_bid columns
ALTER TABLE player_items
ADD COLUMN min_bid INT DEFAULT 0;