package com.hoonit.xeye.net.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.hoonit.xeye.device.DeviceBase;
import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.util.ResourceBundleHandler;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

public class Serial extends NetworkBase implements Runnable, SerialPortEventListener {
	
	private CommPortIdentifier portIdentifier;
	private SerialPort serialPort;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private String portName;
	private int baudRate;
	private int dataBits;
	private int stopBits;
	private String parity;
	
	private ArrayList<DeviceBase> deviceArr = new ArrayList<DeviceBase>();
	
	private int receiveTimeout = 0;
	
	private long readInterval;
	
	private short commErrCnt = 0;
	
	private boolean interrupt = false;
	
	private boolean isFirstLoad = false;
	
	private Object lockObj = new Object();
	
	public Serial(String portName, int baudRate, int dataBits, int stopBits, String parity){
		
		super();
		
		this.portName = portName;
		this.baudRate = baudRate;
		this.dataBits = dataBits;
		this.stopBits = stopBits;
		this.parity   = parity;
	}
	
	private void connect() throws Exception {
		
		logger.info("try to connect " + portName);
		logger.info("Baud Rate="+baudRate);
		logger.info("Data Bits="+dataBits);
		logger.info("Stop Bits="+stopBits);
		logger.info("Parity="+parity);
		
		portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		
        if ( portIdentifier.isCurrentlyOwned() ) {
            throw new Exception("Error: " + this.portName + " is currently in use");
        } else {
        	
        	serialPort = (SerialPort)portIdentifier.open(this.getClass().getName(), 2000);
        	
        	int parityInt = SerialPort.PARITY_NONE;
        	
        	if("odd".equals(parity.toLowerCase())){
        		parityInt = SerialPort.PARITY_ODD;
        	}else if("even".equals(parity.toLowerCase())){
        		parityInt = SerialPort.PARITY_EVEN;
        	}else if("mark".equals(parity.toLowerCase())){
        		parityInt = SerialPort.PARITY_MARK;
        	}else if("space".equals(parity.toLowerCase())){
        		parityInt = SerialPort.PARITY_SPACE;
        	}
        	
            serialPort.setSerialPortParams(baudRate, dataBits, stopBits, parityInt);
            
            inputStream  = new DataInputStream(serialPort.getInputStream());
            outputStream = new DataOutputStream(serialPort.getOutputStream());
            
            serialPort.addEventListener(this);
            
            serialPort.notifyOnBreakInterrupt(true);
            /*serialPort.notifyOnCarrierDetect(true);
            serialPort.notifyOnCTS(true);
            serialPort.notifyOnDSR(true);
            serialPort.notifyOnFramingError(true);
            serialPort.notifyOnOutputEmpty(true);
            serialPort.notifyOnOverrunError(true);
            serialPort.notifyOnParityError(true);*/
            
            if(receiveTimeout > 0){
	            try {
	    			serialPort.enableReceiveTimeout(receiveTimeout); /* milliseconds */
	    	    } catch (UnsupportedCommOperationException e) {
	    	    	logger.error(e.getMessage(), e);
	    	    }
            }
        }
	}
	
	private void close() {
		
		logger.info("close connect ");
		
    	try {
    		
    		if(inputStream != null) inputStream.close();
    		if(outputStream !=  null) outputStream.close();;
    		if(serialPort != null) serialPort.close();
    		
    	} catch (IOException e) {
    		logger.error(e.getMessage(), e);
    	} finally {
    		inputStream = null;
    		outputStream = null;
    		serialPort = null;
    	}
	}
	
	public void serialEvent(SerialPortEvent e) {
		
	    switch (e.getEventType()) {
	    	case SerialPortEvent.DATA_AVAILABLE:
	    		break;
	    	case SerialPortEvent.BI:
	    		logger.info("Serial port break detected");
	    		close();
	    		break;
	    	/*default:
	    		logger.info("Serial port event: " + e.getEventType());*/
	    }
	}
	
	public void setDevice(DeviceBase deviceBase){
		deviceArr.add(deviceBase);
	}
	
	public void clearDevice(){
    	deviceArr.clear();
    }
	
	public void setReadInterval(long readInterval){
		this.readInterval = readInterval;
	}
	
	public void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}
	
	public SerialPort getSerialPort() {
		return serialPort;
	}

	public DataInputStream getInputStream() {
		return inputStream;
	}

	public DataOutputStream getOutputStream() {
		return outputStream;
	}
	
	public void interrupt(){
		this.interrupt = true;
		
		synchronized(lockObj){
			lockObj.notify();
		}
	}
	
	public void run(){
		
		while(!Thread.currentThread().isInterrupted() && !interrupt){
			
			synchronized(lockObj){
				
				if(serialPort == null){
					try{
						connect();
					}catch(Exception e){
						
						logger.error(e.getMessage(), e);
						
						close();
						
						commErrCnt++;
						
						// 연결 오류 3회 이상이면
						if(commErrCnt > Integer.parseInt(ResourceBundleHandler.getInstance().getString("network.error.count"))){
							
							logger.info("connect error applying...");
							
							for(DeviceBase device : deviceArr){
								device.setNetworkError(NetworkError.ABNORMAL);
							}
							
							commErrCnt = 0;
							
						}else{
							logger.info("connect error count : " + commErrCnt);
						}
						
						try{
							//Thread.sleep(reconnectInterval);
							lockObj.wait(reconnectInterval);
						}catch(Exception e1){
							logger.error(e1.getMessage(), e1);
						}
					}
				}
				
				if(serialPort != null){
					
					commErrCnt = 0;
					
					for(DeviceBase device : deviceArr){
						
						if(interrupt){
							break;
						}else{
						
							if(tryWriteLock()){
								try{
									
									device.execute();
									
								}catch(IOException e){
									logger.error(e.getMessage(), e);
								}catch(Exception e){
									logger.error(e.getMessage(), e);
								}finally{
									unWritelock();
									
									try{
										Thread.sleep(1);
									}catch(Exception e){}
								}
							}
						}
					}
					
					if(!isFirstLoad){
						
						try{
							SNMPAgentProxyManager.getInstance().setStaticOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".1.99.0", "1");
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}
						
						isFirstLoad = true;
					}
					
					if(!interrupt){
						try{
							//Thread.sleep(readInterval);
							lockObj.wait(readInterval);
						}catch(Exception e1){
							logger.error(e1.getMessage(), e1);
						}
					}else{
						break;
					}
				}
			}
		}
		
		close();
	}
}
