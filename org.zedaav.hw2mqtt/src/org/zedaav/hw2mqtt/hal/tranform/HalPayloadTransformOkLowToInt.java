package org.zedaav.hw2mqtt.hal.tranform;

import org.zedaav.hw2mqtt.misc.Hw2MqttConstants;

public class HalPayloadTransformOkLowToInt implements HalPayloadTransform {

	@Override
	public String transform(String originalPayload) {
		// Simple Ok -> 100 / Low -> 10 transformation
		if (originalPayload.equals(Hw2MqttConstants.OK)) {
			return "100";
		}
		if (originalPayload.equals(Hw2MqttConstants.LOW)) {
			return "10";
		}
		return originalPayload;
	}

}
