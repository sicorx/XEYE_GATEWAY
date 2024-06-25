package com.hoonit.xeye.test;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

public class SerialTest implements Runnable, SerialPortEventListener {

	private CommPortIdentifier portIdentifier;
	private SerialPort serialPort;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	private String portName;
	private int baudRate;
	private int dataBits;
	private int stopBits;
	private String parity;
	
	private GpioController gpio;
	
	private GpioPinDigitalOutput pin0; // GPIO 0
	private GpioPinDigitalOutput pin1; // GPIO 1
	
	public SerialTest(String portName, int baudRate, int dataBits, int stopBits, String parity){
		
		this.portName = portName;
		this.baudRate = baudRate;
		this.dataBits = dataBits;
		this.stopBits = stopBits;
		this.parity   = parity;
		
		gpio = GpioFactory.getInstance();
		
		pin0 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.LOW); // 17
		pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW); // 18
		
		pin0.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
		pin1.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
		
		pin0.low();
		pin1.low();
	}
	
	private void connect() throws Exception {
		
		System.out.println("try to connect " + portName);
		System.out.println("Baud Rate="+baudRate);
		System.out.println("Data Bits="+dataBits);
		System.out.println("Stop Bits="+stopBits);
		System.out.println("Parity="+parity);
		
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
            //serialPort.notifyOnCarrierDetect(true);
            //serialPort.notifyOnCTS(true);
            //serialPort.notifyOnDSR(true);
            //serialPort.notifyOnFramingError(true);
            //serialPort.notifyOnOutputEmpty(true);
            //serialPort.notifyOnOverrunError(true);
            //serialPort.notifyOnParityError(true);
            
            //if(receiveTimeout > 0){
	            try {
	    			serialPort.enableReceiveTimeout(5000);  //milliseconds 
	    	    } catch (UnsupportedCommOperationException e) {
	    	    	System.out.println(e.getMessage());
	    	    }
            //}
        }
	}
	
	private void close() {
		
		System.out.println("close connect ");
		
    	try {
    		
    		if(inputStream != null) inputStream.close();
    		if(outputStream !=  null) outputStream.close();;
    		if(serialPort != null) serialPort.close();
    		
    	} catch (IOException e) {
    		System.out.println(e.getMessage());
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
	    		System.out.println("Serial port break detected");
	    		close();
	    		break;
	    	default:
	    		System.out.println("Serial port event: " + e.getEventType());
	    }
	}
	
	private String byteArrayToHex(byte[] a) {
	    StringBuilder sb = new StringBuilder();
	    for(final byte b: a)
	        sb.append(String.format("%02x ", b&0xff));
	    return sb.toString();
	}
	
	private void clearInputStream(DataInputStream inputStream) throws IOException {
		
		 if (inputStream.available() > 0) {
			 int len = inputStream.available();
			 byte buf[] = new byte[len];
			 inputStream.read(buf, 0, len);
			 System.out.println("Clear input");
	    }
	}
	
	public void run(){
	
		try{
			
			// 18번 high
			pin1.high();
			
			Thread.sleep(500);
			
			// 17번 high
			pin0.high();
			
			Thread.sleep(500);
			
			// 17번 low
			pin0.low();
			
			Thread.sleep(500);
			
			////////// stm32flash command 실행 시작 ////////////
			
			/*connect();
			
			byte[] bytes = null;
			
			int bufferSize = 0;
			
			byte[] buffer = null;
			
			// 0x7F(INIT) 전송
			bytes = new byte[1];
			bytes[0] = 0x7F;
			
			outputStream.write(bytes, 0, bytes.length);
			outputStream.flush();
			
			System.out.println("0x7F writed...");
			
			bufferSize = 1;
			
			try {
				serialPort.enableReceiveThreshold(bufferSize);
			} catch (UnsupportedCommOperationException e) {
				System.out.println(e.getMessage());
			}
			
			buffer = new byte[bufferSize];
    		
			System.out.println("0x7F cmd readed...");
			
    		inputStream.read(buffer, 0, buffer.length);
    		
    		System.out.println(byteArrayToHex(buffer));
			
			
    		
    		// 0x00(Get) 전송
    		bytes = new byte[2];
    		bytes[0] = 0x00;
			bytes[1] = (byte) ((0xFF) & (0x00 ^ 0xFF));
			
			//outputStream.write(bytes, 0, bytes.length);
			//outputStream.flush();
			
			for(int i = 0; i < bytes.length; i++){
				outputStream.write(bytes[i]);
			}
			outputStream.flush();
			
			
			System.out.println("0x00 writed...");
    		
			bufferSize = 1;
			
			try {
				serialPort.enableReceiveThreshold(bufferSize);
			} catch (UnsupportedCommOperationException e) {
				System.out.println(e.getMessage());
			}
			
			buffer = new byte[bufferSize];
    		
			System.out.println("0x00 cmd readed...");
			
			int readBytes = inputStream.read(buffer, 0, buffer.length);
    		
			System.out.println("readBytes=" + readBytes);
			
    		System.out.println("ACK=" + byteArrayToHex(buffer));
    		
    		Thread.sleep(500);
    		
    		buffer = new byte[bufferSize];
    		
    		try {
				serialPort.enableReceiveThreshold(bufferSize);
			} catch (UnsupportedCommOperationException e) {
				System.out.println(e.getMessage());
			}
    		
    		inputStream.read(buffer, 0, buffer.length);
    		
    		System.out.println("LEN=" + byteArrayToHex(buffer));
    		*/
    		
    		/*
    		// 0x01(GVR) 전송
    		bytes = new byte[2];
			bytes[0] = 0x01;
			//bytes[1] = 0x01 ^ 0xFF;
			bytes[1] = (byte) ((0xFF) & (0x01 ^ 0xFF));
			
			outputStream.write(bytes, 0, bytes.length);
			outputStream.flush();
			
			System.out.println("0x01 writed...");
			
			bufferSize = 1;
			
			try {
				serialPort.enableReceiveThreshold(bufferSize);
			} catch (UnsupportedCommOperationException e) {
				System.out.println(e.getMessage());
			}
			
			buffer = new byte[bufferSize];
    		
			System.out.println("0x01 cmd readed...");
			
    		int readBytes = inputStream.read(buffer, 0, buffer.length);
    		
    		System.out.println("readBytes="+readBytes);
    		
    		System.out.println("ACK="+byteArrayToHex(buffer));
    		
    		Thread.sleep(500);
    		
    		try {
				serialPort.enableReceiveThreshold(bufferSize);
			} catch (UnsupportedCommOperationException e) {
				System.out.println(e.getMessage());
			}
			
			buffer = new byte[bufferSize];
			
    		inputStream.read(buffer, 0, buffer.length);
    		
    		System.out.println("VERSION="+byteArrayToHex(buffer));
    		
    		Thread.sleep(500);
    		
    		try {
				serialPort.enableReceiveThreshold(bufferSize);
			} catch (UnsupportedCommOperationException e) {
				System.out.println(e.getMessage());
			}
			
			buffer = new byte[bufferSize];
			
    		inputStream.read(buffer, 0, buffer.length);
    		
    		System.out.println("OPTION1="+byteArrayToHex(buffer));
    		
    		Thread.sleep(500);
    		
    		try {
				serialPort.enableReceiveThreshold(bufferSize);
			} catch (UnsupportedCommOperationException e) {
				System.out.println(e.getMessage());
			}
			
			buffer = new byte[bufferSize];
			
    		inputStream.read(buffer, 0, buffer.length);
    		
    		System.out.println("OPTION2="+byteArrayToHex(buffer));
    		*/
    		
    		/*
    		// 0x43(Erase) 전송
    		bytes = new byte[2];
			bytes[0] = 0x43;
			//bytes[1] = 0x01 ^ 0xFF;
			bytes[1] = (byte) ((0xFF) & (0x02 ^ 0xFF));
			
			outputStream.write(bytes, 0, bytes.length);
			outputStream.flush();
			
			System.out.println("0x43 writed...");
			
			try {
    			serialPort.enableReceiveTimeout(34 * 1000);  //milliseconds 
    	    } catch (UnsupportedCommOperationException e) {
    	    	System.out.println(e.getMessage());
    	    }
			
			bufferSize = 1;
			
			try {
				serialPort.enableReceiveThreshold(bufferSize);
			} catch (UnsupportedCommOperationException e) {
				System.out.println(e.getMessage());
			}
			
			System.out.println("0x43 cmd read...");
			
			buffer = new byte[bufferSize];
    		
			System.out.println("0x43 cmd readed...");
			
    		inputStream.read(buffer, 0, buffer.length);
    		
    		System.out.println(byteArrayToHex(buffer));
    		*/
    		
    		
    		
    		//close();
    		
    		////////// stm32flash command 실행 끝 ////////////
    		
			
			
			Process process   = null;
			BufferedReader br = null;
			
			// -b : baud rate
			// -m : data bit+flow control+stop bit
			//String[] cmd = {"stm32flash", "-b", "115200", "-m", "8n1" , "-w", "/home/xeye/resource/upload/patch_2017_6_2_14_35_42/sems_new_1_1.hex", "-v", "/dev/ttyS0"};
			String[] cmd = {"python", "/home/xeye/resource/upload/stm32loader.py", "-p", "/dev/ttyS0", "-b", "57600", "-vw", "/home/xeye/resource/upload/patch_2017_6_2_14_35_42/sems_new_1_1.hex"};
			//String[] cmd = {"/opt/arminarm/arminarm", "-f", "/home/xeye/resource/upload/patch_2017_6_2_14_35_42/sems_new_1_1.hex"};
			
			try{
				
				System.out.println("upload file...");
				
				// 프로세스 실행
				process = Runtime.getRuntime ().exec(cmd);
				process.waitFor();
				
				br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				
				String line;
				while (( line = br.readLine ()) != null){
					System.out.println(line);
				}
				
				br.close();
				process.destroy();
				
				System.out.println("upload completed...");
				
			}catch(Exception e){
				System.out.println(e.getMessage());
			}finally{
				try{
					if(br != null) br.close();
					if(process != null) process.destroy();
				}catch(Exception e){
					System.out.println(e.getMessage());
				}
			}
			
			
			
			
			
			
			
			
    		Thread.sleep(500);
			
			// 18번 low
			pin1.low();
			
			Thread.sleep(500);
			
			// 17번 high
			pin0.high();
			
			Thread.sleep(500);
			
			// 17번 low
			pin0.low();
			
		/*}catch(IOException e){
			System.out.println(e.getMessage());
		*/}catch(Exception e){
			System.out.println(e.getMessage());
		}finally{
			close();
			
			if(pin1.isHigh())
				pin1.low();
			
			if(pin0.isHigh())
				pin0.low();
		}
	}
	
	public static void main(String[] args){
		
		String portName = "/dev/ttyS0";
		int baudRate = 57600;
		int dataBits = 8;
		int stopBits = 1;
		String parity = "even";
		
		Thread t = new Thread(new SerialTest(portName, baudRate, dataBits, stopBits, parity));
		t.start();
		
		//System.out.println(Byte.parseByte(String.valueOf(0x01 ^ 0xFF)));
		//byte b = (byte) ((0xFF) & (0x01 ^ 0xFF));
		//System.out.println(b);
	}
}
