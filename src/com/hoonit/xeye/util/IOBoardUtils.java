package com.hoonit.xeye.util;

import org.apache.log4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class IOBoardUtils {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static IOBoardUtils instance = new IOBoardUtils();
	
	private GpioController gpio;
	
	private GpioPinDigitalOutput pin0; // GPIO 0
	private GpioPinDigitalOutput pin1; // GPIO 1
	
	private IOBoardUtils(){
		
		gpio = GpioFactory.getInstance();
		
		pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.LOW); // 17
		pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW); // 18
		
		pin0.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
		pin1.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
	}
	
	public static IOBoardUtils getInstance(){
		
		if(instance == null){
			instance = new IOBoardUtils();
		}
		
		return instance;
	}
	
	public void init(){
		
		pin0.low();
		pin1.low();
	}
	
	public GpioPinDigitalOutput getPin0(){
		return pin0;
	}
	
	public GpioPinDigitalOutput getPin1(){
		return pin1;
	}
	
	public boolean doReset() {
		
		logger.info("I/O Reset");
		
		boolean result = false;
		
		try{
			
			pin0.high();
			
			Thread.sleep(3000);
			
			pin0.low();
			
			result = true;
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}finally{
			if(pin0.isHigh())
				pin0.low();
		}
		
		return result;
	}
	
	/*public boolean doUpload() {
		
		logger.info("I/O Patch file transfer");
		
		boolean result = false;
		
		try{
			
			// 18번 high
			pin1.high();
			
			Thread.sleep(500);
			
			// 17번 high
			pin0.high();
			
			Thread.sleep(500);
			
			// 17번 low
			pin0.low();
			
			// 파일전송
			Thread.sleep(2000);
			
			// 18번 low
			pin1.low();
			
			Thread.sleep(500);
			
			// 17번 high
			pin0.high();
			
			Thread.sleep(500);
			
			// 17번 low
			pin0.low();
			
			result = true;
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}finally{
			
			if(pin1.isHigh())
				pin1.low();
			
			if(pin0.isHigh())
				pin0.low();
		}
		
		return result;
	}*/
}