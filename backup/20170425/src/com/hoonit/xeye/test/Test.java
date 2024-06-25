package com.hoonit.xeye.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.BitSet;
import java.util.StringTokenizer;

import com.hoonit.xeye.util.Utils;

import net.wimpi.modbus.util.ModbusUtil;

public class Test {

	public static void main(String[] args) throws Exception{
		
		//short b = 3;
		
		/*String r = String.format("%8s", Integer.toBinaryString((b + 256) % 256)).replace(' ', '0');
		
		StringBuffer sb = new StringBuffer().append(r).reverse();
		
		char[] achar = sb.toString().toCharArray();
		
		for(int i = 0; i < achar.length; i++){
			System.out.println(achar[i]);
		}*/
		
		//System.out.println((int)b/100.0);
		
		String data = "0,1,16";
		
		String writeData = "";
		
		StringTokenizer st = new StringTokenizer(data, ",");
		
		String gubun1 = st.nextToken(); // 냉/난방(0:난방, 1:냉방)
		String gubun2 = st.nextToken(); // ON/OFF(0:OFF, 1:ON)
		String gubun3 = st.nextToken(); // 설정온도
		
		BitSet bs = new BitSet();
		
		for(short bitIdx = 0; bitIdx < 8; bitIdx++){
			
			if(bitIdx == 6){
	    		if("0".equals(gubun1)){
					bs.set(bitIdx, false);
				}else{
					bs.set(bitIdx, true);
				}
			}else if(bitIdx == 7){
    		
	    		if("0".equals(gubun2)){
					bs.set(bitIdx, false);
				}else{
					bs.set(bitIdx, true);
				}
			}else{
				bs.set(bitIdx, false);
			}
		}
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(2);
		byteBuffer.put(bs.toByteArray()[0]);
		byteBuffer.put(Byte.parseByte(gubun3));
		byteBuffer.flip();
		
		if(byteBuffer.array() != null){
			
			int newData = 0;
			
			for(byte bt : byteBuffer.array()){
				newData = (newData << 8) + (bt & 0xFF);
			}
			
			writeData = String.valueOf(newData);
		}
    	
    	if(!"".equals(writeData)){
			System.out.println(writeData);
		}
		
		// 데이터 검증
    	writeData = "32791";
    	
    	StringBuffer sb = null;
    	
		int dword2 = Integer.parseInt(writeData);
		
		byte[] result = new byte[2];
		
		result[0] = (byte) ((dword2 & 0x0000FF00) >> 8);
		result[1] = (byte) ((dword2 & 0x000000FF) >> 0);
		
		if(result != null){

			sb = new StringBuffer();

			for(int i = 0; i < result.length; i++){
				sb.append(String.format("%8s", Integer.toBinaryString((result[i] + 256) % 256)).replace(' ', '0'));
			}

			data = sb.toString();
		}
		
		short bitCount = 16;
		
		sb = new StringBuffer();

		if(data.length() == bitCount){
			for(short i = 0; i < bitCount; i+=8){
				sb.insert(0, new StringBuffer().append(data.substring(i, i+8)).reverse());
			}
		}

		data = sb.toString();
		
		System.out.println("char arr:" + data);
		
		char[] achar = data.toCharArray();
		
		short idx = 0;
		
		for(int x = 0; x < 3; x++){
			
			// 설정온도
			if(idx == 0){
				
				StringBuffer tempsb = new StringBuffer();
				
				for(int i = 7; i >= 0; i--){
					tempsb.append(String.valueOf(achar[i]));
				}
				
				byte tempByte = (byte)Integer.parseInt(tempsb.toString(), 2);
				
				byte[] b = new byte[1];
				b[0] = tempByte;
				
				int val = new BigInteger(b).intValue();
				
				System.out.println("idx("+idx+")=" + String.valueOf(val));
		        
		        idx += 14;
			}
			// ON/OFF, 냉/난방 이면
			else{
				
				char val = achar[idx++];
				
				System.out.println("idx("+idx+")=" + String.valueOf(val));
			}
		}
		
    	/*BitSet bs = new BitSet();
		bs.set(0, false);
		bs.set(1, false);
		bs.set(2, false);
		bs.set(3, false);
		bs.set(4, false);
		bs.set(5, false);
		bs.set(6, false);
		bs.set(7, false);
		bs.set(8, false);
		bs.set(9, false);
		bs.set(10, false);
		bs.set(11, false);
		bs.set(12, false);
		bs.set(13, false);
		bs.set(14, true);
		bs.set(15, true);
		
		short s = ModbusUtil.registerToShort(bs.toByteArray());
		
		System.out.println(s);
		
		byte[] result = new byte[2];
		
		result[0] = (byte) ((s & 0x0000FF00) >> 8);
		result[1] = (byte) ((s & 0x000000FF) >> 0);
		
		StringBuffer sb = new StringBuffer();
		
		for(int i = 0; i < result.length; i++){
			sb.append(String.format("%8s", Integer.toBinaryString((result[i] + 256) % 256)).replace(' ', '0'));
		}
		
		String data = sb.toString();
		
		System.out.println(data);
		
		short bitCount = 16;
		
		sb = new StringBuffer();

		if(data.length() == bitCount){
			for(short i = 0; i < bitCount; i+=8){
				sb.insert(0, new StringBuffer().append(data.substring(i, i+8)).reverse());
			}
		}

		System.out.println(sb.toString());*/
    	
    	/*bs = new BitSet();
		bs.set(0, false);
		bs.set(1, false);
		bs.set(2, false);
		bs.set(3, false);
		bs.set(4, false);
		bs.set(5, false);
		bs.set(6, true);
		bs.set(7, true);
		
		System.out.println(bs.toByteArray()[0]);*/
    	
    	/*byte[] tb = new byte[2];
    	tb[0] = (byte)-64; // ON/OFF
    	tb[1] = (byte)16;  // 냉/난방
    	
    	int s = ModbusUtil.registerToUnsignedShort(tb);
    	
    	System.out.println(s);*/
	}
}
