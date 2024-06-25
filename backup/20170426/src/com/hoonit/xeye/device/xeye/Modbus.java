package com.hoonit.xeye.device.xeye;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hoonit.xeye.device.ModbusBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.util.Utils;

public class Modbus extends ModbusBase {
	
	public Modbus(String deviceName, int protocol, int deviceType, String deviceOID, int unitID, String channel, String baudRate, List<Map<String, Object>> tagList){
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
					        
							if(tagMap.get("BIT_TAG") == null){
								
								if("-9999".equals(data) || "11000".equals(data)){
									
									logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + "-");
									
									// SNMP
								    resultMap.put(deviceOID + "." + oid + ".0", "-");
								}else{
									
									logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
									
								    // SNMP
								    resultMap.put(deviceOID + "." + oid + ".0", data);
								}
	
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
		    	
		    	reqTagMap = getReqTagMap(oid);
				
				if(reqTagMap != null){
					
					recentData = getRecentData(reqTagMap);
					
					String address  = (String)reqTagMap.get("ADDRESS");
					String dataType = (String)reqTagMap.get("DATA_TYPE");
					String writeData = "";
					
					if(reqTagMap.get("BIT_TAG") == null){
						 writeData = data;
					}else{
						
						char[] achar = recentData.toCharArray();
						recentData = String.valueOf(achar[bitTagIndex]);
						
						BitSet bs = new BitSet();
						
						for(short i = 0; i < achar.length; i++){
							
							if(i == bitTagIndex){
								if("1".equals(data)) bs.set(i, true);
								else bs.set(i, false);
							}else{
								if("1".equals(String.valueOf(achar[i]))) bs.set(i, true);
								else bs.set(i, false);
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
					}
				}
			}finally{
				transObject.unWritelock();
			}
		}
		
		return result;
	}
}
