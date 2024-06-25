package com.hoonit.xeye.device;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.OID;

import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.net.serial.GPIOSerial;
import com.hoonit.xeye.net.snmp.DeviceMIB;
import com.hoonit.xeye.net.snmp.OctetStringOID;
import com.hoonit.xeye.net.tcp.TCP;
import com.hoonit.xeye.util.ByteUtils;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;

import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.util.AtomicCounter;
import net.wimpi.modbus.util.ModbusUtil;

abstract public class ModbusBase extends DeviceBase{
	
	protected int unitID;
	
	protected List<Map<String, Object>> holdingRegisterReqGrpList;
	protected List<Map<String, Object>> inputRegisterReqGrpList;
	protected List<Map<String, Object>> inputDiscreteReqGrpList;
	protected List<Map<String, Object>> coilReqGrpList;
	
	private ByteBuffer tBuffer;
	
	protected Map<String, Object> reqTagGroup;
	
	protected Map<String, Object> reqTagMap;
	
	protected short bitTagIndex = 0;
	
	protected String recentData = "";
	
	private AtomicCounter cTransactionID = new AtomicCounter(Modbus.DEFAULT_TRANSACTION_ID);
	
	// Holding, Input 읽을 갯수
	private int A_CNT = 120;
	
	// Input, Coil 읽을 갯수
	private int D_CNT = 1600;
	
	private long splitInterval = 100;
	
	public ModbusBase(String deviceName, int protocol, int deviceType, String deviceOID, int unitID, String channel, String baudRate, List<Map<String, Object>> tagList){
		
		super(deviceName, protocol, deviceType, deviceOID, channel, baudRate, tagList);
		
		this.unitID = unitID;
		
		this.holdingRegisterReqGrpList = new ArrayList<Map<String, Object>>();
		this.inputRegisterReqGrpList = new ArrayList<Map<String, Object>>();
		this.inputDiscreteReqGrpList = new ArrayList<Map<String, Object>>();
		this.coilReqGrpList = new ArrayList<Map<String, Object>>();
		
		doSeperateTagByAddress();
	}
	
	private void doSeperateTagByAddress(){
		
		List<Map<String, Object>> holdingRegisterList = null;
		List<Map<String, Object>> inputRegisterList   = null;
		List<Map<String, Object>> inputDiscreteList   = null;
		List<Map<String, Object>> coilList   		  = null;
		
		for(Map<String, Object> tagMap : tagList){
    		
			String tagName    = (String)tagMap.get("NAME");
			String dataType   = (String)tagMap.get("DATA_TYPE");
			String tagOID     = (String)tagMap.get("OID");
			String tagAddress = (String)tagMap.get("ADDRESS");
			String tagAccess  = (String)tagMap.get("ACCESS");
			String monitorYN  = (String)tagMap.get("MONITOR_YN");
			
			if(!"".equals(tagAddress)){
				
				if("Y".equals(monitorYN.toUpperCase())){
					
					int address = Integer.parseInt(tagAddress);
					
					// Holding Register
					if ((address / 10000) == 4 || (address / 100000) == 4) {
						
						if (holdingRegisterList == null) {
							holdingRegisterList = new ArrayList<Map<String, Object>>();
						}
						
						// 쓰기만일 경우에는 추가하지 않는다.
						if(!"W".equals(tagAccess)){
							holdingRegisterList.add(tagMap);
						}
					}
					// Input Register
					else if ((address / 10000) == 3 || (address / 100000) == 3) {
						
						if (inputRegisterList == null) {
							inputRegisterList = new ArrayList<Map<String, Object>>();
						}
						
						inputRegisterList.add(tagMap);
					}
					// Input Discrete
					else if ((address / 10000) == 1 || (address / 100000) == 1) {
		
						if (inputDiscreteList == null) {
							inputDiscreteList = new ArrayList<Map<String, Object>>();
						}
						
						inputDiscreteList.add(tagMap);
					}
					// Coil
					else {
		
						if (coilList == null) {
							coilList = new ArrayList<Map<String, Object>>();
						}
		
						// 쓰기만일 경우에는 추가하지 않는다.
						if(!"W".equals(tagAccess)){
							coilList.add(tagMap);
						}
					}
				}
			}
    	} // end for
		
		
		// Holding Register
		if(holdingRegisterList != null){
			if(holdingRegisterList.size() > 0){
				
				Map<String, Object> reqGroup = null;
		    	
		    	List<Map<String, Object>> tagArr = null;
		    	
		    	int cnt = 0;
		    	int nextAddress = 0;
		    	
		    	for(Map<String, Object> tagMap : holdingRegisterList){
		    		
		    		String dataType = (String)tagMap.get("DATA_TYPE");
		    		int address     = Integer.parseInt((String)tagMap.get("ADDRESS"));
		    		
		    		if(cnt == 0){
		    			
		    			if("INT16".equals(dataType) || "UINT16".equals(dataType)){
		    				nextAddress = address + 1;
		    			}else{
		    				nextAddress = address + 2;
		    			}
		    			
		    			tagArr = new ArrayList<Map<String, Object>>();
		    			
		    			reqGroup = new HashMap<String, Object>();
		    			reqGroup.put("UNIT_ID", unitID);
		    			
		    			holdingRegisterReqGrpList.add(reqGroup);
		    			
		    		}else{
		    			
		    			if(nextAddress != address){
		    				
		    				cnt = 0;
		    				
		    				tagArr = new ArrayList<Map<String, Object>>();
			    			
			    			reqGroup = new HashMap<String, Object>();
			    			reqGroup.put("UNIT_ID", unitID);
			    			
			    			holdingRegisterReqGrpList.add(reqGroup);
		    			}
		    			
		    			if("INT16".equals(dataType) || "UINT16".equals(dataType)){
		    				nextAddress = address + 1;
		    			}else{
		    				nextAddress = address + 2;
		    			}
		    		}
		    		
		    		cnt += getWordCountByDataType(dataType);
		    		
		    		tagArr.add(tagMap);
		    		
		    		reqGroup.put("TAGS", tagArr);
		    		reqGroup.put("CNT", String.valueOf(cnt));
		    		
		    		if(cnt >= A_CNT){
		    			cnt = 0;
					}
		    	}
			}
		}
		
		// Input Register
		if(inputRegisterList != null){
			if(inputRegisterList.size() > 0){
				
				Map<String, Object> reqGroup = null;
		    	
		    	List<Map<String, Object>> tagArr = null;
		    	
		    	int cnt = 0;
		    	int nextAddress = 0;
		    	
		    	for(Map<String, Object> tagMap : inputRegisterList){
		    		
		    		String dataType = (String)tagMap.get("DATA_TYPE");
		    		int address     = Integer.parseInt((String)tagMap.get("ADDRESS"));
		    		
		    		if(cnt == 0){
		    			
		    			if("INT16".equals(dataType) || "UINT16".equals(dataType)){
		    				nextAddress = address + 1;
		    			}else{
		    				nextAddress = address + 2;
		    			}
		    			
		    			tagArr = new ArrayList<Map<String, Object>>();
		    			
		    			reqGroup = new HashMap<String, Object>();
		    			reqGroup.put("UNIT_ID", unitID);
		    			
		    			inputRegisterReqGrpList.add(reqGroup);
		    			
		    		}else{
		    			
		    			if(nextAddress != address){
		    				
		    				cnt = 0;
		    				
		    				tagArr = new ArrayList<Map<String, Object>>();
			    			
			    			reqGroup = new HashMap<String, Object>();
			    			reqGroup.put("UNIT_ID", unitID);
			    			
			    			inputRegisterReqGrpList.add(reqGroup);
		    			}
		    			
		    			if("INT16".equals(dataType) || "UINT16".equals(dataType)){
		    				nextAddress = address + 1;
		    			}else{
		    				nextAddress = address + 2;
		    			}
		    		}
		    		
		    		cnt += getWordCountByDataType(dataType);
		    		
		    		tagArr.add(tagMap);
		    		
		    		reqGroup.put("TAGS", tagArr);
		    		reqGroup.put("CNT", String.valueOf(cnt));
		    		
		    		if(cnt >= A_CNT){
		    			cnt = 0;
					}
		    	}
			}
		}
		
		// Input Discrete
		if(inputDiscreteList != null){
			if(inputDiscreteList.size() > 0){
				
				Map<String, Object> reqGroup = null;
		    	
		    	List<Map<String, Object>> tagArr = null;
		    	
		    	int cnt = 0;
		    	int nextAddress = 0;
		    	
		    	for(Map<String, Object> tagMap : inputDiscreteList){
		    		
		    		int address = Integer.parseInt((String)tagMap.get("ADDRESS"));
		    		
		    		if(cnt == 0){

		    			nextAddress = address + 1;
		    			
		    			tagArr = new ArrayList<Map<String, Object>>();
		    			
		    			reqGroup = new HashMap<String, Object>();
		    			reqGroup.put("UNIT_ID", unitID);
		    			
		    			inputDiscreteReqGrpList.add(reqGroup);
		    			
		    		}else{
		    			
		    			if(nextAddress != address){
		    				
		    				cnt = 0;
		    				
		    				tagArr = new ArrayList<Map<String, Object>>();
			    			
			    			reqGroup = new HashMap<String, Object>();
			    			reqGroup.put("UNIT_ID", unitID);
			    			
			    			inputDiscreteReqGrpList.add(reqGroup);
		    			}
		    			
		    			nextAddress = address + 1;
		    		}
		    		
		    		cnt = cnt + 1;
		    		
		    		tagArr.add(tagMap);
		    		
		    		reqGroup.put("TAGS", tagArr);
		    		reqGroup.put("CNT", String.valueOf(cnt));
		    		
		    		if(cnt >= D_CNT){
		    			cnt = 0;
					}
		    	}
			}
		}
		
		// Coil
		if(coilList != null){
			if(coilList.size() > 0){
				
				Map<String, Object> reqGroup = null;
		    	
		    	List<Map<String, Object>> tagArr = null;
		    	
		    	int cnt = 0;
		    	int nextAddress = 0;
		    	
		    	for(Map<String, Object> tagMap : coilList){
		    		
		    		int address = Integer.parseInt((String)tagMap.get("ADDRESS"));
		    		
		    		if(cnt == 0){

		    			nextAddress = address + 1;
		    			
		    			tagArr = new ArrayList<Map<String, Object>>();
		    			
		    			reqGroup = new HashMap<String, Object>();
		    			reqGroup.put("UNIT_ID", unitID);
		    			
		    			coilReqGrpList.add(reqGroup);
		    			
		    		}else{
		    			
		    			if(nextAddress != address){
		    				
		    				cnt = 0;
		    				
		    				tagArr = new ArrayList<Map<String, Object>>();
			    			
			    			reqGroup = new HashMap<String, Object>();
			    			reqGroup.put("UNIT_ID", unitID);
			    			
			    			coilReqGrpList.add(reqGroup);
		    			}
		    			
		    			nextAddress = address + 1;
		    		}
		    		
		    		cnt = cnt + 1;
		    		
		    		tagArr.add(tagMap);
		    		
		    		reqGroup.put("TAGS", tagArr);
		    		reqGroup.put("CNT", String.valueOf(cnt));
		    		
		    		if(cnt >= D_CNT){
		    			cnt = 0;
					}
		    	}
			}
		}
	}
	
