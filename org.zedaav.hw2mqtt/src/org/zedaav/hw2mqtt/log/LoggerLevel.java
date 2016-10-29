package org.zedaav.hw2mqtt.log;

public enum LoggerLevel {
	DEBUG (false),
	INFO (true),
	WARNING (true),
	ERROR (true);
	
	private boolean defaultValue;
	
	private LoggerLevel(boolean def) {
		defaultValue = def;
	}
	
	public boolean getDefaultValue() {
		return defaultValue;
	}
}
