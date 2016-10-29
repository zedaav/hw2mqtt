package org.zedaav.hw2mqtt.mqtt;

import java.io.IOException;
import java.util.Properties;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.zedaav.hw2mqtt.Hw2MqttService;
import org.zedaav.hw2mqtt.log.LoggerLevel;
import org.zedaav.hw2mqtt.log.SimpleLogger;
import org.zedaav.hw2mqtt.misc.PropertiesReader;

public class MqttConnector implements Hw2MqttService {
	private static final String MQTT_BROKER = "mqtt.broker";
	private static final String MQTT_CLIENTID = "mqtt.clientID";
	private static final String MQTT_TOPICROOT = "mqtt.topicRoot";

	private static MqttConnector instance;
	private SimpleLogger logger = SimpleLogger.getInstance();
	
	private String broker;
	private String clientID;
	private String topicRoot;
	private MqttClient client;
	
	public static MqttConnector getInstance() {
		if (instance == null) {
			instance = new MqttConnector();
		}
		return instance;
	}
	
	public void init(Properties props) throws IOException {
		// Check properties
		broker = PropertiesReader.getProperty(MQTT_BROKER, props);
		clientID = PropertiesReader.getProperty(MQTT_CLIENTID, props);
		topicRoot = PropertiesReader.getProperty(MQTT_TOPICROOT, props);
	}
	
	public void start() throws IOException {
		try {
			// Connect to broker
			client = new MqttClient("tcp://" + broker + ":1883", clientID);
			client.connect();
		} catch (MqttException e) {
			throw new IOException(e);
		}
	}
	
	public void publish(String topic, String payloadString) {
		MqttMessage mm = new MqttMessage(payloadString.getBytes());
		try {
			String fullTopic = topicRoot + (topicRoot.endsWith("/")?"":"/") + topic;
			client.publish(fullTopic , mm);
			logger.log(LoggerLevel.INFO, "Sent MQTT message; topic: %s / payload: %s", fullTopic, payloadString);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}
