package com.hoonit.xeye.event;

import java.util.EventObject;
import java.util.Map;

public class DataEvent extends EventObject{

	private Map<String, String> resultMap    = null;
	private Map<String, String> etcResultMap = null;
	
	public DataEvent(Object obj){
		super(obj);
	}

	public Map<String, String> getResultMap() {
		return resultMap;
	}

	public void setResultMap(Map<String, String> resultMap) {
		this.resultMap = resultMap;
	}

	public Map<String, String> getEtcResultMap() {
		return etcResultMap;
	}

	public void setEtcResultMap(Map<String, String> etcResultMap) {
		this.etcResultMap = etcResultMap;
	}
}