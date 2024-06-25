package com.hoonit.xeye.device.vidcom;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hoonit.xeye.device.ModbusBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.util.Utils;

public class PMS8CH extends ModbusBase {
	
	private String ch1AccPwrAmtTotal = "0"; // Ch1 R/S/T 누적사용량 
	private String ch1PwrAmtTotal = "0";    // Ch1 R/S/T 순간사용량
	private String ch1CurrentTotal = "0";   // Ch1 R/S/T 전류
	
	private String ch2AccPwrAmtTotal = "0"; // Ch2 R/S/T 누적사용량 
	private String ch2PwrAmtTotal = "0";    // Ch2 R/S/T 순간사용량
	private String ch2CurrentTotal = "0";   // Ch2 R/S/T 전류
	
	private String ch3AccPwrAmtTotal = "0"; // Ch3 R/S/T 누적사용량 
	private String ch3PwrAmtTotal = "0";    // Ch3 R/S/T 순간사용량
	private String ch3CurrentTotal = "0";   // Ch3 R/S/T 전류
	
	private String ch4AccPwrAmtTotal = "0"; // Ch4 R/S/T 누적사용량 
	private String ch4PwrAmtTotal = "0";    // Ch4 R/S/T 순간사용량
	private String ch4CurrentTotal = "0";   // Ch4 R/S/T 전류
	
	private String ch5AccPwrAmtTotal = "0"; // Ch5 R/S/T 누적사용량 
	private String ch5PwrAmtTotal = "0";    // Ch5 R/S/T 순간사용량
	private String ch5CurrentTotal = "0";   // Ch5 R/S/T 전류
	
	private String ch6AccPwrAmtTotal = "0"; // Ch6 R/S/T 누적사용량 
	private String ch6PwrAmtTotal = "0";    // Ch6 R/S/T 순간사용량
	private String ch6CurrentTotal = "0";   // Ch6 R/S/T 전류
	/*
	private String ch7AccPwrAmtTotal = "0"; // Ch7 R/S/T 누적사용량 
	private String ch7PwrAmtTotal = "0";    // Ch7 R/S/T 순간사용량
	
	private String ch8AccPwrAmtTotal = "0"; // Ch8 R/S/T 누적사용량 
	private String ch8PwrAmtTotal = "0";    // Ch8 R/S/T 순간사용량
	*/
	public PMS8CH(String deviceName, int protocol, int deviceType, String deviceOID, int unitID, String channel, String baudRate, List<Map<String, Object>> tagList){
		super(deviceName, protocol, deviceType, deviceOID, unitID, channel, baudRate, tagList);
	}
	
