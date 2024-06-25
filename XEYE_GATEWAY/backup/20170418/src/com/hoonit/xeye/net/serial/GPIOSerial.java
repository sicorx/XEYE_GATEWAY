package com.hoonit.xeye.net.serial;

import java.io.IOException;
import java.util.ArrayList;

import com.hoonit.xeye.device.DeviceBase;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPort;
import com.pi4j.io.serial.StopBits;

public class GPIOSerial extends NetworkBase implements Runnable, SerialDataEventListener {
	
	private final Serial serial = SerialFactory.createInstance();
	
	private SerialConfig config;
	
	private ArrayList<DeviceBase> deviceArr = new ArrayList<DeviceBase>();
	
	//private GpioController gpio = GpioFactory.getInstance();
	
	/*private GpioPinDigitalOutput pin7; // GPIO 7
	private GpioPinDigitalOutput pin1; // GPIO 1
	private GpioPinDigitalOutput pin0; // GPIO 0
	private GpioPinDigitalOutput pin2; // GPIO 2
	private GpioPinDigitalOutput pin4; // GPIO 4
	private GpioPinDigitalOutput pin3; // GPIO 3
*/	
	private long readInterval;
	
	//private long pinChangeInterval;
	
	//private long readLockWaitInterval;
	
	private Object readLock = new Object();
	
    public GPIOSerial(){
    	
    	super();
    	
    	//this.pinChangeInterval    = Long.parseLong(ResourceBundleHandler.getInstance().getString("serial.pinchange.interval"));
    	//this.readLockWaitInterval = Long.parseLong(ResourceBundleHandler.getInstance().getString("serial.read.lock.wait.interval"));
    	
    	//initGpioPin();
    	
    	initSerialConfig();
    }
    
    /*private void initGpioPin(){
    	
    	pin7 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, PinState.LOW);
    	pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW);
    	pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.LOW);
    	pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW);
    	pin4 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, PinState.LOW);
    	pin3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, PinState.LOW);
    	
    	pin7.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    	pin1.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    	pin0.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    	pin2.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    	pin4.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    	pin3.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
    }*/
    
    private void initSerialConfig(){
    	
    	try{
    		
	    	config = new SerialConfig();
	    	
	    	config.device(SerialPort.getDefaultPort())
					        .baud(Baud._115200)
					        .dataBits(DataBits._8)
					        .parity(Parity.NONE)
					        .stopBits(StopBits._1)
					        .flowControl(FlowControl.NONE);
	    	
	    	//serial.setBufferingDataReceived(false);
	    	
    	}catch(Exception e){
    		logger.error(e.getMessage(), e);
    	}
    }
    
    private void openConnect() throws IOException {
        
    	logger.info("Connecting to: " + config.toString());
	    	
	    serial.open(config);
    }
    
    public void setDevice(DeviceBase device){
    	deviceArr.add(device);
    }
    
    public void clearDevice(){
    	deviceArr.clear();
    }
    
    public void setReadInterval(long readInterval){
    	this.readInterval = readInterval;
    }
    
    public Serial getSerial(){
    	return this.serial;
    }
    
    @Override
    public void dataReceived(SerialDataEvent event) {
    	
    	try{
    		byte[] buffer = event.getBytes();
    		logger.info(new String(buffer, 0, buffer.length));
    		
    		synchronized(readLock) {
				//serial.removeListener(this);
				readLock.notifyAll();
			}
    		
    	}catch(Exception e){
    		logger.error(e.getMessage(), e);
    	}
	}
    
    /*
     * 	2400	0
	 * 	4800	1
	 * 	9600	2
	 * 	14400	3
	 * 	19200	4
	 * 	28800	5
	 * 	38400	6
	 * 	57600	7
	 * 	76800	8
	 * 	115200	9
     */
    /*private byte[] getRS485ChangeByte(char channel, char baudRate){
    	
    	ByteBuffer buffer = ByteBuffer.allocate(20);
		
		// STX
    	buffer.put((byte)'1'); 

    	// Packet Length
    	buffer.put((byte)'0');
    	buffer.put((byte)'0');
    	buffer.put((byte)'1');
    	buffer.put((byte)'4');
    	
    	// Command
    	buffer.put((byte)'B');
    	buffer.put((byte)'A');
    	buffer.put((byte)'U');
    	buffer.put((byte)'D');
    	
    	// RTU 6000은 channel 상관없음....
    	// Channel
    	buffer.put((byte)'0');
    	buffer.put((byte)'0');
    	buffer.put((byte)'0');
    	buffer.put((byte)'0');
		
		// Value
		buffer.put((byte)'0');
    	buffer.put((byte)baudRate);
		
		// Reserve
    	buffer.put((byte)'D');
    	buffer.put((byte)'D');
    	buffer.put((byte)'D');
    	buffer.put((byte)'D');
    	
    	// ETX
    	buffer.put((byte)0x0D);
		
		buffer.flip();
    	
    	return buffer.array();
    }*/
    