	protected int getWordCountByDataType(String dataType) {

		int result = 1;

		if ("INT16".equals(dataType) || "UINT16".equals(dataType)) {
			// 2byte
			result = 1;
		} else if ("INT32".equals(dataType) 
					|| "UINT32".equals(dataType) 
					|| "FLOAT32".equals(dataType)
					|| "SW_FLOAT32".equals(dataType))
		{
			// 4byte
			result = 2;
		}

		return result;
	}
	
	/**
	 * Modbus RTU Read Frame
	 * @param address
	 * @param fc
	 * @param wordCnt
	 * @return
	 */
	protected byte[] makeModbusRTUReadFrame(int fc, int address, int wordCnt) {
    	
		char[] buffer = new char[8];

		buffer[0] = (char) unitID; /* 장비 번호 */
		buffer[1] = (char) fc;
		buffer[2] = (char) ((address & 0xFF00) >> 8); /* start address hi */
		buffer[3] = (char) (address & 0x00FF); /* start address low */
		buffer[4] = (char) ((wordCnt & 0xFF00) >> 8); /* word count hi */
		buffer[5] = (char) (wordCnt & 0x00FF); /* word count low */
		
		byte[] c = new byte[6];
		
		for (int i = 0; i < 6; i++) {
			c[i] = (byte) buffer[i];
		}
		
		int[] crc = ModbusUtil.calculateCRC(c, 0, c.length);
		
		buffer[6] = (char) crc[0];
		buffer[7] = (char) crc[1];
		
		byte[] b = new byte[buffer.length];

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < buffer.length; i++) {
			b[i] = (byte) buffer[i];
			sb.append(ByteUtils.toHexString((byte) buffer[i])).append(" ");
		}
		logger.info(sb.toString());
		
