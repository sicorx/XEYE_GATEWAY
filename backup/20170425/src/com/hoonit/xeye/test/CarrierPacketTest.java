package com.hoonit.xeye.test;

import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;

public class CarrierPacketTest {

	public static void main(String[] args){
		
		try{
			
			byte command = 0x00;
			
			byte[] buffer = new byte[9];
			buffer[0] = (byte)1;
			buffer[1] = (byte)3;
			buffer[2] = (byte)0x02; // STX
			buffer[3] = (byte)0x04; // LEN
			buffer[4] = (byte)0xFF; // TYPE
			buffer[5] = command;    // COMMAND
			
			// CRC
			byte[] c = new byte[1];
			c[0] = command;
			
			short s = CRC16.getInstance().getCRC(c);
			
			byte[] crcBytes = new byte[2];
			crcBytes[0] = (byte)(s & 0x00FF);
			crcBytes[1] = (byte)((s & 0xFF00)>>8);
			
			buffer[6] = crcBytes[0];
			buffer[7] = crcBytes[1];
			
			buffer[8] = (byte)0x03; // ETX
			
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < buffer.length; i++) {
				sb.append(ByteUtils.toHexString((byte) buffer[i])).append(" ");
			}
			
			System.out.println(sb.toString());
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
