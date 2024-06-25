package com.hoonit.xeye.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;

public class Utils {
	
	public static byte[] getBytes(byte[] src, int offset, int length){
        byte[] dest = new byte[length];
        System.arraycopy(src, offset, dest, 0, length);
        return dest;
    }
	
	public static byte[] getFillNullByte(byte[] src, int length) throws ArrayIndexOutOfBoundsException{
		
		try{
			
			byte[] dest = new byte[length];
			
			int len = src.length;
			
			int i = 0;
			for(i = 0; i < len; i++){
				dest[i] = src[i];
			}
			
			if(dest.length < length){
				len = (length - dest.length);
				for( ; i < len; i++){
					dest[i] = 0x20;
				}
			}
			
			return dest;
			
		}catch(ArrayIndexOutOfBoundsException e){
			throw e;
		}
	}
	
	public static String nullToString(String source){
		
		if(source == null){
			return "";
		}else{
			return source;
		}
	}
	
	public static int getBitToByteSize(int bitSize){
		
		BitSet bits = new BitSet(bitSize);
		
		for(int i = 0; i < bitSize; i++){
			bits.set(i);
		}
		
		byte[] bytes = new byte[(bits.length() + 7) / 8];
	    for (int i=0; i<bits.length(); i++) {
	        if (bits.get(i)) {
	            bytes[bytes.length-i/8-1] |= 1<<(i%8);
	        }
	    }
	    
	    return bytes.length;
	}
	
	public static String convertByteToBit(byte b){
		
		StringBuffer result = new StringBuffer(3);
		result.append(Integer.toString((b & 0xF0) >> 4, 16));
		result.append(Integer.toString(b & 0x0F, 16));
		
		return hexToBin(result.toString());
	}
	
	//convert hex to bin
    private static String hexToBin(String hex) {
        hex = hex.toUpperCase();
        if (!isHexaDecimal(hex)) {
            return "00000000";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hex.length(); i++) {
            if (hex.charAt(i) == '0') {
                sb.append("0000");
            } else if (hex.charAt(i) == '1') {
                sb.append("0001");
            } else if (hex.charAt(i) == '2') {
                sb.append("0010");
            } else if (hex.charAt(i) == '3') {
                sb.append("0011");
            } else if (hex.charAt(i) == '4') {
                sb.append("0100");
            } else if (hex.charAt(i) == '5') {
                sb.append("0101");
            } else if (hex.charAt(i) == '6') {
                sb.append("0110");
            } else if (hex.charAt(i) == '7') {
                sb.append("0111");
            } else if (hex.charAt(i) == '8') {
                sb.append("1000");
            } else if (hex.charAt(i) == '9') {
                sb.append("1001");
            } else if (hex.charAt(i) == 'A') {
                sb.append("1010");
            } else if (hex.charAt(i) == 'B') {
                sb.append("1011");
            } else if (hex.charAt(i) == 'C') {
                sb.append("1100");
            } else if (hex.charAt(i) == 'D') {
                sb.append("1101");
            } else if (hex.charAt(i) == 'E') {
                sb.append("1110");
            } else if (hex.charAt(i) == 'F') {
                sb.append("1111");
            }
        }
        
        return sb.toString();
    }
    
    //is input represents a Hexadecimal number
    private static boolean isHexaDecimal(String bin) {
        return bin.toUpperCase().matches("[0-9|A-F]+");
    }
    
    public static String reverseString(String s) {
		return ( new StringBuffer(s) ).reverse().toString();
	}
    
    public static String getTime(){
    	
    	Calendar cal = Calendar.getInstance();
		
		int year   = cal.get(Calendar.YEAR);
		int month  = cal.get(Calendar.MONTH) + 1;
		int date   = cal.get(Calendar.DATE);
		int time   = cal.get(Calendar.HOUR_OF_DAY); // 0~23
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		
		String strYear   = String.valueOf(year);
		String strMonth  = (month > 9)  ? String.valueOf(month)  : String.valueOf("0" + month);
		String strDate   = (date > 9)   ? String.valueOf(date)   : String.valueOf("0" + date);
		String strTime   = (time > 9)   ? String.valueOf(time)   : String.valueOf("0" + time);
		String strMinute = (minute > 9) ? String.valueOf(minute) : String.valueOf("0" + minute);
		String strSecond = (second > 9) ? String.valueOf(second) : String.valueOf("0" + second);
		
		return strYear + "-" + strMonth + "-" + strDate + "-" + strTime + "-" + strMinute + "-" + strSecond;
    }
    