		return b;
	}
	
	/**
	 * Modbus RTU Write Frame
	 * @param fc
	 * @param address
	 * @param data
	 * @return
	 */
	protected byte[] makeModbusRTUWriteFrame(int fc, int address, int data){
		
		int index = 0;
		
		int bufferCnt = 6;
		
		// coil, holding resgister(unit16, int16) 일 경우
		if(fc == 5 || fc == 6){
			bufferCnt += 2;
		}else if(fc == 16){
			bufferCnt += 7;
		}
		
		char[] buffer = new char[bufferCnt];
		
		if(fc == 5 || fc == 6){
			buffer[index++] = (char) unitID; /* 장비 번호 */
			buffer[index++] = (char) fc;
			buffer[index++] = (char) ((address & 0xFF00) >> 8); /* start address hi */
			buffer[index++] = (char) (address & 0x00FF); /* start address low */
			buffer[index++] = (char) ((data & 0xFF00) >> 8);
			buffer[index++] = (char) (data & 0x00FF);
		}else if(fc == 16){
			buffer[index++] = (char) unitID; /* 장비 번호 */
			buffer[index++] = (char) 16;
			buffer[index++] = (char) ((address & 0xFF00) >> 8); /* start address hi */
			buffer[index++] = (char) (address & 0x00FF); /* start address low */
			buffer[index++] = (char) ((2 & 0xFF00) >> 8); /* registers hi */
			buffer[index++] = (char) (2 & 0x00FF); /* registers low */
			buffer[index++] = (char) 4; /* byte count */
			buffer[index++] = (char) ((data & 0xFF000000) >> 24);
			buffer[index++] = (char) ((data & 0xFF0000) >> 16);
			buffer[index++] = (char) ((data & 0xFF00) >> 8);
			buffer[index++] = (char) (data & 0x00FF);
		}
		
		byte[] c = new byte[index];
		
		for (int i = 0; i < index; i++) {
			c[i] = (byte) buffer[i];
		}
		
		int[] crc = ModbusUtil.calculateCRC(c, 0, index);
		
		buffer[index++] = (char) crc[0];
		buffer[index++] = (char) crc[1];

		byte[] b = new byte[buffer.length];

		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < buffer.length; i++) {
			b[i] = (byte) buffer[i];
			sb.append(ByteUtils.toHexString((byte) buffer[i])).append(" ");
		}
		
		logger.info(sb.toString());
		
		return b;
	}
	
	/**
	 * Modbus TCP Read Frame
	 * @param fc
	 * @param address
	 * @param wordCnt
	 * @return
	 */
	private byte[] makeModbusTCPReadFrame(int fc, int address, int wordCnt) {
    	
		char[] buffer = new char[12];
		
		int transactionID = cTransactionID.increment();
		
		buffer[0] = (char) ((transactionID & 0xFF00) >> 8); /* transaction id hi */
		buffer[1] = (char) (transactionID & 0x00FF);        /* transaction id low */
		buffer[2] = (char) ((0 & 0xFF00) >> 8);             /* protocol id hi */
		buffer[3] = (char) (0 & 0x00FF);                    /* protocol id low */
		buffer[4] = (char) ((6 & 0xFF00) >> 8);             /* message length hi */
		buffer[5] = (char) (6 & 0x00FF);                    /* message length low */
		buffer[6] = (char) unitID;                          /* unit id */
		buffer[7] = (char) fc;                              /* function code */
		buffer[8] = (char) ((address & 0xFF00) >> 8);       /* start address hi */
		buffer[9] = (char) (address & 0x00FF);              /* start address low */
		buffer[10] = (char) ((wordCnt & 0xFF00) >> 8);      /* word count hi */
		buffer[11] = (char) (wordCnt & 0x00FF);             /* word count low */
		
		byte[] b = new byte[buffer.length];

		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < buffer.length; i++) {
			b[i] = (byte) buffer[i];
			sb.append(ByteUtils.toHexString((byte) buffer[i])).append(" ");
		}
		logger.info(sb.toString());
		
		return b;
	}
	
	/**
	 * Modbus TCP Write Frame
	 * @param fc
	 * @param address
	 * @param data
	 * @return
	 */
	private byte[] makeModbusTCPWriteFrame(int fc, int address, int data){
		
		short index = 0;
		
		int bufferCnt = 10;
		
		// coil, holding resgister(unit16, int16) 일 경우
		if(fc == 5 || fc == 6){
			bufferCnt += 2;
		}else if(fc == 16){
			bufferCnt += 7;
		}
		
		char[] buffer = new char[bufferCnt];
		
		int transactionID = cTransactionID.increment();
		
		if(fc == 5 || fc == 6){
			buffer[index++] = (char) ((transactionID & 0xFF00) >> 8); /* transaction id hi */
			buffer[index++] = (char) (transactionID & 0x00FF);        /* transaction id low */
			buffer[index++] = (char) ((0 & 0xFF00) >> 8);             /* protocol id hi */
			buffer[index++] = (char) (0 & 0x00FF);                    /* protocol id low */
			buffer[index++] = (char) ((6 & 0xFF00) >> 8);             /* message length hi */
			buffer[index++] = (char) (6 & 0x00FF);                    /* message length low */
			buffer[index++] = (char) unitID;                          /* unit id */
			buffer[index++] = (char) fc;                              /* function code */
			buffer[index++] = (char) ((address & 0xFF00) >> 8);       /* start address hi */
			buffer[index++] = (char) (address & 0x00FF);              /* start address low */
			buffer[index++] = (char) ((data & 0xFF00) >> 8);
			buffer[index++] = (char) (data & 0x00FF);
		}else if(fc == 16){
			buffer[index++] = (char) ((transactionID & 0xFF00) >> 8); /* transaction id hi */
			buffer[index++] = (char) (transactionID & 0x00FF);        /* transaction id low */
			buffer[index++] = (char) ((0 & 0xFF00) >> 8);             /* protocol id hi */
			buffer[index++] = (char) (0 & 0x00FF);                    /* protocol id low */
			buffer[index++] = (char) ((11 & 0xFF00) >> 8);            /* message length hi */
			buffer[index++] = (char) (11 & 0x00FF);                   /* message length low */
			buffer[index++] = (char) unitID;                          /* unit id */
			buffer[index++] = (char) fc;                              /* function code */
			buffer[index++] = (char) ((address & 0xFF00) >> 8);       /* start address hi */
			buffer[index++] = (char) (address & 0x00FF);              /* start address low */
			buffer[index++] = (char) ((2 & 0xFF00) >> 8);             /* registers hi */
			buffer[index++] = (char) (2 & 0x00FF);                    /* registers low */
			buffer[index++] = (char) 4;                               /* byte count */
			buffer[index++] = (char) ((data & 0xFF000000) >> 24);
			buffer[index++] = (char) ((data & 0xFF0000) >> 16);
			buffer[index++] = (char) ((data & 0xFF00) >> 8);
			buffer[index++] = (char) (data & 0x00FF);
		}
		
		byte[] b = new byte[buffer.length];

		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < buffer.length; i++) {
			b[i] = (byte) buffer[i];
			sb.append(ByteUtils.toHexString((byte) buffer[i])).append(" ");
		}
		logger.info(sb.toString());
		
		return b;
	}
	
	protected String getData(String dataType, String rate, byte[] b){
    	
    	String data = "";
    	
    	if("INT16".equals(dataType)){
    		
    		short result = ModbusUtil.registerToShort(b);
			
			//data = (new BigDecimal(result)).multiply(new BigDecimal(rate)).toString();
    		data = String.valueOf(result);
			
    	}else if("UINT16".equals(dataType)){
    		
    		int result = ModbusUtil.registerToUnsignedShort(b);
    		
    		//data = (new BigDecimal(result)).multiply(new BigDecimal(rate)).toString();
    		data = String.valueOf(result);
    		
    	}else if("INT32".equals(dataType)){
    		
    		int result = ModbusUtil.registersToInt(b);
    		
    		//data = (new BigDecimal(result)).multiply(new BigDecimal(rate)).toString();
    		data = String.valueOf(result);
    		
    	}else if("UINT32".equals(dataType)){
    		
    		BigInteger bi = new BigInteger(b);
			long ui = bi.intValue() & 0xFFFFFFFFL;
    		
    		//data = (new BigDecimal(ui)).multiply(new BigDecimal(rate)).toString();
			data = String.valueOf(ui);
    		
    	}else if("FLOAT32".equals(dataType)){
    		
    		float result = ModbusUtil.registersToFloat(b);
    		
    		//data = (new BigDecimal(result)).multiply(new BigDecimal(rate)).toString();
    		data = String.valueOf(result);
    		
    	}else if("SW_FLOAT32".equals(dataType)){
    		
    		float result = ModbusUtil.registersToFloat(b);
    		
    		//data = (new BigDecimal(result)).multiply(new BigDecimal(rate)).toString();
    		data = String.valueOf(result);
    		
    	}
    	
    	return data;
    }
	
	abstract public Object responseMessageForWrite(byte[] buffer) throws IOException, Exception;
	
	@Override
    public void dataReceived(SerialDataEvent event) {
		
		try{
    		
			byte[] buffer = event.getBytes();
			
			tBuffer.put(buffer);
			
			logger.info("buffer remaining : " + tBuffer.remaining());
			
			if(tBuffer.remaining() == 0){
				
				tBuffer.flip();
				
				// 최근 데이터 요청일 경우
				if(isReqRecent){
					recentData = (String)responseMessageForWrite(tBuffer.array());
				}
				// 쓰기일 경우
				else if(isWrite){
					
					byte[] b = tBuffer.array();
					
					StringBuffer sb = new StringBuffer();
					
					for (int i = 0; i < b.length; i++) {
						sb.append(ByteUtils.toHexString(b[i])).append(" ");
					}
					
					logger.info(sb.toString());
				}
				else{
					
					byte[] b = tBuffer.array();
					
					StringBuffer sb = new StringBuffer();
					
					for (int i = 0; i < b.length; i++) {
						sb.append(ByteUtils.toHexString(b[i])).append(" ");
					}
					
					logger.info(sb.toString());
					
					responseMessage(b);
					
					isResponse = true;
				}
				
				synchronized(readLock) {
					readLock.notifyAll();
				}
			}
			
    	}catch(Exception e){
    		logger.error(e.getMessage(), e);
    		
    		synchronized(readLock) {
				readLock.notifyAll();
			}
    	}
	}
	
	@Override
	public byte[] requestMessage() throws IOException{
		return null;
	}
	
	@Override
	public byte[] requestMessage(String channel, String data) throws IOException{
		return null;
	}

	protected void calculateRecvByteForRead(byte[] bytes){
		
		byte fc = bytes[1];
		
		byte[] temp = new byte[2];
		System.arraycopy(bytes, 4, temp, 0, temp.length);
		
		tBuffer = ByteBuffer.allocate(temp.length);
		tBuffer.put(temp);
		tBuffer.flip();
		
		short wordCnt = tBuffer.getShort();
		
		if(fc == 3 | fc == 4){
			// modbus 응답 전체 byte 길이 = (word count * register(2byte)) + 5byte
			// modbus 응답 데이터의 길이를 파악하기 위해서
			tBuffer = ByteBuffer.allocate((wordCnt * 2) + 5);
		}else{
			
			/*
			// 8의 배수가 아니면
			if(wordCnt % 8 != 0){
				
				for(short x = 0; x < 8; x++){
					
					wordCnt++;
					
					if(wordCnt % 8 == 0){
						break;
					}
				}
			}
			
			tBuffer = ByteBuffer.allocate((wordCnt / 8) + 5);
			*/
			
			/*BigDecimal b = new BigDecimal(wordCnt);
			BigDecimal t = b.divide(new BigDecimal("8"));
			
			int tempCnt = (int)Math.round(t.doubleValue());
			
			if(tempCnt == 0)
				tempCnt = 1;*/
			
			int tempCnt = 0;
    		
    		if(wordCnt % 8 != 0){
    			
    			for(short x = 0; x < 8; x++){
					
    				wordCnt++;
					
					if(wordCnt % 8 == 0){
						tempCnt = wordCnt / 8;
						break;
					}
				}
    		}else{
    			tempCnt = wordCnt / 8;
    		}
			
			tBuffer = ByteBuffer.allocate(tempCnt + 5);
		}
	}
	
	protected void calculateRecvByteForWrite(byte[] bytes){
		
		byte fc = bytes[1];
		
		if(fc == 5 || fc == 6){
			tBuffer = ByteBuffer.allocate(8);
		}else{
			tBuffer = ByteBuffer.allocate(13);
		}
	}
	
	public void clearInputStream(DataInputStream inputStream) throws IOException {
		
		 if (inputStream.available() > 0) {
			 int len = inputStream.available();
			 byte buf[] = new byte[len];
			 inputStream.read(buf, 0, len);
			 logger.info("Clear input: " + ModbusUtil.toHex(buf, 0, len));
	    }
	}
	
	@Override
	public void execute() throws IOException{
		
		// Serial(GPIO)일 경우
		if(getTransObject() instanceof GPIOSerial){
			
			GPIOSerial transObject = (GPIOSerial)getTransObject();
			
			Serial serial = transObject.getSerial();
			serial.addListener(this);
			
			boolean isHoldingRegisterResponse = true;
			boolean isInputRegisterResponse = true;
			boolean isInputStatusResponse = true;
			boolean isCoilResponse = true;
			
			if(holdingRegisterReqGrpList.size() > 0){
				
				for(Map<String, Object> tagGroup : holdingRegisterReqGrpList){
		    		
					synchronized(readLock){
						
						try{
							
							isResponse = false;
							
							reqTagGroup = tagGroup;
							
				    		List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
				    		
				    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
				    			ref = (ref % 100000) - 1;
				    		}else{
				    			ref = (ref % 10000) - 1;
				    		}
				    		
				    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
				    		
				    		byte[] bytes = makeModbusRTUReadFrame(3, ref, cnt);
				    		
				    		logger.info("Request modbus rtu holding register...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		calculateRecvByteForRead(bytes);
				    		
				    		//transObject.changePinForSend(channel);
				    		
				    		/*for(byte b : bytes){
					    		serial.write(b);
					    		Thread.sleep(10);
					    	}*/
				    		serial.write(bytes);
				    		serial.flush();
				    		
				    		logger.info("Modbus protocol device packet sended...");
				    		
				    		//transObject.changePinForRecv(channel);
				    		
							readLock.wait(readLockWaitInterval);
							
						}catch(IOException e){
							logger.error(e.getMessage(), e);
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}finally{
							// 응답이 없으면
							if(!isResponse){
								isHoldingRegisterResponse = false;
							}
						}
					}
		    	}
			}
			
			if(inputRegisterReqGrpList.size() > 0){
				
				for(Map<String, Object> tagGroup : inputRegisterReqGrpList){
					
					synchronized(readLock){
						
						try{
							
							isResponse = false;
							
							reqTagGroup = tagGroup;
							
				    		List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
				    		
				    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
				    			ref = (ref % 100000) - 1;
				    		}else{
				    			ref = (ref % 10000) - 1;
				    		}
				    		
				    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
				    		
				    		byte[] bytes = makeModbusRTUReadFrame(4, ref, cnt);
				    		
				    		logger.info("Request modbus rtu input register...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		calculateRecvByteForRead(bytes);
				    		
				    		//transObject.changePinForSend(channel);
				    		
				    		/*for(byte b : bytes){
					    		serial.write(b);
					    		Thread.sleep(10);
					    	}*/
				    		serial.write(bytes);
				    		serial.flush();
				    		
				    		//transObject.changePinForRecv(channel);
				    		
							readLock.wait(readLockWaitInterval);

						}catch(IOException e){
							logger.error(e.getMessage(), e);
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}finally{
							// 응답이 없으면
							if(!isResponse){
								isInputRegisterResponse = false;
							}
						}
					}
		    	}
			}
			
			if(inputDiscreteReqGrpList.size() > 0){
				
				for(Map<String, Object> tagGroup : inputDiscreteReqGrpList){
				
					synchronized(readLock){
						
						try{
							
							isResponse = false;
							
							reqTagGroup = tagGroup;
							
				    		List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
							
				    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
				    			ref = (ref % 100000) - 1;
				    		}else{
				    			ref = (ref % 10000) - 1;
				    		}
							
				    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
				    		
				    		byte[] bytes = makeModbusRTUReadFrame(2, ref, cnt);
				    		
				    		logger.info("Request modbus rtu input status...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		calculateRecvByteForRead(bytes);
				    		
				    		//transObject.changePinForSend(channel);
				    		
				    		/*for(byte b : bytes){
					    		serial.write(b);
					    		Thread.sleep(10);
					    	}*/
				    		serial.write(bytes);
				    		serial.flush();
				    		
				    		//transObject.changePinForRecv(channel);
				    		
							readLock.wait(readLockWaitInterval);
							
						}catch(IOException e){
							logger.error(e.getMessage(), e);
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}finally{
							// 응답이 없으면
							if(!isResponse){
								isInputStatusResponse = false;
							}
						}
					}
				}
			}
			
			if(coilReqGrpList.size() > 0){
				
				for(Map<String, Object> tagGroup : coilReqGrpList){
					
					synchronized(readLock){
						
						try{
							
							isResponse = false;
							
							reqTagGroup = tagGroup;
							
				    		List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
				    		ref = ref - 1;
							
				    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
				    		
				    		byte[] bytes = makeModbusRTUReadFrame(1, ref, cnt);
				    		
				    		logger.info("Request modbus rtu coils...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		calculateRecvByteForRead(bytes);
				    		
				    		//transObject.changePinForSend(channel);
				    		
				    		/*for(byte b : bytes){
					    		serial.write(b);
					    		Thread.sleep(10);
					    	}*/
				    		serial.write(bytes);
				    		serial.flush();
				    		
				    		//transObject.changePinForRecv(channel);
				    		
							readLock.wait(readLockWaitInterval);
	
						}catch(IOException e){
							logger.error(e.getMessage(), e);
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}finally{
							// 응답이 없으면
							if(!isResponse){
								isCoilResponse = false;
							}
						}
					}
				}
			}
			
			serial.removeListener(this);
			
			// 응답이 없는 요청이 하나라도 존재하면
			if(!isHoldingRegisterResponse || !isInputRegisterResponse || !isInputStatusResponse || !isCoilResponse){
				// 통신불량
				setNetworkError(NetworkError.ABNORMAL);
				throw new IOException();
			}else{
				networkErrorCnt = 0;
			}
		}
		// TCP일 경우
		else if(getTransObject() instanceof TCP){
			
			TCP transObject = (TCP)getTransObject();
			
			DataInputStream inputStream   = transObject.getInputStream();
			DataOutputStream outputStream = transObject.getOutputStream();
			
			boolean isHoldingRegisterResponse = true;
			boolean isInputRegisterResponse = true;
			boolean isInputStatusResponse = true;
			boolean isCoilResponse = true;
			
			if(holdingRegisterReqGrpList.size() > 0){
				
				try{
					
					int errCnt = 0;
					
					for(Map<String, Object> tagGroup : holdingRegisterReqGrpList){
			    		
						try{
							
							reqTagGroup = tagGroup;
							
				    		List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
				    		
				    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
				    			ref = (ref % 100000) - 1;
				    		}else{
				    			ref = (ref % 10000) - 1;
				    		}
				    		
				    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
				    		// modbus 응답 전체 byte 길이 = (word count * register(2byte)) + 9byte
							int bufferSize = (cnt * 2 )+ 9;
				    		
				    		byte[] bytes = makeModbusTCPReadFrame(3, ref, cnt);
				    		
				    		logger.info("Request modbus rtu holding register...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		outputStream.write(bytes, 0, bytes.length);
				    		outputStream.flush();
				    		
				    		logger.info("Modbus protocol device packet sended...");
				    		
				    		byte[] buffer = new byte[bufferSize];
				    		
				    		inputStream.read(buffer, 0, buffer.length);
				    		
				    		responseMessage(buffer);
				    		
				    		if(splitInterval > 0)
				    			Thread.sleep(splitInterval);
				    		
						}catch(SocketTimeoutException e){
							logger.error(e.getMessage(), e);
							errCnt++;
						}
			    	}
					
					if(errCnt == holdingRegisterReqGrpList.size()){
						isHoldingRegisterResponse = false;
					}
				
				}catch(SocketException e){
					logger.error(e.getMessage(), e);
					throw e;
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					isHoldingRegisterResponse = false;
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			if(inputRegisterReqGrpList.size() > 0){
				
				try{
					
					int errCnt = 0;
					
					for(Map<String, Object> tagGroup : inputRegisterReqGrpList){
						
						try{
							
							reqTagGroup = tagGroup;
							
				    		List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
				    		
				    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
				    			ref = (ref % 100000) - 1;
				    		}else{
				    			ref = (ref % 10000) - 1;
				    		}
				    		
				    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
				    		// modbus 응답 전체 byte 길이 = (word count * register(2byte)) + 9byte
							int bufferSize = (cnt * 2 )+ 9;
				    		
				    		byte[] bytes = makeModbusTCPReadFrame(4, ref, cnt);
				    		
				    		logger.info("Request modbus rtu input register...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		outputStream.write(bytes, 0, bytes.length);
				    		outputStream.flush();
				    		
				    		logger.info("Modbus protocol device packet sended...");
				    		
				    		byte[] buffer = new byte[bufferSize];
				    		
				    		inputStream.read(buffer, 0, buffer.length);
				    		
				    		responseMessage(buffer);
				    		
				    		if(splitInterval > 0)
				    			Thread.sleep(splitInterval);
				    		
						}catch(SocketTimeoutException e){
							logger.error(e.getMessage(), e);
							errCnt++;
						}
			    	}
					
					if(errCnt == inputRegisterReqGrpList.size()){
						isInputRegisterResponse = false;
					}
					
				}catch(SocketException e){
					logger.error(e.getMessage(), e);
					throw e;
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					isInputRegisterResponse = false;
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			if(inputDiscreteReqGrpList.size() > 0){
				
				try{
					
					int errCnt = 0;
					
					for(Map<String, Object> tagGroup : inputDiscreteReqGrpList){

						try{
							
							reqTagGroup = tagGroup;
							
							List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
							
				    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
				    			ref = (ref % 100000) - 1;
				    		}else{
				    			ref = (ref % 10000) - 1;
				    		}
							
				    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
							
				    		int wordCnt = cnt;
				    		
				    		int tempCnt = 0;
				    		
				    		if(wordCnt % 8 != 0){
				    			
				    			for(short x = 0; x < 8; x++){
									
				    				wordCnt++;
									
									if(wordCnt % 8 == 0){
										tempCnt = wordCnt / 8;
										break;
									}
								}
				    		}else{
				    			tempCnt = wordCnt / 8;
				    		}
							
							int bufferSize = tempCnt + 9;
							
				    		byte[] bytes = makeModbusTCPReadFrame(2, ref, cnt);
				    		
				    		logger.info("Request modbus rtu input status...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		outputStream.write(bytes, 0, bytes.length);
				    		outputStream.flush();
				    		
				    		logger.info("Modbus protocol device packet sended...");
				    		
				    		byte[] buffer = new byte[bufferSize];
				    		
				    		inputStream.read(buffer, 0, buffer.length);
				    		
				    		responseMessage(buffer);
				    		
				    		if(splitInterval > 0)
				    			Thread.sleep(splitInterval);
				    		
						}catch(SocketTimeoutException e){
							logger.error(e.getMessage(), e);
							errCnt++;
						}
					}
					
					if(errCnt == inputDiscreteReqGrpList.size()){
						isInputStatusResponse = false;
					}
					
				}catch(SocketException e){
					logger.error(e.getMessage(), e);
					throw e;
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					isInputStatusResponse = false;
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			if(coilReqGrpList.size() > 0){
				
				try{
					
					int errCnt = 0;
					
					for(Map<String, Object> tagGroup : coilReqGrpList){
						
						try{
						
							reqTagGroup = tagGroup;
							
							List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
							ref = ref - 1;
							
							int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
							
							int wordCnt = cnt;
				    		
				    		int tempCnt = 0;
				    		
				    		if(wordCnt % 8 != 0){
				    			
				    			for(short x = 0; x < 8; x++){
									
				    				wordCnt++;
									
									if(wordCnt % 8 == 0){
										tempCnt = wordCnt / 8;
										break;
									}
								}
				    		}else{
				    			tempCnt = wordCnt / 8;
				    		}
							
							int bufferSize = tempCnt + 9;
				    		
				    		byte[] bytes = makeModbusTCPReadFrame(1, ref, cnt);
				    		
				    		logger.info("Request modbus rtu coils...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		outputStream.write(bytes, 0, bytes.length);
				    		outputStream.flush();
				    		
				    		logger.info("Modbus protocol device packet sended...");
				    		
				    		byte[] buffer = new byte[bufferSize];
				    		
				    		inputStream.read(buffer, 0, buffer.length);
				    		
				    		responseMessage(buffer);
				    		
				    		if(splitInterval > 0)
				    			Thread.sleep(splitInterval);
				    		
						}catch(SocketTimeoutException e){
							logger.error(e.getMessage(), e);
							errCnt++;
						}
					}
					
					if(errCnt == coilReqGrpList.size()){
						isCoilResponse = false;
					}
					
				}catch(SocketException e){
					logger.error(e.getMessage(), e);
					throw e;
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					isCoilResponse = false;
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			// 응답이 없는 요청이 하나라도 존재하면
			if(!isHoldingRegisterResponse || !isInputRegisterResponse || !isInputStatusResponse || !isCoilResponse){
				// 통신불량
				setNetworkError(NetworkError.ABNORMAL);
				throw new IOException();
			}else{
				networkErrorCnt = 0;
			}
		}
		// Serial(RS232, RS485)일 경우
		else if(getTransObject() instanceof com.hoonit.xeye.net.serial.Serial){
			
			com.hoonit.xeye.net.serial.Serial transObject = (com.hoonit.xeye.net.serial.Serial)getTransObject();
			
			SerialPort serialPort         = transObject.getSerialPort();
			DataInputStream inputStream   = transObject.getInputStream();
			DataOutputStream outputStream = transObject.getOutputStream();
			
			boolean isHoldingRegisterResponse = true;
			boolean isInputRegisterResponse = true;
			boolean isInputStatusResponse = true;
			boolean isCoilResponse = true;
			
			if(holdingRegisterReqGrpList.size() > 0){
				
				try{
					
					for(Map<String, Object> tagGroup : holdingRegisterReqGrpList){
		    		
						reqTagGroup = tagGroup;
						
			    		List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
			    		
			    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
			    		
			    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
			    			ref = (ref % 100000) - 1;
			    		}else{
			    			ref = (ref % 10000) - 1;
			    		}
			    		
			    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
			    		// modbus 응답 전체 byte 길이 = (word count * register(2byte)) + 5byte
						int bufferSize = (cnt * 2 )+ 5;
			    		
			    		byte[] bytes = makeModbusRTUReadFrame(3, ref, cnt);
			    		
			    		logger.info("Request modbus rtu holding register...");
			    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
			    		
			    		clearInputStream(inputStream);
			    		
			    		outputStream.write(bytes, 0, bytes.length);
			    		outputStream.flush();
			    		
			    		try {
							serialPort.enableReceiveThreshold(bufferSize);
						} catch (UnsupportedCommOperationException e) {
							logger.error(e.getMessage(), e);
						}
			    		
			    		logger.info("Modbus protocol device packet sended...");
			    		
			    		byte[] buffer = new byte[bufferSize];
			    		
			    		inputStream.read(buffer, 0, buffer.length);
			    		
			    		serialPort.disableReceiveThreshold();
			    		
			    		responseMessage(buffer);
					}
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					serialPort.disableReceiveThreshold();
					isHoldingRegisterResponse = false;
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			if(inputRegisterReqGrpList.size() > 0){
				
				try{
					
					for(Map<String, Object> tagGroup : inputRegisterReqGrpList){
						
						reqTagGroup = tagGroup;
						
			    		List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
			    		
			    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
			    		
			    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
			    			ref = (ref % 100000) - 1;
			    		}else{
			    			ref = (ref % 10000) - 1;
			    		}
			    		
			    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
			    		// modbus 응답 전체 byte 길이 = (word count * register(2byte)) + 5byte
						int bufferSize = (cnt * 2 )+ 5;
			    		
			    		byte[] bytes = makeModbusRTUReadFrame(4, ref, cnt);
			    		
			    		logger.info("Request modbus rtu input register...");
			    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
			    		
			    		clearInputStream(inputStream);
			    		
			    		outputStream.write(bytes, 0, bytes.length);
			    		outputStream.flush();
			    		
			    		try {
							serialPort.enableReceiveThreshold(bufferSize); 
						} catch (UnsupportedCommOperationException e) {
							logger.error(e.getMessage(), e);
						}
			    		
			    		logger.info("Modbus protocol device packet sended...");
			    		
			    		byte[] buffer = new byte[bufferSize];
			    		
			    		inputStream.read(buffer, 0, buffer.length);
			    		
			    		serialPort.disableReceiveThreshold();
			    		
			    		responseMessage(buffer);
		    		}
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					serialPort.disableReceiveThreshold();
					isInputRegisterResponse = false;
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			if(inputDiscreteReqGrpList.size() > 0){
				
				try{
					
					for(Map<String, Object> tagGroup : inputDiscreteReqGrpList){
						
							reqTagGroup = tagGroup;
							
							List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
				    		
				    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
							
				    		if( ((String)((Map<String, Object>)tags.get(0)).get("ADDRESS")).length() > 5 ){
				    			ref = (ref % 100000) - 1;
				    		}else{
				    			ref = (ref % 10000) - 1;
				    		}
							
				    		int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
							
				    		int wordCnt = cnt;
				    		
				    		int tempCnt = 0;
				    		
				    		if(wordCnt % 8 != 0){
				    			
				    			for(short x = 0; x < 8; x++){
									
				    				wordCnt++;
									
									if(wordCnt % 8 == 0){
										tempCnt = wordCnt / 8;
										break;
									}
								}
				    		}else{
				    			tempCnt = wordCnt / 8;
				    		}
							
							int bufferSize = tempCnt + 5;
				    		
				    		byte[] bytes = makeModbusRTUReadFrame(2, ref, cnt);
				    		
				    		logger.info("Request modbus rtu input status...");
				    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
				    		
				    		clearInputStream(inputStream);
				    		
				    		outputStream.write(bytes, 0, bytes.length);
				    		outputStream.flush();
				    		
				    		try {
								serialPort.enableReceiveThreshold(bufferSize); 
							} catch (UnsupportedCommOperationException e) {
								logger.error(e.getMessage(), e);
							}
				    		
				    		logger.info("Modbus protocol device packet sended...");
				    		
				    		byte[] buffer = new byte[bufferSize];
				    		
				    		inputStream.read(buffer, 0, buffer.length);
				    		
				    		serialPort.disableReceiveThreshold();
				    		
				    		responseMessage(buffer);
					}
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					serialPort.disableReceiveThreshold();
					isInputStatusResponse = false;
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			if(coilReqGrpList.size() > 0){
				
				try{
					
					for(Map<String, Object> tagGroup : coilReqGrpList){
						
						reqTagGroup = tagGroup;
						
						List<Map<String, Object>> tags = (List<Map<String, Object>>)tagGroup.get("TAGS");
			    		
			    		int ref = Integer.parseInt((String)((Map<String, Object>)tags.get(0)).get("ADDRESS"));
						ref = ref - 1;
						
						int cnt = Integer.parseInt((String)tagGroup.get("CNT"));
						
						int wordCnt = cnt;
			    		
			    		int tempCnt = 0;
			    		
			    		if(wordCnt % 8 != 0){
			    			
			    			for(short x = 0; x < 8; x++){
								
			    				wordCnt++;
								
								if(wordCnt % 8 == 0){
									tempCnt = wordCnt / 8;
									break;
								}
							}
			    		}else{
			    			tempCnt = wordCnt / 8;
			    		}
						
						int bufferSize = tempCnt + 5;
			    		
			    		byte[] bytes = makeModbusRTUReadFrame(1, ref, cnt);
			    		
			    		logger.info("Request modbus rtu coils...");
			    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
			    		
			    		clearInputStream(inputStream);
			    		
			    		outputStream.write(bytes, 0, bytes.length);
			    		outputStream.flush();
			    		
			    		try {
							serialPort.enableReceiveThreshold(bufferSize); 
						} catch (UnsupportedCommOperationException e) {
							logger.error(e.getMessage(), e);
						}
			    		
			    		logger.info("Modbus protocol device packet sended...");
			    		
			    		byte[] buffer = new byte[bufferSize];
			    		
			    		inputStream.read(buffer, 0, buffer.length);
			    		
			    		serialPort.disableReceiveThreshold();
			    		
			    		responseMessage(buffer);
					}
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					serialPort.disableReceiveThreshold();
					isCoilResponse = false;
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			// 응답이 없는 요청이 하나라도 존재하면
			if(!isHoldingRegisterResponse || !isInputRegisterResponse || !isInputStatusResponse || !isCoilResponse){
				// 통신불량
				setNetworkError(NetworkError.ABNORMAL);
				throw new IOException();
			}else{
				networkErrorCnt = 0;
			}
		}
	}
	
	@Override
	public void setNetworkError(String val){
		
		logger.info("Network Error (" + (networkErrorCnt+1) + "/" +networkErrorBasisCnt + ")");
		
		// 네트워크 에러가 기준치 이상이면
		if(networkErrorCnt >= networkErrorBasisCnt){
			
			Map<String, String> resultMap = new HashMap<String, String>();
			
			for(Map<String, Object> reqGroup : holdingRegisterReqGrpList){
				
				List<Map<String, Object>> tagArr = (List<Map<String, Object>>)reqGroup.get("TAGS");
				
				for(Map<String, Object> tagMap : tagArr){
					
					if(tagMap.get("BIT_TAG") == null){
						
						String oid = (String)tagMap.get("OID");
						
						resultMap.put(deviceOID + "." + oid + ".0", "");
					}else{
						
						List<Map<String, Object>> bitTagList = (List<Map<String, Object>>)tagMap.get("BIT_TAG");
						
						for(Map<String, Object> bitTagMap : bitTagList){
							
							String oid = (String)bitTagMap.get("OID");
							
							resultMap.put(deviceOID + "." + oid + ".0", "");
						}
					}
				}
			}
			
			for(Map<String, Object> reqGroup : inputRegisterReqGrpList){
				
				List<Map<String, Object>> tagArr = (List<Map<String, Object>>)reqGroup.get("TAGS");
				
				for(Map<String, Object> tagMap : tagArr){
					
					if(tagMap.get("BIT_TAG") == null){
						
						String oid = (String)tagMap.get("OID");
						
						resultMap.put(deviceOID + "." + oid + ".0", "");
				        
					}else{
						
						List<Map<String, Object>> bitTagList = (List<Map<String, Object>>)tagMap.get("BIT_TAG");
						
						for(Map<String, Object> bitTagMap : bitTagList){
							
							String oid = (String)bitTagMap.get("OID");
							
							resultMap.put(deviceOID + "." + oid + ".0", "");
						}
					}
				}
			}
			
			for(Map<String, Object> reqGroup : inputDiscreteReqGrpList){
				
				List<Map<String, Object>> tagArr = (List<Map<String, Object>>)reqGroup.get("TAGS");
				
				for(Map<String, Object> tagMap : tagArr){
					
					String oid = (String)tagMap.get("OID");
					
					resultMap.put(deviceOID + "." + oid + ".0", "");
				}
			}
			
			for(Map<String, Object> reqGroup : coilReqGrpList){
				
				List<Map<String, Object>> tagArr = (List<Map<String, Object>>)reqGroup.get("TAGS");
				
				for(Map<String, Object> tagMap : tagArr){
					
					String oid = (String)tagMap.get("OID");
					
					resultMap.put(deviceOID + "." + oid + ".0", "");
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
		}else{
			networkErrorCnt++;
		}
	}
	
	@Override
	public DeviceMIB getMIB(){
		
		deviceMIB = new DeviceMIB();
		
		List<MOScalar> oidList = new ArrayList<MOScalar>();
		
		MOAccess moAccess;
		
		OctetStringOID octetStringOID;
		
		for(Map<String, Object> tagMap : this.tagList){
			
			//if( "Y".equals( ((String)tagMap.get("MONITOR_YN")).toUpperCase() ) ){
				
				if(tagMap.get("BIT_TAG") == null){
					
					moAccess = MOAccessImpl.ACCESS_READ_ONLY;
					
					if( "W".equals((String)tagMap.get("ACCESS")) ){
						moAccess = MOAccessImpl.ACCESS_WRITE_ONLY;
					}else if( "R/W".equals((String)tagMap.get("ACCESS")) ){
						moAccess = MOAccessImpl.ACCESS_READ_WRITE;
					}
					
	    			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + "." + (String)tagMap.get("OID") + ".0"), moAccess);
	    			oidList.add(octetStringOID);
	    			
				}else{
					
					List<Map<String, Object>> bitTagList = (List<Map<String, Object>>)tagMap.get("BIT_TAG");
					
					for(Map<String, Object> bitTagMap : bitTagList){
						
						moAccess = MOAccessImpl.ACCESS_READ_ONLY;
						
						if( "W".equals((String)bitTagMap.get("ACCESS")) ){
							moAccess = MOAccessImpl.ACCESS_WRITE_ONLY;
						}else if( "R/W".equals((String)bitTagMap.get("ACCESS")) ){
							moAccess = MOAccessImpl.ACCESS_READ_WRITE;
						}
						
		    			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + "." + (String)bitTagMap.get("OID") + ".0"), moAccess);
		    			oidList.add(octetStringOID);
					}
				}
			//}
		}
		
		//  PMC일 경우
		if("PMC".equals(this.deviceName)){
			
			// Ch1 R/S/T 누적사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".1.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch1 R/S/T 순간사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".2.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch1 R/S/T 전류 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".3.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch2 R/S/T 누적사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".4.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch2 R/S/T 순간사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".5.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch2 R/S/T 전류 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".6.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch3 R/S/T 누적사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".7.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch3 R/S/T 순간사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".8.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch3 R/S/T 전류 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".9.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch4 R/S/T 누적사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".10.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch4 R/S/T 순간사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".11.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch4 R/S/T 전류 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".12.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch5 R/S/T 누적사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".13.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch5 R/S/T 순간사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".14.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch5 R/S/T 전류 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".15.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch6 R/S/T 누적사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".16.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch6 R/S/T 순간사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".17.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch6 R/S/T 전류 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".18.0"), moAccess);
			oidList.add(octetStringOID);
			
			/*
			// Ch7 R/S/T 누적사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".122.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch7 R/S/T 순간사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".123.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch8 R/S/T 누적사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".124.0"), moAccess);
			oidList.add(octetStringOID);
			
			// Ch8 R/S/T 순간사용량 OID
			moAccess = MOAccessImpl.ACCESS_READ_ONLY;
			octetStringOID = new OctetStringOID(deviceMIB, new OID(deviceOID + ".125.0"), moAccess);
			oidList.add(octetStringOID);
			*/
		}
		
		if(oidList.size() > 0){
			
			deviceMIB.setOIDList(oidList);
			
			addDataListener(deviceMIB);
			
			return deviceMIB;
		}else{
			return null;
		}
	}
	
	/**
	 * Modbus 데이터 부분의 byte를 반환한다.
	 * @param buffer
	 * @return
	 */
	protected byte[] getDataByte(byte[] buffer){
		
		logger.info("response length : " + buffer.length);
		
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < buffer.length; i++) {
			sb.append(ByteUtils.toHexString((byte) buffer[i])).append(" ");
		}
		
		logger.info(sb.toString());
		
		byte[] result = null;
		
		// Serial이면
		if( (getTransObject() instanceof GPIOSerial) || (getTransObject() instanceof com.hoonit.xeye.net.serial.Serial)){
			
			//check CRC
			int dlength = buffer.length - 2;
	        int[] crc = ModbusUtil.calculateCRC(buffer, 0, dlength); //does not include CRC
	        if (ModbusUtil.unsignedByteToInt(buffer[dlength]) != crc[0]
	            || ModbusUtil.unsignedByteToInt(buffer[dlength + 1]) != crc[1]) {
	        	
	        	logger.info("CRC Error in received frame: " + dlength + " bytes");
	        	
	        }else{
			
				int index = 0;
				
				byte responseUnitID = buffer[index++]; // unit id
				byte responseFC = buffer[index++];     // command
				byte byteCnt = buffer[index++];        // byte count
				
				int byteIntCnt = 0;
				byteIntCnt = byteIntCnt << 8 | (byteCnt & 0xFF);
				
				logger.info("UNIT ID      : " + responseUnitID);
				logger.info("Function CD  : " + responseFC);
				logger.info("Byte CNT     : " + byteIntCnt);
				
				if(responseUnitID == unitID){
					
					if(byteIntCnt > 0){
					
						result = new byte[byteIntCnt];
						
						for (int i = 0; i < byteIntCnt; i++) {
							result[i] = buffer[index++];
						}
					}
				}
	        }
		}
		// TCP이면
		else{
			
			short index = 0;
			
			ByteBuffer bb = ByteBuffer.allocate(2);
			
			byte transantionIDHi = buffer[index++]; /* transantion id hi */
			byte transantionIDLo = buffer[index++]; /* transantion id low */
			
			bb.clear();
			bb.put(transantionIDHi);
			bb.put(transantionIDLo);
			bb.flip();
			short transactionID = bb.getShort();
			
			byte protocolIDHi = buffer[index++]; /* protocol id hi */
			byte protocolIDLo = buffer[index++]; /* protocol id low */
			
			bb.clear();
			bb.put(protocolIDHi);
			bb.put(protocolIDLo);
			bb.flip();
			short protocolID = bb.getShort();
			
			byte msgLenHi = buffer[index++]; /* message id hi */
			byte msgLenLo = buffer[index++]; /* message id hi */
			
			bb.clear();
			bb.put(msgLenHi);
			bb.put(msgLenLo);
			bb.flip();
			short msgLen = bb.getShort();
			
			byte responseUnitID = buffer[index++]; // unit id
			byte responseFC = buffer[index++];     // command
			byte byteCnt = buffer[index++];        // byte count
			
			int byteIntCnt = 0;
			byteIntCnt = byteIntCnt << 8 | (byteCnt & 0xFF);
			
			logger.info("Transaction ID : " + transactionID);
			logger.info("Protocol ID    : " + protocolID);
			logger.info("Message Length : " + msgLen);
			logger.info("UNIT ID        : " + responseUnitID);
			logger.info("FUNCTION CD    : " + responseFC);
			logger.info("Byte Cnt       : " + byteIntCnt);
			
			if(responseUnitID == unitID){
				
				if(byteIntCnt > 0){
					
					result = new byte[byteIntCnt];
					
					for (int i = 0; i < byteIntCnt; i++) {
						result[i] = buffer[index++];
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Modbus 응답 결과의 Function Code를 반환한다.
	 * @param buffer
	 * @return
	 */
	protected byte getFunctionCode(byte[] buffer){
		
		// Serial이면
		if((getTransObject() instanceof GPIOSerial) || (getTransObject() instanceof com.hoonit.xeye.net.serial.Serial)){
			return buffer[1];
		}else{
			return buffer[7];
		}
	}
	
	/**
	 * 쓰기 요청 태그 정보를 반환한다.
	 * @param oid
	 * @return
	 */
	protected Map<String, Object> getReqTagMap(String oid){
		
		oid = oid.substring(0, oid.lastIndexOf("."));
    	String cTagOID = oid.substring(oid.lastIndexOf(".")+1);
    	
    	oid = oid.substring(0, oid.lastIndexOf("."));
		String cDeviceOID = oid.substring(oid.lastIndexOf(".")+1);
		
		Map<String, Object> resultTagMap = null;
		bitTagIndex = 0;
		
		for(int i = 0; i < getTagList().size(); i++){
			
			Map<String, Object> tagMap = getTagList().get(i);
			
			if(tagMap.get("BIT_TAG") == null){
				
				if(deviceOID.substring(deviceOID.lastIndexOf(".")+1).equals(cDeviceOID)){
					
					if(tagMap.get("OID").equals(cTagOID)){
						resultTagMap = tagMap;
						break;
					}
				}
			}else{
				
				List<Map<String, Object>> bitTagList = (List<Map<String, Object>>)tagMap.get("BIT_TAG");
				
				short idx = 0;
				
				for(Map<String, Object> bitTagMap : bitTagList){
					
					if(deviceOID.substring(deviceOID.lastIndexOf(".")+1).equals(cDeviceOID)){
						
						if(bitTagMap.get("OID").equals(cTagOID)){
							
							tagMap = new HashMap<String, Object>();
							tagMap.put("ADDRESS", bitTagMap.get("PARENT_ADDRESS"));
							tagMap.put("DATA_TYPE", bitTagMap.get("PARENT_DATA_TYPE"));
							tagMap.put("ACCESS", bitTagMap.get("ACCESS"));
							tagMap.put("OID", bitTagMap.get("OID"));
							tagMap.put("NAME", bitTagMap.get("NAME"));
							tagMap.put("MONITOR_YN", bitTagMap.get("MONITOR_YN"));
							tagMap.put("BIT_TAG", bitTagList);
							
							resultTagMap = tagMap;
							bitTagIndex = idx++;
							break;
						}
					}
				}
			}
		}
		
		return resultTagMap;
	}
	
	/**
	 * 최신 데이터를 가져온다.
	 * @return
	 */
	protected String getRecentData(Map<String, Object> tagMap){
		
		String result = "";
		
		int ref = Integer.parseInt((String)tagMap.get("ADDRESS"));
    	
    	int fc = 0;
    	
    	if ((ref / 10000) == 4 || (ref / 100000) == 4) {
			fc = 3;
		}else if ((ref / 10000) == 3 || (ref / 100000) == 3) {
			fc = 4;
		}else if ((ref / 10000) == 1 || (ref / 100000) == 1){
			fc = 2;
		}else{
			fc = 1;
		}
    	
    	if(((String)tagMap.get("ADDRESS")).length() > 5){
    		ref = (ref % 100000) - 1;
    	}else{
    		ref = (ref % 10000) - 1;
    	}
    	
    	String dataType = (String)tagMap.get("DATA_TYPE");
		int cnt = getWordCountByDataType(dataType);
		String access = (String)tagMap.get("ACCESS");
		
		logger.info("Channel : " + channel);
		logger.info("Ref     : " + ref);
		logger.info("Cnt     : " + cnt);
		logger.info("Access  : " + access);
		
		if("R/W".equals(access)){
			
			// Serial(GPIO)일 경우
			if(getTransObject() instanceof GPIOSerial){
				
				GPIOSerial transObject = (GPIOSerial)getTransObject();
				
				Serial serial = transObject.getSerial();
				serial.addListener(this);
				
				// 제어하는 항목의 최근 데이터를 가져온다.
				// 3번까지 데이터 가져오기 시도
				for(short i = 0; i < 3; i++){
					
					synchronized(readLock){
						
						try{
							
							isReqRecent = true;
							
							byte[] bytes = makeModbusRTUReadFrame(fc, ref, cnt);
							
							calculateRecvByteForRead(bytes);
							
							//transObject.changePinForSend(channel);
				    		
				    		/*for(byte b : bytes){
					    		serial.write(b);
					    		Thread.sleep(10);
					    	}*/
							serial.write(bytes);
							serial.flush();
				    		
				    		//transObject.changePinForRecv(channel);
				    		
							readLock.wait(readLockWaitInterval);
							
						}catch(IOException e){
							logger.error(e.getMessage(), e);
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}finally{
							isReqRecent = false;
							
							if(serial != null) serial.removeListener(this);
							
							result = StringUtils.defaultString(recentData, "");
							
							if(!"".equals(result)){
								break;
							}
						}
					}
				}
			}
			// TCP일 경우
			else if(getTransObject() instanceof TCP){
				
				TCP transObject = (TCP)getTransObject();
				
				try{
					
					DataInputStream inputStream   = transObject.getInputStream();
					DataOutputStream outputStream = transObject.getOutputStream();
					
					int bufferSize = 0;
					
					if(fc == 3 || fc == 4){
						// modbus 응답 전체 byte 길이 = (word count * register(2byte)) + 9byte
						bufferSize = (cnt * 2 )+ 9;
					}else{
						
						int wordCnt = cnt;

						int tempCnt = 0;

						if(wordCnt % 8 != 0){
							
							for(short x = 0; x < 8; x++){
								
								wordCnt++;
								
								if(wordCnt % 8 == 0){
									tempCnt = wordCnt / 8;
									break;
								}
							}
						}else{
							tempCnt = wordCnt / 8;
						}

						bufferSize = tempCnt + 9;
					}
		    		
		    		byte[] bytes = makeModbusTCPReadFrame(fc, ref, cnt);
		    		
		    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
		    		
		    		outputStream.write(bytes, 0, bytes.length);
		    		outputStream.flush();
		    		
		    		byte[] buffer = new byte[bufferSize];
		    		
		    		inputStream.read(buffer, 0, buffer.length);
		    		
		    		result = (String)responseMessageForWrite(buffer);
		    		
				}catch(IOException e){
					logger.error(e.getMessage(), e);
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			// Serial(RS232, RS485)일 경우
			else if(getTransObject() instanceof com.hoonit.xeye.net.serial.Serial){
				
				com.hoonit.xeye.net.serial.Serial transObject = (com.hoonit.xeye.net.serial.Serial)getTransObject();
				
				SerialPort serialPort = null;
				
				try{
					
					serialPort = transObject.getSerialPort();
					DataInputStream inputStream   = transObject.getInputStream();
					DataOutputStream outputStream = transObject.getOutputStream();
					
					int bufferSize = 0;
					
					if(fc == 3 || fc == 4){
						// modbus 응답 전체 byte 길이 = (word count * register(2byte)) + 5byte
						bufferSize = (cnt * 2 )+ 5;
					}else{
						
						int wordCnt = cnt;

						int tempCnt = 0;

						if(wordCnt % 8 != 0){
							
							for(short x = 0; x < 8; x++){
								
								wordCnt++;
								
								if(wordCnt % 8 == 0){
									tempCnt = wordCnt / 8;
									break;
								}
							}
						}else{
							tempCnt = wordCnt / 8;
						}

						bufferSize = tempCnt + 5;
					}
		    		
		    		byte[] bytes = makeModbusRTUReadFrame(fc, ref, cnt);
		    		
		    		logger.info("UnitID=" + unitID + ", Ref=" + ref + ", Cnt=" + cnt);
		    		
		    		clearInputStream(inputStream);
		    		
		    		outputStream.write(bytes, 0, bytes.length);
		    		outputStream.flush();
		    		
		    		try {
						serialPort.enableReceiveThreshold(bytes.length);
					} catch (UnsupportedCommOperationException e) {
						logger.error(e.getMessage(), e);
					}
		    		
		    		byte[] buffer = new byte[bufferSize];
		    		
		    		inputStream.read(buffer, 0, buffer.length);
		    		
		    		serialPort.disableReceiveThreshold();
		    		
		    		result = (String)responseMessageForWrite(buffer);
		    		
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					serialPort.disableReceiveThreshold();
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Modbus write
	 * @param reqData clinet 요청 데이터
	 * @param currentData 현재 데이터
	 * @param writeData 쓰기 데이터
	 * @return
	 */
	protected boolean doWriteData(String reqData, String currentData, String writeData, String address, String dataType){
		
		boolean result = false;
		
		logger.info("Write data=" + reqData + ", Current data=" + currentData);
		
		// 최근 데이터와 다르면
		if(!reqData.equals(currentData)){
			
			// Serial(GPIO) 이면
			if(getTransObject() instanceof GPIOSerial){
				
				GPIOSerial transObject = (GPIOSerial)getTransObject();
				
				Serial serial = transObject.getSerial();
				serial.addListener(this);
			
				synchronized(readLock){
					
					try{
						
						isWrite = true;
						
						int realAddr = 0;
						
						if(address.length() > 5){
							realAddr = Integer.parseInt(address) % 100000;
						}else{
							realAddr = Integer.parseInt(address) % 10000;
						}
						
						int fc = 0;
						
						byte[] bytes = null;
						
						if((Integer.parseInt(address) / 10000) == 4 || (Integer.parseInt(address) / 100000) == 4){
							
							int sendData = 0;
							
							if("FLOAT32".equals(dataType) || "SW_FLOAT32".equals(dataType)){
								
								fc = 16;
								
								sendData = Float.floatToIntBits(Float.parseFloat(writeData));
								
							}else{
								
								if("UINT16".equals(dataType) || "INT16".equals(dataType)){
									fc = 6;
								}else if("UINT32".equals(dataType) || "INT32".equals(dataType)){
									fc = 16;
								}
								
								if("UINT16".equals(dataType) || "INT16".equals(dataType) || "INT32".equals(dataType)){
									sendData = Integer.parseInt(writeData);
								}
								// UINT32
								else{
									
									BigInteger bi = new BigInteger(writeData);
									
									byte[] b = new byte[4];
									
									ByteBuffer buffer = ByteBuffer.wrap(b);
									buffer.putInt(bi.intValue());
									buffer.flip();
									
									sendData = buffer.getInt();
								}
							}
							
							bytes = makeModbusRTUWriteFrame(fc, (realAddr-1), sendData);
							
						}else {
							
							fc = 5;
							
							int sendData = Integer.parseInt(writeData);
							
							if(sendData != 0)
								sendData = 65280;
							
							bytes = makeModbusRTUWriteFrame(fc, (realAddr-1), sendData);
						}
						
						calculateRecvByteForWrite(bytes);
						
						//transObject.changePinForSend(channel);
			    		
			    		/*for(byte b : bytes){
				    		serial.write(b);
				    		Thread.sleep(10);
				    	}*/
						serial.write(bytes);
						serial.flush();
			    		
			    		//transObject.changePinForRecv(channel);
			    		
						readLock.wait(readLockWaitInterval);
						
						result = true;
						
					}catch(IOException e){
						logger.error(e.getMessage(), e);
					}catch(Exception e){
						logger.error(e.getMessage(), e);
					}finally{
						if(serial != null) serial.removeListener(this);
						isWrite = false;
					}
				}
			}
			// TCP 이면
			else if(getTransObject() instanceof TCP){
				
				TCP transObject = (TCP)getTransObject();
				
				try{
					
					DataInputStream inputStream   = transObject.getInputStream();
					DataOutputStream outputStream = transObject.getOutputStream();
					
					//int realAddr = Integer.parseInt(address) % 10000;
					int realAddr = 0;
					
					if(address.length() > 5){
						realAddr = Integer.parseInt(address) % 100000;
					}else{
						realAddr = Integer.parseInt(address) % 10000;
					}
					
					int fc = 0;
					
					byte[] bytes = null;
					
					if((Integer.parseInt(address)/10000) == 4 || (Integer.parseInt(address) / 100000) == 4){
						
						int sendData = 0;
						
						if("FLOAT32".equals(dataType) || "SW_FLOAT32".equals(dataType)){
							
							fc = 16;
							
							sendData = Float.floatToIntBits(Float.parseFloat(writeData));
							
						}else{
							
							if("UINT16".equals(dataType) || "INT16".equals(dataType)){
								fc = 6;
							}else if("UINT32".equals(dataType) || "INT32".equals(dataType)){
								fc = 16;
							}
							
							if("UINT16".equals(dataType) || "INT16".equals(dataType) || "INT32".equals(dataType)){
								sendData = Integer.parseInt(writeData);
							}
							// UINT32
							else{
								
								BigInteger bi = new BigInteger(writeData);
								
								byte[] b = new byte[4];
								
								ByteBuffer buffer = ByteBuffer.wrap(b);
								buffer.putInt(bi.intValue());
								buffer.flip();
								
								sendData = buffer.getInt();
							}
						}
						
						bytes = makeModbusTCPWriteFrame(fc, (realAddr-1), sendData);
						
					}else {
						
						fc = 5;
						
						int sendData = Integer.parseInt(writeData);
						
						if(sendData != 0)
							sendData = 65280;
						
						bytes = makeModbusTCPWriteFrame(fc, (realAddr-1), sendData);
					}
		    		
		    		outputStream.write(bytes, 0, bytes.length);
		    		outputStream.flush();
		    		
		    		byte[] buffer = new byte[bytes.length];
		    		
		    		inputStream.read(buffer, 0, bytes.length);
					
					StringBuffer sb = new StringBuffer();
					
					for (int i = 0; i < buffer.length; i++) {
						sb.append(ByteUtils.toHexString(buffer[i])).append(" ");
					}
					
					logger.info(sb.toString());
					
					result = true;
		    		
				}catch(IOException e){
					logger.error(e.getMessage(), e);
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			// Serial(RS232, RS485) 이면
			else if(getTransObject() instanceof com.hoonit.xeye.net.serial.Serial){
				
				com.hoonit.xeye.net.serial.Serial transObject = (com.hoonit.xeye.net.serial.Serial)getTransObject();
				
				SerialPort serialPort = null;
				
				try{
					
					serialPort = transObject.getSerialPort();
					DataInputStream inputStream   = transObject.getInputStream();
					DataOutputStream outputStream = transObject.getOutputStream();
					
					//int realAddr = Integer.parseInt(address) % 10000;
					int realAddr = 0;
					
					if(address.length() > 5){
						realAddr = Integer.parseInt(address) % 100000;
					}else{
						realAddr = Integer.parseInt(address) % 10000;
					}
					
					int fc = 0;
					
					byte[] bytes = null;
					
					if((Integer.parseInt(address)/10000) == 4 || (Integer.parseInt(address) / 100000) == 4){
						
						int sendData = 0;
						
						if("FLOAT32".equals(dataType) || "SW_FLOAT32".equals(dataType)){
							
							fc = 16;
							
							sendData = Float.floatToIntBits(Float.parseFloat(writeData));
							
						}else{
							
							if("UINT16".equals(dataType) || "INT16".equals(dataType)){
								fc = 6;
							}else if("UINT32".equals(dataType) || "INT32".equals(dataType)){
								fc = 16;
							}
							
							if("UINT16".equals(dataType) || "INT16".equals(dataType) || "INT32".equals(dataType)){
								sendData = Integer.parseInt(writeData);
							}
							// UINT32
							else{
								
								BigInteger bi = new BigInteger(writeData);
								
								byte[] b = new byte[4];
								
								ByteBuffer buffer = ByteBuffer.wrap(b);
								buffer.putInt(bi.intValue());
								buffer.flip();
								
								sendData = buffer.getInt();
							}
						}
						
						bytes = makeModbusRTUWriteFrame(fc, (realAddr-1), sendData);
						
					}else {
						
						fc = 5;
						
						int sendData = Integer.parseInt(writeData);
						
						if(sendData != 0)
							sendData = 65280;
						
						bytes = makeModbusRTUWriteFrame(fc, (realAddr-1), sendData);
					}
					
					clearInputStream(inputStream);
		    		
		    		outputStream.write(bytes, 0, bytes.length);
		    		outputStream.flush();
		    		
		    		try {
						serialPort.enableReceiveThreshold(bytes.length);
					} catch (UnsupportedCommOperationException e) {
						logger.error(e.getMessage(), e);
					}
		    		
		    		byte[] buffer = new byte[bytes.length];
		    		
		    		inputStream.read(buffer, 0, bytes.length);
		    		
		    		serialPort.disableReceiveThreshold();
					
					StringBuffer sb = new StringBuffer();
					
					for (int i = 0; i < buffer.length; i++) {
						sb.append(ByteUtils.toHexString(buffer[i])).append(" ");
					}
					
					logger.info(sb.toString());
					
					result = true;
		    		
				}catch(IOException e){
					logger.error(e.getMessage(), e);
					serialPort.disableReceiveThreshold();
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
		}else{
			logger.info("The write data is same with recent data...");
		}
		
		return result;
	}
}
