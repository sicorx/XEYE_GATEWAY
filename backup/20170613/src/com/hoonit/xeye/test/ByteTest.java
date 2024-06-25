package com.hoonit.xeye.test;

import java.nio.ByteBuffer;

import com.hoonit.xeye.util.ByteUtils;

public class ByteTest {

	public static void main(String[] args){
		
		/*byte[] ip1 = ByteUtils.toBytes(Short.parseShort("192"));
		
		System.out.println(ip1.length);
		System.out.println(ip1[0]);
		System.out.println(ip1[1]);
		
		System.out.println(ByteUtils.toShort(ip1));*/
		
		/*short len = 0x17;
		
		byte[] ip1 = ByteUtils.toBytes(len);
		
		System.out.println(ip1.length);
		System.out.println(ip1[0]);
		System.out.println(ip1[1]);
		
		System.out.println(ByteUtils.toShort(ip1));*/
		
		//short s = ByteUtils.unsignedShort(65535);
		/*int s = ByteUtils.unsignedShort(65535);
		
		System.out.println(s);
		
		byte[] ss = ByteUtils.toBytes((short)s);
		
		System.out.println(ByteUtils.toShort(ss));*/
		
		// ByteUtils 테스트
		//byte b = -1;
		//System.out.println(ByteUtils.unsignedByte(b));
		
		/*short s = -1;
		
		byte[] b = ByteUtils.toBytes(s);
		// signed
		System.out.println(ByteUtils.toShort(b));
		// unsigned
		System.out.println(ByteUtils.toUnsignedShort(b));
		
		int i = 192;
		b = ByteUtils.toUnsignedShortBytes(i);
		System.out.println(ByteUtils.toUnsignedShort(b));*/
		
		/*int i = -1;
		
		byte[] b = ByteUtils.toBytes(i);
		// signed
		System.out.println(ByteUtils.toInt(b));
		// unsigned
		System.out.println(ByteUtils.toUnsignedInt(b));
		
		long l = 4294967295L;
		b = ByteUtils.toUnsignedIntBytes(l);
		System.out.println(ByteUtils.toUnsignedInt(b));*/
	}
}
