package com.hoonit.xeye.device.carrier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.OID;

import com.hoonit.xeye.device.DeviceBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.net.serial.Serial;
import com.hoonit.xeye.net.snmp.DeviceMIB;
import com.hoonit.xeye.net.snmp.OctetStringOID;
import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.pi4j.io.serial.SerialDataEvent;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import net.wimpi.modbus.util.ModbusUtil;

public class InverterHub extends DeviceBase{

	private static int BUFFER_SIZE = 260;
	
	private static int UNIT_ID = 2;
	private static int FC = 3;
	
	private static final byte REQ_STATUS = 0x00;  // 상태요구
	private static final byte REQ_CONTROL = 0x01; // 제어요구
	private static final byte REQ_RESET = 0x08;   // Rset 제어
	
	
	public InverterHub(String deviceName, int protocol, int deviceType, String deviceOID, List<Map<String, Object>> tagList){
		
		super(deviceName, protocol, deviceType, deviceOID, tagList);
	}
	
	private void clearInputStream(DataInputStream inputStream) throws IOException {
		
		 if (inputStream.available() > 0) {
			 int len = inputStream.available();
			 byte buf[] = new byte[len];
			 inputStream.read(buf, 0, len);
			 logger.info("Clear input: " + ModbusUtil.toHex(buf, 0, len));
	    }
	}
	
	@Override
	public void dataReceived(SerialDataEvent event) {
	}
	
	@Override
	public byte[] requestMessage() throws IOException{
		return null;
	}
	
	@Override
	public byte[] requestMessage(String channel, String data) throws IOException{
		return null;
	}
	
	@Override
	public Object responseMessage(byte[] buffer) throws IOException{
		return null;
	}
	
	private byte[] requestMessage(byte command) throws IOException, Exception{
		
		byte[] buffer = null;
		
		if(command == REQ_STATUS){
			
			buffer = new byte[9];
			buffer[0] = (byte)UNIT_ID;
			buffer[1] = (byte)FC;
			buffer[2] = (byte)0x02; // STX
			buffer[3] = (byte)0x04; // LEN
			buffer[4] = (byte)0xFF; // TYPE
			buffer[5] = command;    // COMMAND
			
			// CRC
			byte[] c = new byte[1];
			c[0] = command;
			
			short s = CRC16.getInstance().getCRC(c);
			
			byte[] crcBytes = new byte[2];
			crcBytes[0] = (byte)(s & 0x00FF);
			crcBytes[1] = (byte)((s & 0xFF00)>>8);
			
			buffer[6] = crcBytes[0];
			buffer[7] = crcBytes[1];
			
			buffer[8] = (byte)0x03; // ETX
		}
		
		if(buffer != null){
			
			StringBuffer sb = new StringBuffer();
			
			for (int i = 0; i < buffer.length; i++) {
				sb.append(ByteUtils.toHexString((byte) buffer[i])).append(" ");
			}
			
			logger.info(sb.toString());
		}
		
		return buffer;
	}
	
