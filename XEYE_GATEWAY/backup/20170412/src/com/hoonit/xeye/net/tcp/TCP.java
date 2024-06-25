package com.hoonit.xeye.net.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

import com.hoonit.xeye.device.DeviceBase;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;

public class TCP extends NetworkBase implements Runnable {
	
	private SocketAddress socketAddress = null;
	private Socket socket = null;
	private DataInputStream inputStream = null;
	private DataOutputStream outputStream = null;
	
	private String ip;
	private int port;
	
	private ArrayList<DeviceBase> deviceArr = new ArrayList<DeviceBase>();
	
	private long readInterval;
	
	private int connectTimeOut = 5000;
	
	private int soTimeOut = 3000;
	
	public TCP(String ip, int port){
		
		super();
		
		this.ip = ip;
		this.port = port;
		
		socketAddress = new InetSocketAddress(ip, port);
	}
	
	private void connect(){
		
		try{
			
			logger.info("try to connect " + this.ip + ":" + this.port);
			
			socket = new Socket();
			socket.setSoTimeout(soTimeOut);
			socket.connect(socketAddress, connectTimeOut);
			
			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());
			
		}catch(Exception e){

			logger.error(e.getMessage(), e);

			try{
				if (inputStream != null) inputStream.close();
				if (outputStream != null) outputStream.close();
				if (socket != null) socket.close();
			}catch (Exception xe){
				logger.error(xe.getMessage(), xe);
			}finally{
				inputStream = null;
				outputStream = null;
				socket = null;
			}
		}
	}
	
	private void close(){
		
		try{
			
			if (inputStream != null) inputStream.close();
			if (outputStream != null) outputStream.close();
			if (socket != null) socket.close();
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}finally{
			inputStream = null;
			outputStream = null;
			socket = null;
		}
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
	
	public int getSoTimeOut() {
		return soTimeOut;
	}

	public void setSoTimeOut(int soTimeOut) {
		this.soTimeOut = soTimeOut;
	}

	public DataInputStream getInputStream() {
		return inputStream;
	}

	public DataOutputStream getOutputStream() {
		return outputStream;
	}
	
	// 모든 장비 통신불량 상태로 변경 
    private void doNetworkErrorAllDevice(){
    	
    	for(DeviceBase device : deviceArr){
			device.setNetworkError(NetworkError.ABNORMAL);
		}
    }
	
	public void run(){
		
		while(!Thread.currentThread().isInterrupted()){
			
			if(socket == null){
				connect();
			}
			
			if(socket == null){
				
				doNetworkErrorAllDevice();
				
				try{
					Thread.sleep(reconnectInterval);
				}catch(Exception xe){
					logger.error(xe.getMessage(), xe);
				}
				
			}else{
				
				for(DeviceBase device : deviceArr){
					
					if(tryWriteLock()){
						
						try{
							
							if(socket != null){
								device.execute();
							}
							
						}catch(SocketException e){
							close();
						}catch(IOException e){
							logger.error(e.getMessage(), e);
						}finally{
							unWritelock();
						}
					}
				} // end for
				
				try{
					Thread.sleep(readInterval);
				}catch(Exception xe){
					logger.error(xe.getMessage(), xe);
				}
			}
		}
	}
}
