package com.hoonit.xeye.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import net.wimpi.modbus.util.ModbusUtil;

public class RXTX implements SerialPortEventListener {

	private CommPortIdentifier portIdentifier;
	private SerialPort serialPort;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	
	public RXTX(){
		
		try{
			portIdentifier = CommPortIdentifier.getPortIdentifier("/dev/ttyS0");
			
	        if ( portIdentifier.isCurrentlyOwned() ) {
	            throw new Exception("Error: /dev/ttyS0 is currently in use");
	        } else {
	        	
	        	serialPort = (SerialPort)portIdentifier.open(this.getClass().getName(), 2000);
	        	
	        	int parityInt = SerialPort.PARITY_NONE;
	        	
	            serialPort.setSerialPortParams(115200, 8, 1, parityInt);
	            
	            inputStream  = new DataInputStream(serialPort.getInputStream());
	            outputStream = new DataOutputStream(serialPort.getOutputStream());
	            
	            serialPort.addEventListener(this);
	            serialPort.notifyOnBreakInterrupt(true);
		    	serialPort.enableReceiveTimeout(2000); /* milliseconds */
	        }
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void serialEvent(SerialPortEvent e) {
	    
	    switch (e.getEventType()) {
	    	case SerialPortEvent.DATA_AVAILABLE:
	    		break;
	    	case SerialPortEvent.BI:
	    		System.out.println("Serial port break detected");
	    		break;
	    }
	}
	
	private byte[] requestMessage() throws IOException, Exception{
		
		ByteBuffer buffer = ByteBuffer.allocate(14);
    	
		// STX
    	buffer.put((byte)'1'); 

    	// Packet Length
    	buffer.put((byte)'0');
    	buffer.put((byte)'0');
    	buffer.put((byte)'0');
    	buffer.put((byte)'E');
    	
    	// Command
    	buffer.put((byte)'D');
    	buffer.put((byte)'I');
    	buffer.put((byte)'R');
    	buffer.put((byte)'Q');
    	
    	// Channel
    	buffer.put((byte)'0');
    	buffer.put((byte)'0');
    	buffer.put((byte)'0');
    	buffer.put((byte)'1');
    	
    	// ETX
    	buffer.put((byte)0x0D);
    	
    	buffer.flip();
    	
    	byte[] b = buffer.array();
    	
    	return b;
	}
	
	private void clearInputStream(DataInputStream inputStream) throws IOException {
		
		 if (inputStream.available() > 0) {
			 int len = inputStream.available();
			 byte buf[] = new byte[len];
			 inputStream.read(buf, 0, len);
			 System.out.println("Clear input: " + ModbusUtil.toHex(buf, 0, len));
	    }
	}
	
	private byte[] readMessage(DataInputStream inputStream, int bufferSize) throws IOException{
		
		byte[] buffer = new byte[bufferSize];
		
		inputStream.read(buffer, 0, buffer.length);
		
		return buffer;
	}
	
	/**
	 * javac -classpath .:./resource/lib/jamod-1.2-SNAPSHOT.jar Test.java
	 *
	 * java -Dgnu.io.rxtx.SerialPorts=/dev/ttyS0 -classpath .:./resource/lib/jamod-1.2-SNAPSHOT.jar Test
	 */
	public void doStart(){
		
		try{
			
			byte[] bytes = requestMessage();
			
			clearInputStream(inputStream);
			
			for(byte b : bytes){
				outputStream.write(b);
	    		Thread.sleep(10);
	    	}
			
			byte[] buffer = readMessage(inputStream, 30);
			
			serialPort.disableReceiveThreshold();
			
			StringBuffer logSb = new StringBuffer();
			
			logSb.setLength(0);
			
			byte[] dest = new byte[1];
	        System.arraycopy(buffer, 0, dest, 0, dest.length);
	        logSb.append((char)dest[0]).append(" ");
	        
	        dest = new byte[4];
	        System.arraycopy(buffer, 1, dest, 0, dest.length);
	        String len = new String(dest, 0, dest.length);
	        logSb.append(len).append(" ");
	        
	        dest = new byte[4];
	        System.arraycopy(buffer, 5, dest, 0, dest.length);
	        String command = new String(dest, 0, dest.length);
	        logSb.append(command).append(" ");
	        
	        //dest = new byte[8];
	        dest = new byte[20];
	        System.arraycopy(buffer, 9, dest, 0, dest.length);
	        String channelVal = new String(dest, 0, dest.length);
	        logSb.append(channelVal).append(" ");
	        
	        dest = new byte[1];
	        System.arraycopy(buffer, 29, dest, 0, dest.length);
	        logSb.append((char)dest[0]);
	        
	        System.out.println(logSb.toString());
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		RXTX t = new RXTX();
		t.doStart();
	}
}
