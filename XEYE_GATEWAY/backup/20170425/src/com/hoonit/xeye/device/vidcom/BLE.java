package com.hoonit.xeye.device.vidcom;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.hoonit.xeye.device.ModbusBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.net.NetworkError;

public class BLE extends ModbusBase {
	
	public BLE(String deviceName, int protocol, int deviceType, String deviceOID, int unitID, String channel, String baudRate, List<Map<String, Object>> tagList){
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
								
								dest[0] = destTemp[0];
								dest[1] = destTemp[1];
								dest[2] = destTemp[2];
								dest[3] = destTemp[3];
						        
						        index += 4;
							}
							
							String data = getData(dataType, rate, dest);
					        
							if(tagMap.get("BIT_TAG") == null){
								
								if("-9999".equals(data) || "11000".equals(data)){
									
									logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + "-");
							        
								    // SNMP
								    resultMap.put(deviceOID + "." + oid + ".0", "-");
									
								}else{
									
									// 온도일 경우 변환작업
									if("INT16".equals(dataType)){
										
										/*logger.info("=====BLE Temp Original Value Info=====");
										logger.info("Hex=" + ModbusUtil.toHex(dest));
										logger.info("Data=" + data);*/
										
										BigDecimal bd1 = new BigDecimal(data);
										BigDecimal bd2 = new BigDecimal("0.01");
										
										String result = bd1.multiply(bd2).toString();
										
										//logger.info("result=" + result);
										
										if(result.contains(".")){
											
											StringTokenizer st = new StringTokenizer(result, ".");
											
											String val1 = st.nextToken();
											String val2 = st.nextToken();
											
											/*logger.info("val1=" + val1);
											logger.info("val2=" + val2);*/
											
											int g = Integer.parseInt(val1);
											
											//logger.info("g=" + g);
											
											byte v = (byte)g;
											
											//logger.info("v=" + v);
											
											data = String.valueOf(v)+ val2;
											
										}else{
											data = "0";
										}
									}
									
									if(Integer.parseInt(deviceOID.substring(deviceOID.lastIndexOf(".")+1)) >= 70){
										
										if("1".equals((String)tagMap.get("OID"))){
											
											ByteBuffer byteBuffer = ByteBuffer.allocate(2);
											byteBuffer.putShort(Short.parseShort(data));
											byteBuffer.flip();
											
											byte major = byteBuffer.get(0);
											byte minor = byteBuffer.get(1);
											
											data = Byte.toString(major) + Byte.toString(minor);
										}
									}
									
									logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
							        
								    // SNMP
								    resultMap.put(deviceOID + "." + oid + ".0", data);
								}
							}
						}
					}catch(Exception e){
						logger.error(e.getMessage(), e);
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
		return false;
	}
}