    // Pin change 데이터 요청
    /*private void requestPinChangeMessage(char channel, char baudRate){
    	
    	try{
    		
    		logger.info("Pin change Channel : " + channel + ", Baud Rate : " + baudRate);
    		
			byte[] b = getRS485ChangeByte(channel, baudRate);
			
			logger.info("Pin change packet send...");
			
			doWrite(b);
				
    	}catch(IOException e){
			logger.error(e.getMessage(), e);
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
    }*/
    
    private void doWrite(byte[] bytes) throws IOException {
    	
    	/*for(byte b : bytes){
    		serial.write(b);
    		
    		try{
    			Thread.sleep(10);
    		}catch(Exception e){
    			logger.error(e.getMessage(), e);
    		}
    	}*/
    	//serial.flush();
    	
    	serial.write(bytes);
    	serial.flush();
    }
    
    // 모든 장비 통신불량 상태로 변경 
    private void doNetworkErrorAllDevice(){
    	
    	for(DeviceBase device : deviceArr){
			device.setNetworkError(NetworkError.ABNORMAL);
		}
    }
    
    /*public void changePinDefault(){
    	
    	pin7.low();
		pin1.low();
		pin0.low();
		pin2.low();
		pin4.low();
		pin3.low();
		
		// 0.05 초 대기
		try{
			Thread.sleep(pinChangeInterval);
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
    }*/
    
    /*public void changePinForSend(String channel){
    	
    	// Channel 이 1이면
		if("1".equals(channel)){
			pin7.high();
			pin1.low();
			pin0.low();
			pin2.low();
			pin4.low();
			pin3.low();
		}
		// Channel 이 2이면
		else if("2".equals(channel)){
			pin7.low();
			pin1.high();
			pin0.low();
			pin2.low();
			pin4.low();
			pin3.low();
		}
		// Channel 이 3이면
		else if("3".equals(channel)){
			pin7.high();
			pin1.high();
			pin0.low();
			pin2.low();
			pin4.low();
			pin3.low();
		}
		// Channel 이 4이면
		else if("4".equals(channel)){
			pin7.low();
			pin1.low();
			pin0.high();
			pin2.low();
			pin4.low();
			pin3.low();
		}
		// Channel 이 5이면
		else if("5".equals(channel)){
			pin7.high();
			pin1.low();
			pin0.high();
			pin2.low();
			pin4.low();
			pin3.low();
		}
		// Channel 이 6이면
		else if("6".equals(channel)){
			pin7.low();
			pin1.high();
			pin0.high();
			pin2.low();
			pin4.low();
			pin3.low();
		}
		// Channel이 없으면 RS232
		else{
			pin7.high();
			pin1.high();
			pin0.high();
			pin2.low();
			pin4.low();
			pin3.low();
		}
		
		// 0.05 초 대기
		try{
			Thread.sleep(pinChangeInterval);
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
    }*/
    
    /*public void changePinForRecv(String channel){
    	
    	// Channel 이 1이면
		if("1".equals(channel)){
			pin7.high();
			pin1.low();
			pin0.low();
			pin2.high();
			pin4.low();
			pin3.low();
		}
		// Channel 이 2이면
		else if("2".equals(channel)){
			pin7.low();
			pin1.high();
			pin0.low();
			pin2.low();
			pin4.high();
			pin3.low();
		}
		// Channel 이 3이면
		else if("3".equals(channel)){
			pin7.high();
			pin1.high();
			pin0.low();
			pin2.high();
			pin4.high();
			pin3.low();
		}
		// Channel 이 4이면
		else if("4".equals(channel)){
			pin7.low();
			pin1.low();
			pin0.high();
			pin2.low();
			pin4.low();
			pin3.high();
		}
		// Channel 이 5이면
		else if("5".equals(channel)){
			pin7.high();
			pin1.low();
			pin0.high();
			pin2.high();
			pin4.low();
			pin3.high();
		}
		// Channel 이 6이면
		else if("6".equals(channel)){
			pin7.high();
			pin1.high();
			pin0.high();
			pin2.low();
			pin4.high();
			pin3.high();
		}
		// Channel이 없으면 RS232
		else{
			pin7.high();
			pin1.high();
			pin0.high();
			pin2.high();
			pin4.high();
			pin3.high();
		}
    }*/
    
    public void run(){
    	
    	try{
    		openConnect();
    	}catch(IOException e){
    		logger.error(e.getMessage(), e);
    		doNetworkErrorAllDevice();
    	}
    		
    	while(!Thread.currentThread().isInterrupted()){
    		
			for(DeviceBase device : deviceArr){
				
				if(tryWriteLock()){
				
					try{
						
	    				/*if(device.getDeviceType() != DeviceType.DI){
	    					
	    					synchronized(readLock){
	    						
		    					serial.addListener(this);
		    					
								changePinDefault();
								
								char channel  = device.getChannel().charAt(0);
								char baudRate = device.getBaudRate().charAt(0);
								
								requestPinChangeMessage(channel, baudRate);
								
								try{
									readLock.wait(readLockWaitInterval);
								}catch(Exception e){
									logger.error(e.getMessage(), e);
								}finally{
									serial.removeListener(this);
								}
	    					}
						}*/
	    				
						device.execute();
						
					}catch(IOException e){
					}finally{
						unWritelock();
					}
				}
			}
			
			try{
				Thread.sleep(readInterval);
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
    	}
    }
}