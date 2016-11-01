package org.zedaav.hw2mqtt.hal.tranform;

import org.zedaav.hw2mqtt.misc.Hw2MqttConstants;

public class HalPayloadTransformOnOff2OpenClosed implements HalPayloadTransform {

	@Override
	public String transform(String originalPayload) {
		// Simple ON -> OPEN / OFF -> CLOSED transformation
		if (originalPayload.equals(Hw2MqttConstants.ON)) {
			return Hw2MqttConstants.OPEN;
		}
		if (originalPayload.equals(Hw2MqttConstants.OFF)) {
			return Hw2MqttConstants.CLOSED;
		}
		return originalPayload;
	}

}