	@Override
	public Object responseMessage(byte[] buffer) throws IOException{
		
		byte[] temp = getDataByte(buffer);
		
		if(temp == null){
			// 통신불량
			throw new IOException("Packet is null");
		}else{
			
			byte responseFC = getFunctionCode(buffer);
			
			Map<String, Object> tagGroup = reqTagGroup;
			
			Map<String, String> resultMap = new HashMap<String, String>();
			
			if(responseFC == 3 || responseFC == 4){
				
				int index = 0;
				
				List<Map<String, Object>> tagArr = (List<Map<String, Object>>)tagGroup.get("TAGS");
				
				Map<String, Object> tagMapTemp = tagArr.get(0);
				
				// 처음 주소가 40001이면
				if("40001".equals((String)tagMapTemp.get("ADDRESS"))){
					
					ch1AccPwrAmtTotal = "0";
					ch1PwrAmtTotal = "0";
					ch1CurrentTotal = "0";
					                               
					ch2AccPwrAmtTotal = "0";
					ch2PwrAmtTotal = "0";
					ch2CurrentTotal = "0";
					                               
					ch3AccPwrAmtTotal = "0";
					ch3PwrAmtTotal = "0";
					ch3CurrentTotal = "0";
					                               
					ch4AccPwrAmtTotal = "0";
					ch4PwrAmtTotal = "0";
					ch4CurrentTotal = "0";
					                               
					ch5AccPwrAmtTotal = "0";
					ch5PwrAmtTotal = "0";
					ch5CurrentTotal = "0";
					                               
					ch6AccPwrAmtTotal = "0";
					ch6PwrAmtTotal = "0";
					ch6CurrentTotal = "0";
					                               
					//ch7AccPwrAmtTotal = "0";
					//ch7PwrAmtTotal = "0";   
					                               
					//ch8AccPwrAmtTotal = "0";
					//ch8PwrAmtTotal = "0";   
				}
				
				for(Map<String, Object> tagMap : tagArr){
					
					try{
						
						String oid       = (String)tagMap.get("OID");
						String address   = (String)tagMap.get("ADDRESS");
						String dataType  = (String)tagMap.get("DATA_TYPE");
						String rate      = (String)tagMap.get("RATE");
						String monitorYN = ((String)tagMap.get("MONITOR_YN")).toUpperCase();
						
						if( "Y".equals(monitorYN) ){
							
							byte[] dest = null;
							
							if("INT16".equals(dataType) || "UINT16".equals(dataType)){
								
								dest = new byte[2];
						        System.arraycopy(temp, index, dest, 0, dest.length);
						        
						        index += 2;
						        
							}else if("INT32".equals(dataType)
									|| "UINT32".equals(dataType)
									|| "FLOAT32".equals(dataType)
									|| "SW_FLOAT32".equals(dataType)){
								
								dest = new byte[4];
								
								byte[] destTemp = new byte[4];
								
								System.arraycopy(temp, index, destTemp, 0, destTemp.length);
								
								dest[0] = destTemp[2];
								dest[1] = destTemp[3];
								dest[2] = destTemp[0];
								dest[3] = destTemp[1];
						        
						        index += 4;
							}
							
							String data = getData(dataType, rate, dest);
					        
							if(tagMap.get("BIT_TAG") == null){
								
								// Ch1 R/S/T 누적전력량
								if("40001".equals(address) || "40017".equals(address) || "40033".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch1AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch1AccPwrAmtTotal = bd1.add(bd2).toString();
									
								}
								// Ch2 R/S/T 누적전력량
								else if("40003".equals(address) || "40019".equals(address) || "40035".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch2AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch2AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch3 R/S/T 누적전력량
								else if("40005".equals(address) || "40021".equals(address) || "40037".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch3AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch3AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch4 R/S/T 누적전력량
								else if("40007".equals(address) || "40023".equals(address) || "40039".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch4AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch4AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch5 R/S/T 누적전력량
								else if("40009".equals(address) || "40025".equals(address) || "40041".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch5AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch5AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch6 R/S/T 누적전력량
								else if("40011".equals(address) || "40027".equals(address) || "40043".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch6AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch6AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								/*// Ch7 R/S/T 누적전력량
								else if("40013".equals(address) || "40029".equals(address) || "40045".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch7AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch7AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch8 R/S/T 누적전력량
								else if("40015".equals(address) || "40031".equals(address) || "40047".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch8AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch8AccPwrAmtTotal = bd1.add(bd2).toString();
								}*/
								// Ch1 R/S/T 순간전력량
								else if("40049".equals(address) || "40065".equals(address) || "40081".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch1PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch1PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch2 R/S/T 순간전력량
								else if("40051".equals(address) || "40067".equals(address) || "40083".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch2PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch2PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch3 R/S/T 순간전력량
								else if("40053".equals(address) || "40069".equals(address) || "40085".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch3PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch3PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch4 R/S/T 순간전력량
								else if("40055".equals(address) || "40071".equals(address) || "40087".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch4PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch4PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch5 R/S/T 순간전력량
								else if("40057".equals(address) || "40073".equals(address) || "40089".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch5PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch5PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch6 R/S/T 순간전력량
								else if("40059".equals(address) || "40075".equals(address) || "40091".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch6PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch6PwrAmtTotal = bd1.add(bd2).toString();
								}
								/*// Ch7 R/S/T 순간전력량
								else if("40061".equals(address) || "40077".equals(address) || "40093".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch7PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch7PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch8 R/S/T 순간전력량
								else if("40063".equals(address) || "40079".equals(address) || "40095".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch8PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch8PwrAmtTotal = bd1.add(bd2).toString();
								}*/
								// CH1 R/S/T 전류
								else if("40097".equals(address) || "40105".equals(address) || "40113".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch1CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch1CurrentTotal = bd1.add(bd2).toString();
								}
								// CH2 R/S/T 전류
								else if("40098".equals(address) || "40106".equals(address) || "40114".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch2CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch2CurrentTotal = bd1.add(bd2).toString();
								}
								// CH3 R/S/T 전류
								else if("40099".equals(address) || "40107".equals(address) || "40115".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch3CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch3CurrentTotal = bd1.add(bd2).toString();
								}
								// CH4 R/S/T 전류
								else if("40100".equals(address) || "40108".equals(address) || "40116".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch4CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch4CurrentTotal = bd1.add(bd2).toString();
								}
								// CH5 R/S/T 전류
								else if("40101".equals(address) || "40109".equals(address) || "40117".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch5CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch5CurrentTotal = bd1.add(bd2).toString();
								}
								// CH6 R/S/T 전류
								else if("40102".equals(address) || "40110".equals(address) || "40118".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch5CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch5CurrentTotal = bd1.add(bd2).toString();
								}
								
								logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
							        
							    // SNMP
							    resultMap.put(deviceOID + "." + oid + ".0", data);
							}else{
								
								int dword = Integer.parseInt(data);
	
								byte[] result = null;
	
								if ("INT32".equals(tagMap.get("DATA_TYPE")) || "UINT32".equals(tagMap.get("DATA_TYPE"))) {
	
									result = new byte[4];
									
									result[0] = (byte) ((dword & 0xFF000000) >> 24);
									result[1] = (byte) ((dword & 0x00FF0000) >> 16);
									result[2] = (byte) ((dword & 0x0000FF00) >> 8);
									result[3] = (byte) ((dword & 0x000000FF) >> 0);
								}else {
	
									result = new byte[2];
	
									result[0] = (byte) ((dword & 0x0000FF00) >> 8);
									result[1] = (byte) ((dword & 0x000000FF) >> 0);
								}
	
								if(result != null){
	
									StringBuffer sb = new StringBuffer();
	
									for(int i = 0; i < result.length; i++){
										sb.append(String.format("%8s", Integer.toBinaryString((result[i] + 256) % 256)).replace(' ', '0'));
									}
	
									data = sb.toString();
								}
								
								short bitCount = 16;
	
								if ("INT32".equals(tagMap.get("DATA_TYPE")) || "UINT32".equals(tagMap.get("DATA_TYPE"))) {
									bitCount = 32;
								}
								
								StringBuffer sb = new StringBuffer();
	
								if(data.length() == bitCount){
									for(short i = 0; i < bitCount; i+=8){
										sb.insert(0, new StringBuffer().append(data.substring(i, i+8)).reverse());
									}
								}
	
								data = sb.toString();
								
								char[] achar = data.toCharArray();
								
								List<Map<String, Object>> bitTagList = (List<Map<String, Object>>)tagMap.get("BIT_TAG");
								
								short idx = 0;
								
								for(Map<String, Object> bitTagMap : bitTagList){
									
									if("Y".equals( ((String)bitTagMap.get("MONITOR_YN")).toUpperCase() )){
										
										char val = achar[idx++];
										
										logger.info((String)bitTagMap.get("NAME") + "(" + (String)bitTagMap.get("OID") + ")=" + String.valueOf(val));
								        
								        // SNMP
								        resultMap.put(deviceOID + "." + (String)bitTagMap.get("OID") + ".0", String.valueOf(val));
									}else{
										idx++;
									}
								}
							}
						}
					}catch(Exception e){
						logger.error(e.getMessage(), e);
					}
				} // end for
				
				Map<String, Object> tagMap = tagArr.get(0);
				
				// 처음 주소가 40001이면
				if("40001".equals((String)tagMap.get("ADDRESS"))){
					
					// Ch1 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH1 전체 누적전력량(1)=" + ch1AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".1.0", ch1AccPwrAmtTotal);
				    
				    // Ch1 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH1 전체 순간전력량(2)=" + ch1PwrAmtTotal);
				    resultMap.put(deviceOID + ".2.0", ch1PwrAmtTotal);
				    
				    // Ch1 R/S/T 전류 SNMP 갱신
					logger.info("CH1 전체 전류(3)=" + ch1CurrentTotal);
				    resultMap.put(deviceOID + ".3.0", ch1CurrentTotal);
				    
				    // Ch2 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH2 전체 누적전력량(4)=" + ch2AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".4.0", ch2AccPwrAmtTotal);
				    
				    // Ch2 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH2 전체 순간전력량(5)=" + ch2PwrAmtTotal);
				    resultMap.put(deviceOID + ".5.0", ch2PwrAmtTotal);
				    
				    // Ch2 R/S/T 전류 SNMP 갱신
					logger.info("CH3 전체 전류(6)=" + ch2CurrentTotal);
				    resultMap.put(deviceOID + ".6.0", ch2CurrentTotal);
				    
				    // Ch3 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH3 전체 누적전력량(7)=" + ch3AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".7.0", ch3AccPwrAmtTotal);
				    
				    // Ch3 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH3 전체 순간전력량(8)=" + ch3PwrAmtTotal);
				    resultMap.put(deviceOID + ".8.0", ch3PwrAmtTotal);
				    
				    // Ch3 R/S/T 전류 SNMP 갱신
					logger.info("CH3 전체 전류(9)=" + ch3CurrentTotal);
				    resultMap.put(deviceOID + ".9.0", ch3CurrentTotal);
				    
				    // Ch4 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH4 전체 누적전력량(10)=" + ch4AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".10.0", ch4AccPwrAmtTotal);
				    
				    // Ch4 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH4 전체 순간전력량(11)=" + ch4PwrAmtTotal);
				    resultMap.put(deviceOID + ".11.0", ch4PwrAmtTotal);
				    
				    // Ch4 R/S/T 전류 SNMP 갱신
					logger.info("CH4 전체 전류(12)=" + ch4CurrentTotal);
				    resultMap.put(deviceOID + ".12.0", ch4CurrentTotal);
				    
				    // Ch5 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH5 전체 누적전력량(13)=" + ch5AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".13.0", ch5AccPwrAmtTotal);
				    
				    // Ch5 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH5 전체 순간전력량(14)=" + ch5PwrAmtTotal);
				    resultMap.put(deviceOID + ".14.0", ch5PwrAmtTotal);
				    
				    // Ch5 R/S/T 전류 SNMP 갱신
					logger.info("CH5 전체 전류(15)=" + ch5CurrentTotal);
				    resultMap.put(deviceOID + ".15.0", ch5CurrentTotal);
				    
				    // Ch6 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH6 전체 누적전력량(16)=" + ch6AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".16.0", ch6AccPwrAmtTotal);
				    
				    // Ch6 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH6 전체 순간전력량(17)=" + ch6PwrAmtTotal);
				    resultMap.put(deviceOID + ".17.0", ch6PwrAmtTotal);
				    
				    // Ch6 R/S/T 전류 SNMP 갱신
					logger.info("CH6 전체 전류(18)=" + ch6CurrentTotal);
				    resultMap.put(deviceOID + ".18.0", ch6CurrentTotal);
				    
				    /*
				    // Ch7 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH7 전체 누적전력량(122)=" + ch7AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".122.0", ch7AccPwrAmtTotal);
				    
				    // Ch7 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH7 전체 순간전력량(123)=" + ch7PwrAmtTotal);
				    resultMap.put(deviceOID + ".123.0", ch7PwrAmtTotal);
				    
				    // Ch8 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH8 전체 누적전력량(124)=" + ch8AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".124.0", ch8AccPwrAmtTotal);
				    
				    // Ch8 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH8 전체 순간전력량(125)=" + ch8PwrAmtTotal);
				    resultMap.put(deviceOID + ".125.0", ch8PwrAmtTotal);*/
				}
			}
			
			// 통신불량 - 정상
			Map<String, String> etcResultMap = new HashMap<String, String>();
			etcResultMap.put(deviceOID + ".0.0", NetworkError.NORMAL);
			
			notifyData(resultMap, etcResultMap);
		}
		
		return null;
	}
	
	@Override
	public Object responseMessageForWrite(byte[] buffer) throws IOException, Exception{
		
		String data = "";
		
		byte[] temp = getDataByte(buffer);
		
		if(temp != null){
			
			byte responseFC = getFunctionCode(buffer);
			
			if(responseFC == 3 || responseFC == 4){
				
				String oid       = (String)reqTagMap.get("OID");
				String dataType  = (String)reqTagMap.get("DATA_TYPE");
				String rate      = (String)reqTagMap.get("RATE");
				String monitorYN = ((String)reqTagMap.get("MONITOR_YN")).toUpperCase();
				
				if( "Y".equals(monitorYN) ){
					
					byte[] dest = null;
					
					if("INT16".equals(dataType) || "UINT16".equals(dataType)){
						
						dest = new byte[2];
						System.arraycopy(temp, 0, dest, 0, dest.length);
				        
					}else if("INT32".equals(dataType)
							|| "UINT32".equals(dataType)
							|| "FLOAT32".equals(dataType)
							|| "SW_FLOAT32".equals(dataType)){
						
						dest = new byte[4];
						
						if("SW_FLOAT32".equals(dataType)){
							
							dest[0] = temp[2];
							dest[1] = temp[3];
							dest[2] = temp[0];
							dest[3] = temp[1];
						}else{
							System.arraycopy(temp, 0, dest, 0, dest.length);
						}
					}
					
					data = getData(dataType, rate, dest);
			        
					if(reqTagMap.get("BIT_TAG") != null){
						
						int dword = Integer.parseInt(data);
	
						byte[] result = null;
						
						if ("INT32".equals(reqTagMap.get("DATA_TYPE")) || "UINT32".equals(reqTagMap.get("DATA_TYPE"))) {
	
							result = new byte[4];
							
							result[0] = (byte) ((dword & 0xFF000000) >> 24);
							result[1] = (byte) ((dword & 0x00FF0000) >> 16);
							result[2] = (byte) ((dword & 0x0000FF00) >> 8);
							result[3] = (byte) ((dword & 0x000000FF) >> 0);
						}else {
	
							result = new byte[2];
	
							result[0] = (byte) ((dword & 0x0000FF00) >> 8);
							result[1] = (byte) ((dword & 0x000000FF) >> 0);
						}
	
						if(result != null){
	
							StringBuffer sb = new StringBuffer();
	
							for(int i = 0; i < result.length; i++){
								sb.append(String.format("%8s", Integer.toBinaryString((result[i] + 256) % 256)).replace(' ', '0'));
							}
	
							data = sb.toString();
						}
						
						short bitCount = 16;
	
						if ("INT32".equals(reqTagMap.get("DATA_TYPE")) || "UINT32".equals(reqTagMap.get("DATA_TYPE"))) {
							bitCount = 32;
						}
						
						StringBuffer sb = new StringBuffer();
	
						if(data.length() == bitCount){
							for(short i = 0; i < bitCount; i+=8){
								sb.insert(0, new StringBuffer().append(data.substring(i, i+8)).reverse());
							}
						}
	
						data = sb.toString();
						
						char[] achar = data.toCharArray();
						
						sb.setLength(0);
						
						for(int i = 0; i < achar.length; i++){
							sb.append(String.valueOf(achar[i]));
						}
						
						data = sb.toString();
					}
				}
			}else{
				
				if(responseFC == 2 || responseFC == 1){
					
					StringBuffer bitSB = new StringBuffer();
					StringBuffer tempSB = null;
					
					for (byte b : temp) {
						
						tempSB = new StringBuffer();
						tempSB.append(Utils.convertByteToBit(b));
						
						bitSB.append(tempSB.reverse());
					}
					
					char[] achar = bitSB.toString().toCharArray();
					
					data = String.valueOf(achar[0]);
				}
			}
		}
        
        return data;
	}
	
	/**
	 * SNMP Write
	 */
	public boolean notifySet(SetEvent evt){
		
		boolean result = false;
		
		NetworkBase transObject = (NetworkBase)getTransObject();
		
		if(transObject.tryWriteLock()){
			
			try{
		
				logger.info("=====Control start=====");
				
				String oid 	= evt.getOid();
		    	String data = evt.getData();
		    	
		    	logger.info("oid=" + oid);
		    	logger.info("data=" + data);
		    	
		    	reqTagMap = getReqTagMap(oid);
				
				if(reqTagMap != null){
					
					//recentData = getRecentData(reqTagMap);
					
					String address  = (String)reqTagMap.get("ADDRESS");
					String dataType = (String)reqTagMap.get("DATA_TYPE");
					//String access   = (String)reqTagMap.get("ACCESS");
					String writeData = "";
					
					if("40151".equals(address)){
						
						short bitCnt = 16;
						
						BitSet bs = new BitSet();
						
						if("0".equals(data)){
							
							for(short i = 0; i < bitCnt; i++){
								bs.set(i, false);
							}
							
						}else{
							
							for(short i = 0; i < bitCnt; i++){
								
								if(i < 2){
									bs.set(i, true);
								}else{
									bs.set(i, false);
								}
							}
						}
						
						if(bs.toByteArray() != null){
							
							int newData = 0;
							
							for(byte bt : bs.toByteArray()){
								newData = (newData << 8) + (bt & 0xFF);
							}
							
							writeData = String.valueOf(newData);
						}
					}
					
					if(!"".equals(writeData)){
						
						result = doWriteData(data, recentData, writeData, address, dataType);
						
						try{
							
							Thread.sleep(1000);
							
							execute();
							
						}catch(Exception e){
						}
					}
				}
			}catch(Exception e){
				result = false;
			}finally{
				transObject.unWritelock();
			}
		}
		
		return result;
	}
}
