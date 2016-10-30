package org.zedaav.hw2mqtt;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.zedaav.hw2mqtt.hal.HwAbstractLayer;
import org.zedaav.hw2mqtt.log.SimpleLogger;
import org.zedaav.hw2mqtt.misc.PropertiesReader;
import org.zedaav.hw2mqtt.mqtt.MqttConnector;
import org.zedaav.hw2mqtt.rfxcom.Rfxcom2Mqtt;

public class Hw2MqttApp {

	// List initialized with basic services
	private static List<Hw2MqttService> services = new ArrayList<Hw2MqttService>(Arrays.asList(SimpleLogger.getInstance(), MqttConnector.getInstance()));
	
	// Optional services map (with keys)
	private static Map<String, Hw2MqttService> optionalServices;
	static {
		optionalServices = new HashMap<String, Hw2MqttService>();
		optionalServices.put("rfxcom", Rfxcom2Mqtt.getInstance());
		optionalServices.put("hal", HwAbstractLayer.getInstance());
	}
	
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
			
			// Setup required services
			setupServices(props);
			
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

	private static void setupServices(Properties props) throws IOException {
		// Verify required services
		Set<String> serviceIDs =  PropertiesReader.getPropertySet("enabledServices", props);
		for (String serviceID : serviceIDs) {
			Hw2MqttService instance = optionalServices.get(serviceID);
			if (instance == null) {
				// Unknown service
				throw new IOException("Unknown service identifier: " + serviceID);
			}
			services.add(instance);
		}
	}

}
