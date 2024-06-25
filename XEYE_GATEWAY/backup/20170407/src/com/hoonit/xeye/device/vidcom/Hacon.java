package com.hoonit.xeye.device.vidcom;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snmp4j.agent.mo.MOScalar;

import com.hoonit.xeye.device.ModbusBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.net.snmp.DeviceMIB;
import com.hoonit.xeye.net.snmp.OctetStringOID;

public class Hacon extends ModbusBase {
	
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
							
							if("-9999".equals(data) || "11000".equals(data)){
								
								logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + "-");
								
								// SNMP
							    resultMap.put(deviceOID + "." + oid + ".0", "-");
							}else{
								
								logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
								
							    // SNMP
							    resultMap.put(deviceOID + "." + oid + ".0", data);
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
		
		boolean result = false;
		
		String oid 	= evt.getOid();
		
		String tempOID = oid.substring(0, oid.lastIndexOf("."));
    	String cTagOID = tempOID.substring(tempOID.lastIndexOf(".")+1);
    	
    	logger.info("cTagOID=" + cTagOID);
		
    	// ON/OFF 태그일 경우에만 실제 제어를 수행한다.
    	if("6".equals(cTagOID)){
    	
			NetworkBase transObject = (NetworkBase)getTransObject();
			
			if(transObject.tryWriteLock()){
				
				try{
			
					logger.info("=====Control start=====");
					
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
						
						DeviceMIB deviceMIB = getDeviceMIB();
						
						List<MOScalar> oidList = deviceMIB.getOIDList();
						
						String productNm  = "";
			    		String heatCool   = "";
			    		String temparture = "";
			    		
				    	// OFF 이면
				    	if("0".equals(data)){
				    		
				    		for(MOScalar scalar :  oidList){
				    			
				    			// 제조사
				    			// LG : 0, Samsung : 1
								if((deviceOID + "." + "3.0").equals("."+scalar.getOid().toString())){
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
								if((deviceOID + "." + "3.0").equals("."+scalar.getOid().toString())){
									productNm = ((OctetStringOID)scalar).getValue().toString();
								}
								// 냉/난방
								else if((deviceOID + "." + "4.0").equals("."+scalar.getOid().toString())){
									heatCool = ((OctetStringOID)scalar).getValue().toString();
								}
								// 설정온도
								else if((deviceOID + "." + "5.0").equals("."+scalar.getOid().toString())){
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
