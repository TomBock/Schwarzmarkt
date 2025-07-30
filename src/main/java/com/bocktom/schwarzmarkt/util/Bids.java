package com.bocktom.schwarzmarkt.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Bids extends HashMap<UUID, Integer> {
	public Bids(Map<UUID, Integer> bids) {
		super(bids);
	}

	public Bids() {
	}
}