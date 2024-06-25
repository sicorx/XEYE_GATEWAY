package com.hoonit.xeye.util;

import org.apache.log4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class IOBoardUploadUtils {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static IOBoardUploadUtils instance = new IOBoardUploadUtils();
	
	private GpioController gpio;
	
	private GpioPinDigitalOutput pin0; // GPIO 0
	private GpioPinDigitalOutput pin1; // GPIO 1
	
	private IOBoardUploadUtils(){
		
		gpio = GpioFactory.getInstance();
		
		pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.LOW);
		pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW);
		
		pin0.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
		pin1.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
	}
	
	public static IOBoardUploadUtils getInstance(){
		
		if(instance == null){
			instance = new IOBoardUploadUtils();
		}
		
		return instance;
	}
	
	public boolean doUpload() {
		
		boolean result = false;
		
		try{
			
			pin0.high();
			
			Thread.sleep(2000);
			
			pin1.high();
			
			result = true;
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}finally{
			pin0.low();
			pin1.low();
		}
		
		return result;
	}
}