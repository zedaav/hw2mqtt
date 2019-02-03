package org.zedaav.hw2mqtt.zibase;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;

import org.zedaav.hw2mqtt.Hw2MqttService;
import org.zedaav.hw2mqtt.log.LoggerLevel;
import org.zedaav.hw2mqtt.log.SimpleLogger;
import org.zedaav.hw2mqtt.misc.PropertiesReader;
import org.zedaav.hw2mqtt.mqtt.MqttConnector;

import fr.zapi.ZbResponse;
import fr.zapi.Zibase;

public class ZibaseMqtt implements Hw2MqttService {

	private static final String ZIBASE_HOST = "zibase.host";
	private static final String ZIBASE_LISTEN_PORT = "zibase.listeningPort";
	private static ZibaseMqtt instance;

	private String host;
	private int listeningPort;
	private Zibase zibase;
	private Thread readerThread = null;
	private SimpleLogger logger = SimpleLogger.getInstance();

	public static ZibaseMqtt getInstance() {
		if (instance == null) {
			instance = new ZibaseMqtt();
		}
		return instance;
	}

	@Override
	public void init(Properties props) throws IOException {
		// Check ZiBase IP
		host = PropertiesReader.getProperty(ZIBASE_HOST, props);
		listeningPort = Integer.parseInt(PropertiesReader.getProperty(ZIBASE_LISTEN_PORT, props, "9876"));
	}

	@Override
	public void start() throws IOException {
		// Setup ZiBase interface
		zibase = new Zibase(host);

		// Run reader
		readerThread = new ZibaseReader();
		readerThread.start();
	}

	private class ZibaseReader extends Thread {
		boolean interrupted = false;
		final String listeningHost = "127.0.0.1";
		DatagramSocket serverSocket;

		private void setup() throws UnknownHostException, SocketException {
			// Register to Zibase
			zibase.hostRegistering(listeningHost, listeningPort);

			// Bind listening socket
			serverSocket = new DatagramSocket(listeningPort);
		}

		private void clean() {
			// Unregister from zibase and close listening socket
			try {
				zibase.hostUnregistering(listeningHost, listeningPort);
				serverSocket.close();
			} catch (UnknownHostException e) {
				logger.error("Error while unregistering: %s", e.getMessage());
			}
		}

		@Override
		public void interrupt() {
			interrupted = true;
			super.interrupt();
			clean();
		}

		public void run() {
			logger.log(LoggerLevel.DEBUG, "Zibase listener started");

			// Loop to reconnect in case of unexpected exception
			while (true) {
				try {
					// Register to Zibase
					setup();

					// Handle data read
					readLoop();
				} catch (SocketTimeoutException e) {
					logger.error("Read timeout");
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
					logger.error("Interrupted via InterruptedIOException");
				} catch (IOException e) {
					logger.error("Reading from socket failed");
				} catch (Exception e) {
					logger.error("Unknown exception: %s %s", e.getClass().getName(), e.getMessage());
				}

				// Clean everything before retry
				clean();

				// Restart after some time
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					// Fails silently
				}
			}
		}

		private void readLoop() throws IOException {
			byte[] receiveData = new byte[470];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			while (!interrupted) {
				// the real thread work is here: read message and analyze it to publish events
				serverSocket.receive(receivePacket);
				ZbResponse zbResponse = new ZbResponse(receivePacket.getData());
				logger.log(LoggerLevel.DEBUG, "ZIBASE MESSAGE: " + zbResponse.getMessage().replaceAll("%", "%%"));
				processZibFrame(zbResponse);

				// reset buffer
				Arrays.fill(receivePacket.getData(), 0, receivePacket.getLength(), (byte) 0);
			}
		}
		
		private void processZibFrame(ZbResponse frame) {
			// Check that we can get fields
			if (frame.getMessage() != null) {
				String RF = frame.getTagFromMessage("rf");
				if (RF != null && !RF.isEmpty()) {
					if (RF.contains("Oregon")) {
						processOregonFrame(frame);
					}
				}
			}
		}

		private void processOregonFrame(ZbResponse frame) {
			// Get fields
			String id = frame.getTagFromMessage("id");
			String tem = frame.getTagFromMessage("tem");
			String hum = frame.getTagFromMessage("hum");
			String bat = frame.getTagFromMessage("bat");
			logger.log(LoggerLevel.DEBUG, "Parsed Oregon Frame: %s %s %s %s", id, tem, hum, bat);
			
			// Build and publish messages
			publishOregonMessage(id, tem, "temp");
			publishOregonMessage(id, hum, "humidity");
			publishOregonMessage(id, bat, "battery");
		}

		private void publishOregonMessage(String id, String value, String type) {
			if (value != null) {
				logger.log(LoggerLevel.INFO, "OREGON event: %s %s %s", type, id, value);
				MqttConnector.getInstance().publish("oregon/" + type + "/" + id, value);
			}
		}
	}
}
