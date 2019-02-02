package org.zedaav.hw2mqtt.rfxcom;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Properties;

import org.zedaav.hw2mqtt.Hw2MqttService;
import org.zedaav.hw2mqtt.log.LoggerLevel;
import org.zedaav.hw2mqtt.log.SimpleLogger;
import org.zedaav.hw2mqtt.misc.Hw2MqttConstants;
import org.zedaav.hw2mqtt.misc.PropertiesReader;
import org.zedaav.hw2mqtt.mqtt.MqttConnector;

public class Rfxcom2Mqtt implements Hw2MqttService {

	private static final String RFXCOM_HOST = "rfxcom.host";
	private static Rfxcom2Mqtt instance;

	private String host;
	private Thread readerThread = null;
	private SimpleLogger logger = SimpleLogger.getInstance();

	public static Rfxcom2Mqtt getInstance() {
		if (instance == null) {
			instance = new Rfxcom2Mqtt();
		}
		return instance;
	}

	public void init(Properties props) throws IOException {
		// Check Rfxcom IP
		host = PropertiesReader.getProperty(RFXCOM_HOST, props);
	}

	public void start() throws IOException {
		// Run reader
		readerThread = new SocketReader(host, 10001);
		readerThread.start();
	}

	private class SocketReader extends Thread {
		boolean interrupted = false;
		InputStream in;
		OutputStream out;
		Socket socket;
		String host;
		int port;

		public SocketReader(String host, int port) {
			this.host = host;
			this.port = port;
		}

		public void connect() throws IOException {
			logger.log(LoggerLevel.INFO, "Connecting to RFXCOM: %s %d", this.host, this.port);
			this.socket = new Socket(this.host, this.port);
			this.in = socket.getInputStream();
			this.out = socket.getOutputStream();
			socket.setSoTimeout(60*1000);

			out.flush();
			if (this.in.markSupported()) {
				this.in.reset();
			}
		}

		public void disconnect() {
			try {
				this.in.close();
				this.out.close();
				this.socket.close();
			} catch (IOException e) {
			} // quietly close
		}

		@Override
		public void interrupt() {
			interrupted = true;
			super.interrupt();
			disconnect();
		}

		public void run() {
			logger.log(LoggerLevel.DEBUG, "Data listener started");
			
			// Loop to reconnect in case of unexpected disconnection
			// (e.g. power cut)
			while (true) {
				try {
					// Connect to RFXCOM
					connect();
					
					// Handle data read
					readLoop();
				} catch (SocketTimeoutException e) {
					logger.error("Read timeout");
				} catch (InterruptedIOException e) {
					Thread.currentThread().interrupt();
					logger.error("Interrupted via InterruptedIOException");
				} catch (IOException e) {
					logger.error("Reading from socket failed", e);
				}
				
				// Clean any pending connection
				disconnect();
				
				// Restart after some time
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					// Fails silently
				}
			}
		}

		private void readLoop() throws IOException, SocketTimeoutException {
			final int dataBufferMaxLen = Byte.MAX_VALUE;

			byte[] dataBuffer = new byte[dataBufferMaxLen];

			int msgLen = 0;
			boolean start_found = false;

			byte[] tmpData = new byte[20];
			int len = -1;

			while (!interrupted) {
				len = in.read(tmpData);
				if (len < 0) {
					logger.log(LoggerLevel.ERROR, "Error while reading from socket");
					break;
				}

				byte[] logData = Arrays.copyOf(tmpData, len);
				logger.log(LoggerLevel.DEBUG, "Received data (len=%d): %s", len, logger.printHexBinary(logData));

				// Just one byte received? --> start of the frame
				if (len == 1) {
					// We're OK to go with the next read. This is the frame length (in bits)
					start_found = true;
					dataBuffer[0] = tmpData[0];
					logger.log(LoggerLevel.DEBUG, "Start of frame detected");
				} else if (start_found) {
					// Validate length
					if ((dataBuffer[0] <= len * 8) && (dataBuffer[0] > (len - 1) * 8)) {
						// Remember the length
						msgLen = len + 1;

						// Copy the frame
						System.arraycopy(tmpData, 0, dataBuffer, 1, len);

						// whole message received, send an event
						byte[] msg = new byte[msgLen + 1];

						for (int j = 0; j < msgLen; j++) {
							msg[j] = dataBuffer[j];
						}

						// Full frame event
						logger.log(LoggerLevel.DEBUG, "Full frame detected (len=%d): %s", msgLen,
								logger.printHexBinary(msg));
						processFrame(msg);
					} else {
						logger.log(LoggerLevel.DEBUG, "Invalid frame length: %d: was expecting %d", len * 8,
								dataBuffer[0]);
					}

					// find new start
					start_found = false;
				} else {
					// Garbage data, don't know what to do with it
					logger.log(LoggerLevel.DEBUG, "Unknown data");
				}
			}
		}
	}

	private void processFrame(byte[] msg) {
		// Check for length
		if ((msg[0] == 34) && (msg[5] == 0)) {
			// "CHACON-like" event
			processChaconEvent(msg);
		} else {
			logger.log(LoggerLevel.DEBUG, "Unknown frame: %s", logger.printHexBinary(msg));
		}
	}

	private void processChaconEvent(byte[] msg) {
		// Processing Chacon event
		byte[] longMsg = new byte[8];
		System.arraycopy(msg, 1, longMsg, 4, 4);
		ByteBuffer bb = ByteBuffer.wrap(longMsg, 0, 8);
		long deviceID = bb.getLong();
		boolean on_off = (deviceID & 16) != 0;
		if (on_off) {
			deviceID = deviceID - 16;
		}
		String onOffPayload = on_off ? Hw2MqttConstants.ON : Hw2MqttConstants.OFF;
		String deviceIDStr = String.format("%X", deviceID);
		logger.log(LoggerLevel.INFO, "CHACON event: %s %s", deviceIDStr, onOffPayload);

		// Publish event
		MqttConnector.getInstance().publish("chacon/" + deviceIDStr, onOffPayload);
	}
}
