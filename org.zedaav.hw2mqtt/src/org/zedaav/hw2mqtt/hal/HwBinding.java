package org.zedaav.hw2mqtt.hal;

public class HwBinding {
	private String place;
	private String device;
	private String hwTopic;
	
	public HwBinding(String place, String device, String hwTopic) {
		this.place = place;
		this.device = device;
		this.hwTopic = hwTopic;
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
}
