package com.hoonit.xeye.event;

import java.util.EventObject;

public class IPChangeEvent extends EventObject {
	
	private String address;
	
	private String netmask;
	
	private String gateway;
	
	public IPChangeEvent(Object obj){
		super(obj);
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getNetmask() {
		return netmask;
	}

	public void setNetmask(String netmask) {
		this.netmask = netmask;
	}

	public String getGateway() {
		return gateway;
	}

	public void setGateway(String gateway) {
		this.gateway = gateway;
	}
}
