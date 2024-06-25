package com.hoonit.xeye.test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.hoonit.xeye.device.ModbusBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;

public class HaconBak extends ModbusBase {
	
	public HaconBak(String deviceName, int protocol, int deviceType, String deviceOID, int unitID, String channel, String baudRate, List<Map<String, Object>> tagList){
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
							
							logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
						        
						    // SNMP
						    resultMap.put(deviceOID + "." + oid + ".0", data);
						}
						// 무선센서 배터리 잔량 및 통신상태
						else{
							
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
							
							//short idx = 0;
							
							for(Map<String, Object> bitTagMap : bitTagList){
								
								/*if("Y".equals( ((String)bitTagMap.get("MONITOR_YN")).toUpperCase() )){
									
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
										
										logger.info((String)bitTagMap.get("NAME") + "(" + (String)bitTagMap.get("OID") + ")=" + String.valueOf(val));
								        // SNMP
								        resultMap.put(deviceOID + "." + (String)bitTagMap.get("OID") + ".0", String.valueOf(val));
								        
								        idx += 14;
									}
									// ON/OFF, 냉/난방 이면
									else{
										
										char val = achar[idx++];
										
										logger.info((String)bitTagMap.get("NAME") + "(" + (String)bitTagMap.get("OID") + ")=" + String.valueOf(val));
								        // SNMP
								        resultMap.put(deviceOID + "." + (String)bitTagMap.get("OID") + ".0", String.valueOf(val));
									}
								}else{
									idx++;
								}*/
								
								StringBuffer tempsb = new StringBuffer();
								
								for(int i = 15; i >= 0; i--){
									tempsb.append(String.valueOf(achar[i]));
								}
								
								byte tempByte = (byte)Integer.parseInt(tempsb.toString(), 2);
								
								byte[] b = new byte[1];
								b[0] = tempByte;
								
								int val = new BigInteger(b).intValue();
								
								logger.info((String)bitTagMap.get("NAME") + "(" + (String)bitTagMap.get("OID") + ")=" + String.valueOf(val));
						        // SNMP
						        resultMap.put(deviceOID + "." + (String)bitTagMap.get("OID") + ".0", String.valueOf(val));
							}
						}
					}
				} // end for
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
		    		
		    		recentData = "";
		    		
		    		String address  = (String)reqTagMap.get("ADDRESS");
					String dataType = (String)reqTagMap.get("DATA_TYPE");
					String writeData = "";
					
					ByteBuffer byteBuffer = ByteBuffer.allocate(2);
					
			    	// OFF 이면
			    	if("0".equals(data)){
			    		
			    		BitSet bs = new BitSet();
			    		
			    		for(int i = 0; i < 16; i++){
			    			bs.set(i, false);
			    		}
			    		
			    		for(byte b : bs.toByteArray()){
			    			byteBuffer.put(b);
			    		}
			    		
			    		byteBuffer.flip();
			    	}
			    	// ON 이면
			    	else{
			    		
			    		StringTokenizer st = new StringTokenizer(data, ",");
			    		
			    		String gubun1 = "1";            // ON/OFF(0:OFF, 1:ON)
			    		String gubun2 = st.nextToken(); // 냉/난방(0:난방, 1:냉방)
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
			    		
			    		byteBuffer.put(bs.toByteArray()[0]);
			    		byteBuffer.put(Byte.parseByte(gubun3));
			    		byteBuffer.flip();
			    	}
			    	
			    	if(byteBuffer.array() != null){
						
						int newData = 0;
						
						for(byte bt : byteBuffer.array()){
							newData = (newData << 8) + (bt & 0xFF);
						}
						
						writeData = String.valueOf(newData);
					}
			    	
			    	if(!"".equals(writeData)){
			    		
						result = doWriteData(data, recentData, writeData, address, dataType);
						
						try{
							
							Thread.sleep(2000);
							
							execute();
							
						}catch(Exception e){
						}
					}
		    	}
				
			}finally{
				transObject.unWritelock();
			}
		}
		
		return result;
	}
}
