package com.bocktom.schwarzmarkt.util;

import java.util.UUID;

public class Bid {

	public UUID player;
	public int amount;

	public Bid(UUID player, int amount) {
		this.player = player;
		this.amount = amount;
	}
}
