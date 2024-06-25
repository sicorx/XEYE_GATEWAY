package com.hoonit.xeye.device.vidcom;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.hoonit.xeye.device.ModbusBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.util.Utils;

public class Temon extends ModbusBase {
	
	public Temon(String deviceName, int protocol, int deviceType, String deviceOID, int unitID, String channel, String baudRate, List<Map<String, Object>> tagList){
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
				
				for(Map<String, Object> tagMap : tagArr){
					
					try{
						
						String oid       = (String)tagMap.get("OID");
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
								
								if("SW_FLOAT32".equals(dataType)){
									
									byte[] destTemp = new byte[4];
									
									System.arraycopy(temp, index, destTemp, 0, destTemp.length);
									
									dest[0] = destTemp[2];
									dest[1] = destTemp[3];
									dest[2] = destTemp[0];
									dest[3] = destTemp[1];
								}else{
							        System.arraycopy(temp, index, dest, 0, dest.length);
								}
								
						        index += 4;
							}
							
							String data = getData(dataType, rate, dest);
								
							if("-9999".equals(data) || "11000".equals(data)){
								
								logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + "-");
								
								// SNMP
							    resultMap.put(deviceOID + "." + oid + ".0", "-");
							}else{
								
								BigDecimal bd1 = new BigDecimal(data);
								BigDecimal bd2 = new BigDecimal("0.01");
								
								String result = bd1.multiply(bd2).toString();
								
								if(result.contains(".")){
									
									StringTokenizer st = new StringTokenizer(result, ".");
									
									String val1 = st.nextToken();
									String val2 = st.nextToken();
									
									int g = Integer.parseInt(val1);
									
									byte v = (byte)g;
									
									data = String.valueOf(v)+ val2;
									
								}else{
									data = "0";
								}
								
								logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
								
							    // SNMP
							    resultMap.put(deviceOID + "." + oid + ".0", data);
							}
						}
					}catch(Exception e){
						logger.error(e.getMessage(), e);
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
					
					List<Map<String, Object>> tagArr = (List<Map<String, Object>>)tagGroup.get("TAGS");
					
					int index = 0;
					int nextAddress = 0;
					
					for(Map<String, Object> tagMap : tagArr){
						
						try{
							
							int address = Integer.parseInt((String)tagMap.get("ADDRESS"));
							
							if(index == 0){
								
								nextAddress = address + 1;
								
								if("Y".equals( ((String)tagMap.get("MONITOR_YN")).toUpperCase() )){
									
									logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + String.valueOf(achar[index]));
							        
							        // SNMP
							        resultMap.put(deviceOID + "." + (String)tagMap.get("OID") + ".0", String.valueOf(achar[index]));
								}
							}else{
								
								if(nextAddress == address){
									
									nextAddress = address + 1;
									
									if("Y".equals( ((String)tagMap.get("MONITOR_YN")).toUpperCase() )){
										
										logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + String.valueOf(achar[index]));
								        
								        // SNMP
								        resultMap.put(deviceOID + "." + (String)tagMap.get("OID") + ".0", String.valueOf(achar[index]));
									}
								}
							}
							
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}
						
						index++;
					}
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
        return null;
	}
	
	/**
	 * SNMP Write
	 */
	public boolean notifySet(SetEvent evt){
		return false;
	}
}
