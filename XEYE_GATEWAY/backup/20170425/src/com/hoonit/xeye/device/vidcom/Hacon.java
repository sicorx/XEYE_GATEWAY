package com.hoonit.xeye.device.vidcom;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snmp4j.agent.mo.MOScalar;

import com.hoonit.xeye.device.ModbusTHBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.net.snmp.DeviceMIB;
import com.hoonit.xeye.net.snmp.OctetStringOID;

public class Hacon extends ModbusTHBase {
	
	public Hacon(String deviceName, int protocol, int deviceType, String deviceOID, int unitID, String channel, String baudRate, List<Map<String, Object>> tagList){
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
				} // end for
			}
			
			// 통신불량 - 정상
			Map<String, String> etcResultMap = new HashMap<String, String>();
			etcResultMap.put(deviceOID + ".2.0", NetworkError.NORMAL);
			
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
		
		String oid 	= evt.getOid();
		
		String tempOID = oid.substring(0, oid.lastIndexOf("."));
    	String cTagOID = tempOID.substring(tempOID.lastIndexOf(".")+1);
    	
    	logger.info("cTagOID=" + cTagOID);
    	
    	String data = evt.getData();
    	
    	logger.info("data=" + data);
		
    	// 제어 태그일 경우에만 실제 제어를 수행한다.
    	if("8".equals(cTagOID) && "1".equals(data)){
    	
			NetworkBase transObject = (NetworkBase)getTransObject();
			
			if(transObject.tryWriteLock()){
				
				try{
			
					logger.info("=====Control start=====");
			    	
			    	logger.info("oid=" + oid);
			    	
			    	reqTagMap = getReqTagMap(oid);
			    	
			    	if(reqTagMap != null){
			    		
			    		recentData = "";
			    		
			    		String address  = (String)reqTagMap.get("ADDRESS");
						String dataType = (String)reqTagMap.get("DATA_TYPE");
						String writeData = "";
						
						ByteBuffer byteBuffer = ByteBuffer.allocate(2);
						
						DeviceMIB deviceMIB = getDeviceMIB();
						
						List<MOScalar> oidList = deviceMIB.getOIDList();
						
						String productNm  = "";
			    		String heatCool   = "";
			    		String temparture = "";
			    		
			    		String power = "";
			    		
			    		for(MOScalar scalar :  oidList){
			    			
			    			// ON/OFF
							if((deviceOID + "." + "7.0").equals("."+scalar.getOid().toString())){
								power = ((OctetStringOID)scalar).getValue().toString();
								break;
							}
						}
			    		
				    	// OFF 이면
				    	if("0".equals(power)){
				    		
				    		for(MOScalar scalar :  oidList){
				    			
				    			// 제조사
				    			// LG : 0, Samsung : 1
								if((deviceOID + "." + "4.0").equals("."+scalar.getOid().toString())){
									productNm = ((OctetStringOID)scalar).getValue().toString();
									break;
								}
							}
				    		
				    		logger.info("Product="+productNm);
				    		
				    		if(!"".equals(productNm)){
				    			
					    		BitSet bs = new BitSet();
					    		
					    		for(int i = 0; i < 3; i++){
					    			bs.set(i, false);
					    		}
					    		
					    		// 제조사
					    		// LG
					    		if("0".equals(productNm)){
					    			bs.set(3, false);
					    			bs.set(4, false);
					    			bs.set(5, false);
					    		}
					    		// Samsung
					    		else if("1".equals(productNm)){
					    			bs.set(3, false);
					    			bs.set(4, false);
					    			bs.set(5, true);
					    		}
					    		
					    		for(int i = 6; i < 16; i++){
					    			bs.set(i, false);
					    		}
					    		
					    		for(byte b : bs.toByteArray()){
					    			byteBuffer.put(b);
					    		}
					    		
					    		byteBuffer.flip();
				    		}
				    	}
				    	// ON 이면
				    	else{
				    		
				    		for(MOScalar scalar :  oidList){
				    			
				    			// 제조사
				    			// LG : 0, Samsung : 1
								if((deviceOID + "." + "4.0").equals("."+scalar.getOid().toString())){
									productNm = ((OctetStringOID)scalar).getValue().toString();
								}
								// 냉/난방
								else if((deviceOID + "." + "5.0").equals("."+scalar.getOid().toString())){
									heatCool = ((OctetStringOID)scalar).getValue().toString();
								}
								// 설정온도
								else if((deviceOID + "." + "6.0").equals("."+scalar.getOid().toString())){
									temparture = ((OctetStringOID)scalar).getValue().toString();
									break;
								}
							}
				    		
				    		logger.info("Product="+productNm);
				    		logger.info("HeatCool="+heatCool);
				    		logger.info("Temparture="+temparture);
				    		
				    		if(!"".equals(productNm) && !"".equals(heatCool) && !"".equals(temparture)){
				    		
					    		BitSet bs = new BitSet();
					    		
					    		bs.set(0, false);
					    		bs.set(1, false);
					    		bs.set(2, false);
					    		
					    		// 제조사
					    		// LG
					    		if("0".equals(productNm)){
					    			bs.set(3, false);
					    			bs.set(4, false);
					    			bs.set(5, false);
					    		}
					    		// Samsung
					    		else if("1".equals(productNm)){
					    			bs.set(3, false);
					    			bs.set(4, false);
					    			bs.set(5, true);
					    		}
					    		
					    		// 냉난방
					    		if("1".equals(heatCool)){
					    			bs.set(6, true);
					    		}else{
					    			bs.set(6, false);
					    		}
					    		
					    		// ON/OFF
					    		bs.set(7, true);
					    		
					    		byteBuffer.put(bs.toByteArray()[0]);
					    		byteBuffer.put(Byte.parseByte(temparture));
					    		byteBuffer.flip();
				    		}
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
    	}else{
    		result = true;
    	}
		
		return result;
	}
}
