package org.zedaav.hw2mqtt.misc;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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
	
	public static Set<String> getPropertySet(String name, Properties props) throws IOException {
		String list = getProperty(name, props);
		Set<String> ret = new HashSet<String>(Arrays.asList(list.split("[, ]")));
		return ret;
	}
}