    // 냉난방정책
 	public static int getHACPolicy(int month){
 		
 		int result;
 		
 		switch(month){
 			case 1 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac1MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 2 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac2MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 3 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac3MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 4 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac4MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 5 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac5MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 6 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac6MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 7 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac7MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 8 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac8MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 9 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac9MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 10 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac10MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 11 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac11MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			case 12 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac12MPolicy(), "255")); // 1:난방, 2:냉방, 3: 환절기 
 				break;
 			default : 
 				result = 255;
 				break;
 		}
 		
 		return result;
 	}
 	
 	// 권장온도
 	public static int getHACTemp(int month){
 		
 		int result;
 		
 		switch(month){
 			case 1 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac1MTemp(), "255"));
 				break;
 			case 2 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac2MTemp(), "255"));
 				break;
 			case 3 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac3MTemp(), "255"));
 				break;
 			case 4 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac4MTemp(), "255"));
 				break;
 			case 5 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac5MTemp(), "255"));
 				break;
 			case 6 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac6MTemp(), "255"));
 				break;
 			case 7 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac7MTemp(), "255"));
 				break;
 			case 8 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac8MTemp(), "255"));
 				break;
 			case 9 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac9MTemp(), "255"));
 				break;
 			case 10 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac10MTemp(), "255"));
 				break;
 			case 11 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac11MTemp(), "255"));
 				break;
 			case 12 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac12MTemp(), "255"));
 				break;
 			default : 
 				result = 255;
 				break;
 		}
 		
 		return result;
 	}
 	
 	// 티센서 평균온도
 	public static int getAverageTemp() throws Exception{
 		
 		int temp = 0, deviceCnt = 0;
 		
		String bleConOIDVal = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".69.1.0"); // BLE연결 개수
		short bleConCnt = Short.parseShort(StringUtils.defaultIfEmpty(bleConOIDVal, "0"));
		
		List<String> list = new ArrayList<String>();
		
		short x = 0;
		
		// 무선티센서가 존재하면
		if(bleConCnt > 0){
			
			for(x = 0; x < 10; x++){
				list.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".1.0"); // 구분
				list.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".2.0"); // 온도
			}
			
			Map<String, String> map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
			
			for(x = 0; x < 10; x++){
				String portNo = map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".1.0");
				
				// Major가 16 이면 상온에 있는 센서
				if("16".equals(portNo)){
					temp += Integer.parseInt(StringUtils.defaultIfEmpty(map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".2.0"), "0"));
					deviceCnt++;
				}
			}
			
			if(deviceCnt > 0 && temp > 0)
				temp = (temp / deviceCnt);
		}
		// 무선티센서가 존재하지 않으면 유선티센서
		else{
			
			for(x = 0; x < 5; x++){
				list.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".1.0"); // 연결상태
				list.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".3.0"); // 온도
			}
			
			Map<String, String> map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
			
			for(x = 0; x < 5; x++){
				String gubun = map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".1.0");
				
				// 연결이 정상이면
				if("0".equals(gubun)){
					temp += Integer.parseInt(StringUtils.defaultIfEmpty(map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".3.0"), "0"));
					deviceCnt++;
				}
			}
			
			if(deviceCnt > 0 && temp > 0)
				temp = (temp / deviceCnt);
		}
 		
 		return temp;
 	}
 	
 	// 오류 응답이 필요한 command인지 판단
 	// STX, ETX, CRC
    public static boolean isErrorReturnCmd(byte cmd){
    	
    	boolean flag = true;
    	
    	// IF Server 접속(G->S)
    	if(cmd == 0x01){
    		flag = false;
    	}
    	// Data 전송(G->S)
    	else if(cmd == 0x02){
    		flag = false;
    	}
    	// 간판제어결과 전송(G->S)
    	else if(cmd == 0x09){
    		flag = false;
    	}
    	// 냉난방 온도제어결과 전송(G->S)
    	else if(cmd == 0x0A){
    		flag = false;
    	}
    	// 전력피크알람 전송(G->S)
		else if(cmd == 0x0C){
			flag = false;
		}
    	// 시간동기화 설정(G->S)
		else if(cmd == 0x0D){
			flag = false;
		}
    	// 날씨정보 요청(G->S)
		else if(cmd == 0x11){
			flag = false;
		}
    	
    	return flag;
    }
}
