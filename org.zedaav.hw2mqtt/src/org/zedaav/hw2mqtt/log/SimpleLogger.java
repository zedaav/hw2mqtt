package org.zedaav.hw2mqtt.log;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.zedaav.hw2mqtt.Hw2MqttService;
import org.zedaav.hw2mqtt.misc.PropertiesReader;

public class SimpleLogger implements Hw2MqttService {

	private static SimpleLogger instance;

	public static SimpleLogger getInstance() {
		if (instance == null) {
			instance = new SimpleLogger();
		}
		return instance;
	}
	
	private Map<LoggerLevel, Boolean> enabledLevels = new HashMap<LoggerLevel, Boolean>();

	public void log(LoggerLevel level, String format, Object... args) {
		if (enabledLevels.get(level)) {
			PrintStream usedStream = System.out;
			if (level == LoggerLevel.ERROR) {
				usedStream = System.err;
			}
			usedStream.printf("[" + level + "] " + format + "\n", args);
		}
	}

	public void error(String format, Object... args) {
		log(LoggerLevel.ERROR, format, args);
	}

	public String printHexBinary(byte[] buffer) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < buffer.length; i++) {
			b.append(String.format("%X ", buffer[i]));
		}
		return b.toString().trim();
	}

	@Override
	public void init(Properties props) throws IOException {
		// Load enabled levels from properties
		for (LoggerLevel l : LoggerLevel.values()) {
			enabledLevels.put(l, new Boolean(PropertiesReader.getProperty("logger." + l, props, Boolean.toString(l.getDefaultValue()))));
		}
	}

	@Override
	public void start() throws IOException {
		// Nothing to do (no running process)
	}
}
