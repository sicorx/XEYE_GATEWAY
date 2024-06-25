package com.hoonit.xeye.test;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.hoonit.xeye.util.CRC16;

import net.wimpi.modbus.util.ModbusUtil;

public class UDPTest {

	public static void main(String[] args){
		
		// ip : 10.25.132.20 port : 1609
		
		String ip = "10.25.132.20";
		int port = 1609;
		
		DatagramSocket dsock = null;
        
        try{
        	
        	InetAddress inetaddr = InetAddress.getByName(ip);
        	
        	dsock = new DatagramSocket();
        	
        	// 태그 등록
        	byte cmd = 1;
        	byte fclear = 2;
        	//byte[] data1 = "D54".getBytes(); // 저수조_수위(D54)
        	byte[] data1 = "저수조.저수조_수위".getBytes(); // 저수조_수위(D54)
        	//byte[] data2 = "저수조_고수위경보".getBytes();
        	
        	ByteBuffer buffer = ByteBuffer.allocate(1024);
    		buffer.put(cmd); // cmd
    		buffer.put(fclear); // fclear
    		buffer.put(data1); // data
    		//buffer.put((byte)1);
    		//buffer.put(data2); // data
    		//buffer.put((byte)0);
    		
    		for(int i = 0; i < (1021 - (data1.length)); i++){
    			buffer.put((byte)0);
    		}
    		
    		// Check Sum
    		int chkSum = cmd + fclear;
    		
    		for(int i = 0; i < data1.length; i++){
    			chkSum += data1[i];
    		}
    		
    		//chkSum += (byte)1;
    		
    		/*for(int i = 0; i < data2.length; i++){
    			chkSum += data2[i];
    		}*/
    		
    		//chkSum += (byte)0;
    		
    		byte hi  = (byte) ((chkSum & 0xFF00) >> 8);  //start chkSum hi 
    		byte low = (byte) (chkSum & 0x00FF); // start chkSum low 
    		
    		buffer.put(low);
    		
    		buffer.flip();
    		
    		System.out.println("보낸값 : " + ModbusUtil.toHex(buffer.array()));
    		
    		// DatagramPacket에 각 정보를 담고 전송
        	DatagramPacket sendPacket = new DatagramPacket(buffer.array(), buffer.array().length, inetaddr, port);
        	dsock.send(sendPacket);
        	
        	byte[] b = new byte[1024];
        	// 반송되는 DatagramPacket을 받기 위해
        	//receivePacket 생성한 후 대기
        	DatagramPacket receivePacket = new DatagramPacket(b, b.length);
        	dsock.receive(receivePacket);
        	
        	byte[] rb = receivePacket.getData();
        	
        	String msg = new String(rb, 0, rb.length);
        	System.out.println("전송받은 문자열 : "+msg);
        	
        	System.out.println(ModbusUtil.toHex(rb));
        	
        	// 서버 생존 확인
        	/*byte cmd = (byte)3;
        	
        	ByteBuffer buffer = ByteBuffer.allocate(1024);
    		buffer.put(cmd); // cmd
    		
    		for(int i = 0; i < 1022; i++){
    			buffer.put((byte)0);
    		}
    		
    		// Check Sum
    		int chkSum = cmd;
    		
    		byte hi  = (byte) ((chkSum & 0xFF00) >> 8);
    		byte low = (byte) (chkSum & 0x00FF);

    		buffer.put(low);
    		
    		System.out.println(ModbusUtil.toHex(buffer.array()));
    		
    		buffer.flip();*/
        	
        	// 태그 목록 취득
        	/*byte cmd = 2;
        	
        	ByteBuffer buffer = ByteBuffer.allocate(1024);
    		buffer.put(cmd); // cmd
    		
    		for(int i = 0; i < 1022; i++){
    			buffer.put((byte)0);
    		}
    		
    		// Check Sum
    		int chkSum = cmd;
    		
    		byte hi  = (byte) ((chkSum & 0xFF00) >> 8);
    		byte low = (byte) (chkSum & 0x00FF);
    		
    		buffer.put(low);
    		
    		buffer.flip();*/
        	
        	Thread.sleep(1000);
        	
        	// 태그 목록 취득
        	while(true){
        		
        		System.out.println("요청");
        		
	        	cmd = 2;
	        	
	        	buffer = ByteBuffer.allocate(1024);
	    		buffer.put(cmd); // cmd
	    		
	    		for(int i = 0; i < 1022; i++){
	    			buffer.put((byte)0);
	    		}
	    		
	    		// Check Sum
	    		chkSum = cmd;
	    		
	    		hi  = (byte) ((chkSum & 0xFF00) >> 8);
	    		low = (byte) (chkSum & 0x00FF);
	    		
	    		buffer.put(low);
	    		
	    		buffer.flip();
	
	        	// DatagramPacket에 각 정보를 담고 전송
	    		sendPacket = new DatagramPacket(buffer.array(), buffer.array().length, inetaddr, port);
	        	dsock.send(sendPacket);
			 
	        	b = new byte[1024];
	        	// 반송되는 DatagramPacket을 받기 위해
	        	//receivePacket 생성한 후 대기
	        	receivePacket = new DatagramPacket(b, b.length);
	        	dsock.receive(receivePacket);
			 
	        	// 받은 결과 출력
	        	System.out.println(receivePacket.getData().length);
	        	msg = new String(receivePacket.getData(), 0, receivePacket.getData().length);
	        	System.out.println("전송받은 문자열 : "+msg);
	        	
	        	rb = receivePacket.getData();
	        	
	        	System.out.println(ModbusUtil.toHex(rb));
	        	
	        	Thread.sleep(5000);
        	}
        	
        }catch(Exception ex){
               System.out.println(ex);
        }finally{
               if(dsock != null) dsock.close();
        }
	}
}
