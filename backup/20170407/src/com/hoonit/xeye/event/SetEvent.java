package com.hoonit.xeye.event;

import java.util.EventObject;

public class SetEvent extends EventObject {
	
	private String oid;
	
	private String data;
	
	public SetEvent(Object obj){
		super(obj);
	}
	
	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
