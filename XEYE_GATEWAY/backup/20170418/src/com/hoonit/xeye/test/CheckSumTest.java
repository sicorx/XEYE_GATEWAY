package com.hoonit.xeye.test;

import java.nio.ByteBuffer;

import net.wimpi.modbus.util.ModbusUtil;

public class CheckSumTest {

	public static void main(String[] args){
		
		//11 + 00 + 38 + 48 + 65 + 6C + 6C + 6F + 20 + 77 + 6F + 72 + 6C + 64 + 2E + 0A +00 = 4BD
		//4BD에서 한바이트 만을 남기고 상위 비트를 제거하면
		// FF - BD =  42

		/*ByteBuffer buffer = ByteBuffer.allocate(17);
		buffer.put((byte)0x11);
		buffer.put((byte)0x00);
		buffer.put((byte)0x38);
		buffer.put((byte)0x48);
		buffer.put((byte)0x65);
		buffer.put((byte)0x6C);
		buffer.put((byte)0x6C);
		buffer.put((byte)0x6F);
		buffer.put((byte)0x20);
		buffer.put((byte)0x77);
		buffer.put((byte)0x6F);
		buffer.put((byte)0x72);
		buffer.put((byte)0x6C);
		buffer.put((byte)0x64);
		buffer.put((byte)0x2E);
		buffer.put((byte)0x0A);
		buffer.put((byte)0x00);
		buffer.flip();
		
		int csum = 0;
		
		byte[] b = buffer.array();
		
		for(int i = 0; i < b.length; i++){
			csum += b[i];
		}
		
		byte hi  = (byte) ((csum & 0xFF00) >> 8);
		byte low = (byte) (csum & 0x00FF);
		
		System.out.println(new String(ModbusUtil.toHex(0xFF - low)));*/
		
		//FF +  10 + 01 + 30 +  3F + 01 + 56 + 70 + 2B + 5E + 71 + 2B + 72 + 2B +73 + 21 + 46 + 01 + 34 + 21
		//~38 =  C7
		
		ByteBuffer buffer = ByteBuffer.allocate(20);
		buffer.put((byte)0xFF);
		buffer.put((byte)0x10);
		buffer.put((byte)0x01);
		buffer.put((byte)0x30);
		buffer.put((byte)0x3F);
		buffer.put((byte)0x01);
		buffer.put((byte)0x56);
		buffer.put((byte)0x70);
		buffer.put((byte)0x2B);
		buffer.put((byte)0x5E);
		buffer.put((byte)0x71);
		buffer.put((byte)0x2B);
		buffer.put((byte)0x72);
		buffer.put((byte)0x2B);
		buffer.put((byte)0x73);
		buffer.put((byte)0x21);
		buffer.put((byte)0x46);
		buffer.put((byte)0x01);
		buffer.put((byte)0x34);
		buffer.put((byte)0x21);
		buffer.flip();
		
		int csum = 0;
		
		byte[] b = buffer.array();
		
		for(int i = 0; i < b.length; i++){
			csum += b[i];
		}
		
		System.out.println(new String(ModbusUtil.toHex(~csum)));
	}
}
