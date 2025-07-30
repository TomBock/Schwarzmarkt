DELETE FROM item_cooldown
WHERE cooldown_end < CURRENT_TIMESTAMP;