package com.bocktom.schwarzmarkt.util;

import com.bocktom.schwarzmarkt.Schwarzmarkt;

public class MSG {

	public static String get(String path) {
		return Schwarzmarkt.plugin.getConfig().getString("messages." + path);
	}

	public static String get(String path, String... replaceVariables) {
		if(replaceVariables.length % 2 != 0) {
			throw new IllegalArgumentException("replaceVariables must be a multiple of 2");
		}
		String message = get(path);
		for(int i = 0; i < replaceVariables.length; i += 2) {
			message = message.replace(replaceVariables[i], replaceVariables[i + 1]);
		}
		return message;
	}

}