	@Override
	public void execute() throws IOException{
		
		// Serial일 경우
		if(getTransObject() instanceof Serial){
			
			com.hoonit.xeye.net.serial.Serial transObject = (com.hoonit.xeye.net.serial.Serial)getTransObject();
			
			SerialPort serialPort         = transObject.getSerialPort();
			DataInputStream inputStream   = transObject.getInputStream();
			DataOutputStream outputStream = transObject.getOutputStream();
			
			try{
				
				// 상태요구
				byte[] bytes = requestMessage(REQ_STATUS);
				
				clearInputStream(inputStream);
				
				outputStream.write(bytes, 0, bytes.length);
	    		outputStream.flush();
	    		
	    		try {
					serialPort.enableReceiveThreshold(BUFFER_SIZE);
				} catch (UnsupportedCommOperationException e) {
					logger.error(e.getMessage(), e);
				}
	    		
	    		byte[] buffer = new byte[BUFFER_SIZE];
	    		
	    		inputStream.read(buffer, 0, buffer.length);
	    		
	    		serialPort.disableReceiveThreshold();
	    		
	    		// 상태응답 처리
	    		int idx = 0;
	    		
	    		byte stx = buffer[idx++]; // STX
	    		
	    		if(stx == 2){
	    		
		    		byte len          = buffer[idx++]; // LEN
		    		byte reserved     = buffer[idx++]; // RESERVED
		    		byte command      = buffer[idx++]; // COMMAND
		    		
		    		// 냉장장비개수
		    		int n = (int)buffer[idx++];
		    		
		    		Map<String, String> dataMap = new HashMap<String, String>();
		    		dataMap.put(String.valueOf((36*8)+1), String.valueOf(n)); // 냉장장비개수
		    		
		    		int oidIdx = 0;
		    		
		    		for(int i = 0; i < n; i++){
		    			
		    			byte addr = buffer[idx++];
		    			
		    			// Address에 따른 OID 시작 Index 계산
		    			oidIdx = (addr * 33) + 1;
		    			
		    			byte fwVer = buffer[idx++];
		    			byte type = buffer[idx++];       // 0:Cooler, 1:Freezer
		    			byte model = buffer[idx++];      // 0:정속형, 1:인버터
		    			byte capacity = buffer[idx++];   // 0:0.0kw, 1:0.1kw ~ 255:25.5kw
		    			
		    			// 알람 MASK1
		    			byte alarmMask1 = buffer[idx++];
		    			String r = String.format("%8s", Integer.toBinaryString(alarmMask1 & 0xFF)).replace(' ', '0');
		    			StringBuffer sb = new StringBuffer().append(r).reverse();
		    			
		    			char[] achar = sb.toString().toCharArray();
		    			
		    			String am1HighTemp = ""; // 알람 MASK1-실내고온알람(0:Disable, 0:Enable)
		    			String am1LowTemp  = ""; // 알람 MASK1-실내저온알람(0:Disable, 0:Enable)
		    			String am1TRA      = ""; // 알람 MASK1-실내온도센서(TRA)(0:Disable, 0:Enable)
		    			String am1TDF      = ""; // 알람 MASK1-실내제상온도센서(TDF)(0:Disable, 0:Enable)
		    			String am1HighPre  = ""; // 알람 MASK1-고압알람(0:Disable, 0:Enable)
		    			String am1LowPre   = ""; // 알람 MASK1-저압알람(0:Disable, 0:Enable)
		    			String am1Pre      = ""; // 알람 MASK1-압축기알람(0:Disable, 0:Enable)
		    			String am1Link     = ""; // 알람 MASK1-Reserved(0:Disable, 0:Enable)
		    			
		    			for(int x = 0; x < achar.length; x++){
		    				
		    				if(x == 0)
		    					am1HighTemp = String.valueOf(achar[x]);
		    				else if(x == 1)
		    					am1LowTemp = String.valueOf(achar[x]);
		    				else if(x == 2)
		    					am1TRA = String.valueOf(achar[x]);
		    				else if(x == 3)
		    					am1TDF = String.valueOf(achar[x]);
		    				else if(x == 4)
		    					am1HighPre = String.valueOf(achar[x]);
		    				else if(x == 5)
		    					am1LowPre = String.valueOf(achar[x]);
		    				else if(x == 6)
		    					am1Pre = String.valueOf(achar[x]);
		    				else
		    					am1Link = String.valueOf(achar[x]);
		    			}
		    			
		    			// 알람 MASK2 사용무
		    			idx++;
		    			
		    			// 알람1
		    			byte alarm1 = buffer[idx++];
		    			r = String.format("%8s", Integer.toBinaryString(alarm1 & 0xFF)).replace(' ', '0');
		    			sb = new StringBuffer().append(r).reverse();
		    			
		    			achar = sb.toString().toCharArray();
		    			
		    			String alm1HighTemp = ""; // 알람1-실내고온알람(1:Alarm, 0:Normal)
		    			String alm1LowTemp  = ""; // 알람1-실내저온알람(1:Alarm, 0:Normal)
		    			String alm1TRA      = ""; // 알람1-실내온도센서(TRA)(1:Alarm, 0:Normal)
		    			String alm1TDF      = ""; // 알람1-실내제상온도센서(TDF)(1:Alarm, 0:Normal)
		    			String alm1HighPre  = ""; // 알람1-고압알람(1:Alarm, 0:Normal)
		    			String alm1LowPre   = ""; // 알람1-저압알람(1:Alarm, 0:Normal)
		    			String alm1Pre      = ""; // 알람1-압축기알람(1:Alarm, 0:Normal)
		    			String alm1Link     = ""; // 알람1-압축기알람(1:Alarm, 0:Normal)
		    			
		    			for(int x = 0; x < achar.length; x++){
		    				
		    				if(x == 0)
		    					alm1HighTemp = String.valueOf(achar[x]);
		    				else if(x == 1)
		    					alm1LowTemp = String.valueOf(achar[x]);
		    				else if(x == 2)
		    					alm1TRA = String.valueOf(achar[x]);
		    				else if(x == 3)
		    					alm1TDF = String.valueOf(achar[x]);
		    				else if(x == 4)
		    					alm1HighPre = String.valueOf(achar[x]);
		    				else if(x == 5)
		    					alm1LowPre = String.valueOf(achar[x]);
		    				else if(x == 6)
		    					alm1Pre = String.valueOf(achar[x]);
		    				else
		    					alm1Link = String.valueOf(achar[x]);
		    			}
		    			
		    			// 알람2 사용무
		    			idx++;
		    			
		    			byte errCD = buffer[idx++];      // 에러코드(0~99)
		    			byte handleMode = buffer[idx++]; // 운전모드(0:정지, 17:냉장/냉동, 18:제상, 19: 제수)
		    			
		    			// 실내온도
		    			byte[] b = new byte[2];
				        System.arraycopy(buffer, idx, b, 0, b.length);
				        
				        short temp = ModbusUtil.registerToShort(b);
				        
				        idx += 2;
				        
				        // 실내설정온도
				        b = new byte[2];
				        System.arraycopy(buffer, idx, b, 0, b.length);
				        
				        short setTemp = ModbusUtil.registerToShort(b);
				        
				        idx += 2;
				        
				        // 실외기온도
				        b = new byte[2];
				        System.arraycopy(buffer, idx, b, 0, b.length);
				        
				        short outdoorTemp = ModbusUtil.registerToShort(b);
				        
				        idx += 2;
				        
				        // 고온경보사용유무
				        byte highSetTempYN = buffer[idx++]; // (0:Disable, 1:Enable)
				        
				        // 고온경보설정온도
		    			byte highSetTemp = buffer[idx++];
		    			
		    			// 저온경보사용유무
				        byte lowSetTempYN = buffer[idx++]; // (0:Disable, 1:Enable)
				        
				        // 저온경보설정온도
		    			byte lowSetTemp = buffer[idx++];
		    			
		    			// 실내제상온도
				        b = new byte[2];
				        System.arraycopy(buffer, idx, b, 0, b.length);
				        
				        short coolerTemp = ModbusUtil.registerToShort(b);
				        
				        idx += 2;
				        
				        // 제상/제수 동작기준 선택
				        byte move = buffer[idx++]; // (0:Master, 1: Slave)
				        
				        // 제상복귀온도
				        byte coolerRtnTemp = buffer[idx++];
				        
				        // 제상간격
				        byte coolerTerm = buffer[idx++];
				        
				        // 제상시간
				        byte coolerTime = buffer[idx++];
				        
				        // 제수시간
				        byte waterTime = buffer[idx++];
				        
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(addr));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(fwVer));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(type));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(model));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(capacity));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(am1HighTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(am1LowTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(am1TRA));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(am1TDF));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(am1HighPre));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(am1LowPre));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(am1Pre));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(am1Link));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(alm1HighTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(alm1LowTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(alm1TRA));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(alm1TDF));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(alm1HighPre));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(alm1LowPre));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(alm1Pre));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(alm1Link));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(errCD));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(handleMode));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(temp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(setTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(outdoorTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(highSetTempYN));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(highSetTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(lowSetTempYN));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(lowSetTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(coolerTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(move));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(coolerRtnTemp));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(coolerTerm));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(coolerTime));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf(waterTime));
		    		}
		    		
		    		// 냉장장비개수가 8보다 작을경우 나머지값 세팅
		    		for(int i = 0; i < (8-n); i++){
		    			
		    			dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
		    		}
		    		
		    		Map<String, String> resultMap = new HashMap<String, String>();
		            
		            // 통신불량
			        Map<String, String> etcResultMap = new HashMap<String, String>();
			        
			        for(Map<String, Object> tagMap : tagList){
			        	
			        	if( "Y".equals( ((String)tagMap.get("MONITOR_YN")).toUpperCase() ) && !"W".equals( ((String)tagMap.get("ACCESS")).toUpperCase() )){
			        		
			        		if("0".equals(((String)tagMap.get("OID")))){
			        			
			    				etcResultMap.put(deviceOID + "." + tagMap.get("OID") + ".0", NetworkError.NORMAL);
			        			
			        		}else{
			        			
			        			if(dataMap.get(tagMap.get("OID")) != null){
			        				
			        				String data = dataMap.get(tagMap.get("OID"));
			        				
				        			logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
				        			
				        			// SNMP
					        		resultMap.put(deviceOID + "." + tagMap.get("OID") + ".0", data);
			        			}
			        		}
			        	}
			        }
			        
					notifyData(resultMap, etcResultMap);
					
					networkErrorCnt = 0;
	    		}else{
	    			// 통신불량
		        	//setNetworkError("1");
	    			
	    			Map<String, String> dataMap = new HashMap<String, String>();
		    		dataMap.put(String.valueOf((36*8)+1), "0"); // 냉장장비개수
		    		
		    		int oidIdx = 1;
		    		
					for(int i = 0; i < 8; i++){
		    			
						dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
				        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
		    		}
					
					Map<String, String> resultMap = new HashMap<String, String>();
		    		
		    		// 통신불량
			        Map<String, String> etcResultMap = new HashMap<String, String>();
			        
			        for(Map<String, Object> tagMap : tagList){
			        	
			        	if( "Y".equals( ((String)tagMap.get("MONITOR_YN")).toUpperCase() ) && !"W".equals( ((String)tagMap.get("ACCESS")).toUpperCase() )){
			        		
			        		if("0".equals(((String)tagMap.get("OID")))){
			        			
			    				etcResultMap.put(deviceOID + "." + tagMap.get("OID") + ".0", NetworkError.ABNORMAL);
			        			
			        		}else{
			        			
			        			if(dataMap.get(tagMap.get("OID")) != null){
			        				
			        				String data = dataMap.get(tagMap.get("OID"));
			        				
				        			logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
				        			
				        			// SNMP
					        		resultMap.put(deviceOID + "." + tagMap.get("OID") + ".0", data);
			        			}
			        		}
			        	}
			        }
			        
					notifyData(resultMap, etcResultMap);
	    		}
				
			}catch(IOException e){
				logger.error(e.getMessage(), e);
				
				serialPort.disableReceiveThreshold();
				
				// 통신불량
	        	setNetworkError("1");
				
				/*Map<String, String> dataMap = new HashMap<String, String>();
	    		dataMap.put(String.valueOf((36*8)+1), "0"); // 냉장장비개수
	    		
	    		int oidIdx = 1;
	    		
				for(int i = 0; i < 8; i++){
	    			
					dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
			        dataMap.put(String.valueOf(oidIdx++), String.valueOf("-"));
	    		}
				
				Map<String, String> resultMap = new HashMap<String, String>();
	    		
	    		// 통신불량
		        Map<String, String> etcResultMap = new HashMap<String, String>();
		        
		        for(Map<String, Object> tagMap : tagList){
		        	
		        	if( "Y".equals( ((String)tagMap.get("MONITOR_YN")).toUpperCase() ) && !"W".equals( ((String)tagMap.get("ACCESS")).toUpperCase() )){
		        		
		        		if("0".equals(((String)tagMap.get("OID")))){
		        			
		    				etcResultMap.put(deviceOID + "." + tagMap.get("OID") + ".0", NetworkError.ABNORMAL);
		        			
		        		}else{
		        			
		        			if(dataMap.get(tagMap.get("OID")) != null){
		        				
		        				String data = dataMap.get(tagMap.get("OID"));
		        				
			        			logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
			        			
			        			// SNMP
				        		resultMap.put(deviceOID + "." + tagMap.get("OID") + ".0", data);
		        			}
		        		}
		        	}
		        }
		        
				notifyData(resultMap, etcResultMap);*/
	        	
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	@Override
	public void setNetworkError(String val) {
		
		logger.info("Network error count : " + networkErrorCnt);
		
		// 네트워크 에러가 기준치 이상이면
		if(networkErrorCnt > networkErrorBasisCnt){
			
			Map<String, String> resultMap = new HashMap<String, String>();
			
			for(Map<String, Object> tagMap : tagList){
				
				if(!"0".equals(tagMap.get("OID"))){
				
					if( "Y".equals( ((String)tagMap.get("MONITOR_YN")).toUpperCase() ) ){
		        		
			        	// SNMP
		        		resultMap.put(deviceOID + "." + tagMap.get("OID") + ".0", "");
		        	}
				}
			}
			
			// 통신불량
			Map<String, String> etcResultMap = null;
			
			if(this.tagList.size() > 0){
				
				Map<String, Object> tagMap = this.tagList.get(0);
				
				if("0".equals((String)tagMap.get("OID"))){
					etcResultMap = new HashMap<String, String>();
					etcResultMap.put(deviceOID + ".0.0", val);
				}
			}
			
			notifyData(resultMap, etcResultMap);
			
			networkErrorCnt = 0;
		}
		
		networkErrorCnt++;
		
	}

	@Override
	public DeviceMIB getMIB() {
		
		deviceMIB = new DeviceMIB();
		
		List<MOScalar> oidList = new ArrayList<MOScalar>();
		
		for(Map<String, Object> tagMap : this.tagList){
			
			//if( "Y".equals( ((String)tagMap.get("MONITOR_YN")).toUpperCase() ) ){
				
				MOAccess moAccess = MOAccessImpl.ACCESS_READ_ONLY;
				
				if( "W".equals((String)tagMap.get("ACCESS")) ){
					moAccess = MOAccessImpl.ACCESS_WRITE_ONLY;
				}else if( "R/W".equals((String)tagMap.get("ACCESS")) ){
					moAccess = MOAccessImpl.ACCESS_READ_WRITE;
				}
				
				OctetStringOID octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + "." + (String)tagMap.get("OID") + ".0"), moAccess);
    			oidList.add(octetStringOID);
			//}
		}
		
		if(oidList.size() > 0){
			
			deviceMIB.setOIDList(oidList);
			
			addDataListener(deviceMIB);
			
			return deviceMIB;
		}else{
			return null;
		}
	}
	
	@Override
	public boolean notifySet(SetEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
}
