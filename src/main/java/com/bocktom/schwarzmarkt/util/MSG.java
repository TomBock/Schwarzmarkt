package com.bocktom.schwarzmarkt.util;

public class MSG {

	public static String get(String path) {
		return Config.msg.get.getString(path);
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
