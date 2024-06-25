package com.hoonit.xeye.device;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.hoonit.xeye.event.DataEvent;
import com.hoonit.xeye.event.DataListener;
import com.hoonit.xeye.event.SetListener;
import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.net.snmp.DeviceMIB;
import com.hoonit.xeye.util.ResourceBundleHandler;
import com.pi4j.io.serial.SerialDataEventListener;

abstract public class DeviceBase implements SerialDataEventListener, SetListener{
	
	protected Logger logger;
	
	protected DeviceMIB deviceMIB;
	
	protected String deviceName;
	
	protected int protocol;
	
	protected int deviceType;
	
	protected String deviceOID;
	
	protected String channel;
	
	protected String baudRate;
	
	protected List<Map<String, Object>> tagList;
	
	private Vector<DataListener> dataListener;
	
	private DataEvent dataEvt;
	
	private Object transObject;
	
	protected int networkErrorCnt = 0;
	
	protected int networkErrorBasisCnt;
	
	protected long readLockWaitInterval;
	
	// 응답여부
	protected boolean isResponse = false; 
	
	// 쓰기여부
	protected boolean isWrite = false; 
	
	// 최근데이터 요청 여부
	protected boolean isReqRecent = false;
	
	protected Object readLock = new Object();
	
	public DeviceBase(){
	}
	
	public DeviceBase(String deviceName, int protocol, int deviceType, String deviceOID, List<Map<String, Object>> tagList){
		
		this.deviceName = deviceName;
		
		this.protocol = protocol;
		
		this.deviceType = deviceType;
		
		this.deviceOID = EnterpriseOIDManager.getEnterpriseOID() + "." + deviceOID;
		
		this.tagList = tagList;
		
		this.dataListener = new Vector<DataListener>();
		
		this.dataEvt = new DataEvent(this);
		
		this.networkErrorBasisCnt = Integer.parseInt(ResourceBundleHandler.getInstance().getString("network.error.count"));
		
		this.readLockWaitInterval = Long.parseLong(ResourceBundleHandler.getInstance().getString("serial.read.lock.wait.interval"));
	}
	
	public DeviceBase(String deviceName, int protocol, int deviceType, String deviceOID, String channel, String baudRate, List<Map<String, Object>> tagList){
		
		this(deviceName, protocol, deviceType, deviceOID, tagList);
		
		this.channel  = channel;
		
		this.baudRate = baudRate;
	}
	
	public void addDataListener(DataListener obs) {
		dataListener.add(obs);
	}

	public void removeDataListener(DataListener obs) {
		dataListener.remove(obs);
	}

	public void notifyData(Map<String, String> resultMap, 
						   Map<String, String> etcResultMap){
		
		dataEvt.setResultMap(resultMap);
		dataEvt.setEtcResultMap(etcResultMap);
		
		for(int i = 0; i < dataListener.size(); i++){
			dataListener.get(i).notifyData(dataEvt);
		}
	}
	
	public void setLogger(Logger logger){
		this.logger = logger;
	}
	
	public void setTransObject(Object transObject){
		this.transObject = transObject;
	}
	
	public Object getTransObject(){
		return this.transObject;
	}
	
	public int getProtocol() {
		return protocol;
	}
	
	public int getDeviceType(){
		return deviceType;
	}

	public String getDeviceOID() {
		return deviceOID;
	}

	public String getChannel() {
		return channel;
	}

	public String getBaudRate() {
		return baudRate;
	}
	
	public int getNetworkErrorCnt(){
		return networkErrorCnt;
	}
	
	public int getNetworkErrorBasisCnt(){
		return networkErrorBasisCnt;
	}

	public List<Map<String, Object>> getTagList() {
		return tagList;
	}
	
	public DeviceMIB getDeviceMIB(){
		return this.deviceMIB;
	}

	/**
	 * 데이터 요청 메시지
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	abstract public byte[] requestMessage() throws IOException;
	
	/**
	 * 데이터 요청 메시지
	 * @param data
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	abstract public byte[] requestMessage(String channel, String data) throws IOException;
	
	/**
	 * 데이터 응답 메시지
	 * @param buffer
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	abstract public Object responseMessage(byte[] buffer) throws IOException;
	
	/**
	 * 요청 및 응답 실행
	 */
	abstract public void execute() throws IOException;
	
	/**
	 * 통신불량
	 * @param val (0:Normal, 1:Abnormal)
	 */
	abstract public void setNetworkError(String val);
	
	/**
	 * 장비별 MIB 정보 생성 및 반환
	 * @return
	 */
	abstract public DeviceMIB getMIB();
}