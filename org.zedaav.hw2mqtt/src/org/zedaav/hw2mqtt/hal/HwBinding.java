package org.zedaav.hw2mqtt.hal;

import java.io.IOException;

import org.zedaav.hw2mqtt.hal.tranform.HalPayloadTransform;
import org.zedaav.hw2mqtt.hal.tranform.HalPayloadTransformOkLowToInt;
import org.zedaav.hw2mqtt.hal.tranform.HalPayloadTransformOnOff2OpenClosed;

public class HwBinding {
	private String place;
	private String device;
	private String hwTopic;
	private HalPayloadTransform transform;
	
	public HwBinding(String place, String device, String hwTopic) {
		this.place = place;
		this.device = device;
		this.hwTopic = hwTopic;
	}

	public void initPayloadTransform(String payloadTransform) throws IOException {
		// Look for known transformations
		if (payloadTransform.equals("onoff2openclosed")) {
			transform = new HalPayloadTransformOnOff2OpenClosed();
		} else if (payloadTransform.equals("oklow2int")) {
			transform = new HalPayloadTransformOkLowToInt();
		} else {
			throw new IOException("Unknown payload tranformation: " + payloadTransform);
		}
	}

	public String getPlace() {
		return place;
	}

	public String getDevice() {
		return device;
	}

	public String getHwTopic() {
		return hwTopic;
	}

	public HalPayloadTransform getTranform() {
		return transform;
	}
}
