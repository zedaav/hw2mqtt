package org.zedaav.hw2mqtt;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.zedaav.hw2mqtt.log.SimpleLogger;
import org.zedaav.hw2mqtt.mqtt.MqttConnector;
import org.zedaav.hw2mqtt.rfxcom.Rfxcom2Mqtt;

public class Hw2MqttApp {

	private static List<Hw2MqttService> services = Arrays.asList(SimpleLogger.getInstance(), MqttConnector.getInstance(), Rfxcom2Mqtt.getInstance());
	
	public static void main(String[] args) {
		// Look for properties
		if (args.length < 1) {
			System.err.println("No arguments specified.");
			System.exit(1);
		}
		try {
			// Load properties
			String propPath = args[0];
			Properties props = new Properties();
			props.load(new FileInputStream(propPath));
			
			// Prepare services
			for (Hw2MqttService s : services) {
				// Load from properties
				s.init(props);
				
				// Start
				s.start();
			}
			
			// Nothing to do but sleeping
			while(true) {
				Thread.sleep(10*1000);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}

}
