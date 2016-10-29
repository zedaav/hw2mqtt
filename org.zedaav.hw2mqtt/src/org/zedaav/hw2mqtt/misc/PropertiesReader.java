package org.zedaav.hw2mqtt.misc;

import java.io.IOException;
import java.util.Properties;

public final class PropertiesReader {

	public static String getProperty(String name, Properties props) throws IOException {
		String ret = props.getProperty(name);
		if (ret == null) {
			throw new IOException(name + " property not found");
		}
		return ret;
	}

	public static String getProperty(String name, Properties props, String defaultV) {
		String ret = props.getProperty(name, defaultV);
		return ret;
	}
}
