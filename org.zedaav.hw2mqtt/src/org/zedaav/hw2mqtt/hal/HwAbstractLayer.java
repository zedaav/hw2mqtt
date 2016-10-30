package org.zedaav.hw2mqtt.hal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.zedaav.hw2mqtt.Hw2MqttService;
import org.zedaav.hw2mqtt.log.LoggerLevel;
import org.zedaav.hw2mqtt.log.SimpleLogger;
import org.zedaav.hw2mqtt.misc.PropertiesReader;
import org.zedaav.hw2mqtt.mqtt.MqttConnector;

public class HwAbstractLayer implements Hw2MqttService {

	private static HwAbstractLayer instance;
	public static HwAbstractLayer getInstance() {
		if (instance == null) {
			instance = new HwAbstractLayer();
		}
		return instance;
	}
	
	// Main topic to subscribe for HW events
	private String mainHwTopic;
	
	// Map to preserve HW bindings (indexed on topic)
	private Map<String, HwBinding> hwBindings = new HashMap<String, HwBinding>();
	
	private SimpleLogger logger = SimpleLogger.getInstance();
	
	@Override
	public void init(Properties props) throws IOException {
		// Main HW topic to be subscribed
		mainHwTopic = PropertiesReader.getProperty("hal.mainHwTopic", props);
		
		// Load properties for devices map
		// Places list
		Set<String> placeIDs = PropertiesReader.getPropertySet("hal.places", props);
		for (String placeID: placeIDs) {
			// Devices list
			Set<String> deviceIDs = PropertiesReader.getPropertySet("hal." + placeID + ".devices", props);
			for (String deviceID : deviceIDs) {
				// Get HW topic
				String hwTopic = PropertiesReader.getProperty("hal." + placeID + "." + deviceID + ".hwTopic", props);
				hwBindings.put(mainHwTopic + "/" + hwTopic, new HwBinding(placeID, deviceID, hwTopic));
			}
		}
	}

	@Override
	public void start() throws IOException {
		// Subscribe to main HW topic
		MqttConnector.getInstance().subscribe(mainHwTopic + "/#", new HwListener());
	}

	private class HwListener implements IMqttMessageListener {
		@Override
		public void messageArrived(String topic, MqttMessage msg) throws Exception {
			// Check for known binding
			HwBinding hb = hwBindings.get(topic);
			if (hb != null) {
				String payload = new String(msg.getPayload());
				
				// New HW event
				logger.log(LoggerLevel.INFO, "Received HW event: %s -- %s", topic, payload);
				
				// Forward as abstracted event
				MqttConnector.getInstance().publish("places/" + hb.getPlace() + "/devices/" + hb.getDevice(), payload);
			} else {
				// Unknown event
				logger.log(LoggerLevel.DEBUG, "Unknown HW event: %s", topic);
			}
		}
	}
}
