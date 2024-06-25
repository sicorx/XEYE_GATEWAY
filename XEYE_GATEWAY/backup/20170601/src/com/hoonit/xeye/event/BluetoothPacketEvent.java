package com.hoonit.xeye.event;

import java.util.EventObject;

public class BluetoothPacketEvent extends EventObject{

	private String packet;
	
	public BluetoothPacketEvent(Object obj){
		super(obj);
	}

	public String getPacket() {
		return packet;
	}

	public void setPacket(String packet) {
		this.packet = packet;
	}
}