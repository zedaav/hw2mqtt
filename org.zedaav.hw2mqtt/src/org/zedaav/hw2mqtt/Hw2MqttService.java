package org.zedaav.hw2mqtt;

import java.io.IOException;
import java.util.Properties;

public interface Hw2MqttService {

	void init(Properties props) throws IOException;
	
	void start() throws IOException;
}
