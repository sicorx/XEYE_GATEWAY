package com.hoonit.xeye.scheduler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.IFClientProxyManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;
import com.hoonit.xeye.net.socket.IFClient;
import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.DynamicConfUtils;
import com.hoonit.xeye.util.Utils;

public class PowerPeakPolicyJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private final byte STX              = 0x02;
	private final byte ETX              = 0x03;
	
	private final byte NORMAL           = 0x01; // 정상
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		logger.info("Power Peak Policy job is executing...");
		
		if(!"".equals(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getStoreCD(), ""))){
			
			try{
				
				String dataLoadComplete = SNMPAgentProxyManager.getInstance().getStaticOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".1.99.0");
				String commStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".10.0.0");
			
				// 데이터가 전부 로딩되고 통신상태가 정상이면
				if("1".equals(dataLoadComplete) && "0".equals(commStatus)){
					
					// 계약전력(W)
					long contractPower = Long.parseLong(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getContractPower(), "0"));
					
					if(contractPower > 0){
						doProcess(contractPower);
					}
				}
				
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
		
		logger.info("Power Peak Policy job is executed...");
	}
	
	private void doProcess(long contractPower){
		
		try{
			
			List<String> list = new ArrayList<String>();
			list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.55.0"); // CH1 R 전력
			list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.61.0"); // CH1 S 전력
			list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.67.0"); // CH1 T 전력
			
			Map<String, String> map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
			
			long ch1RPower = Long.parseLong(map.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.55.0"));
			long ch1SPower = Long.parseLong(map.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.61.0"));
			long ch1TPower = Long.parseLong(map.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.67.0"));
			
			long ch1PowerSum = ch1RPower + ch1SPower + ch1TPower; // CH1 전력 합
			
			long rate = 0L;
			
			if(ch1PowerSum > 0){
				rate = Math.round(((double)ch1PowerSum / (double)contractPower) * 100.0D);
			}
			
			// 순시전력(W)값이 계약전력(W)값의 70%까지 도달하면
			if(rate >= 70){
				
				// 피크가 발생하지 않았으면
				if(!DynamicConfUtils.getInstance().isPowerPeak()){
				
					DynamicConfUtils.getInstance().setPowerPeak(true);
					
					Calendar cal = Calendar.getInstance();
					int year   = cal.get(Calendar.YEAR);
					int month  = cal.get(Calendar.MONTH) + 1;
					int date   = cal.get(Calendar.DATE);
					int hour   = cal.get(Calendar.HOUR_OF_DAY);
					int minute = cal.get(Calendar.MINUTE);
					int second = cal.get(Calendar.SECOND);
					
					// 센싱온도
					int sensingTemp = getAverageTemp();
					
					boolean isControl = false;
					
					// 초과비율이 80%까지 도달하면 냉난방기를 제어한다.
					if(rate >= 80){
						
						for(short i = 0; i < 5; i++){
							
							// 하콘연결상태 
							String connStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".1.0");
							
							if("0".equals(connStatus)){
								
								// 제조사
								SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".4.0", DynamicConfUtils.getInstance().getHacManufacture());
								// OFF
								SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".7.0", "0");
								// 제어
								SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".8.0", "1");
							}
						}
						
						// 냉난방온도제어결과 전송
						doProcessSendHAC(year, month, date, hour, minute, second, sensingTemp);
						
						isControl = true;
					}
					
					// 피크알람 서버전송
					doProcessSendPeak(year, month, date, hour, minute, second, ch1PowerSum, contractPower, isControl, sensingTemp);
				}
			}else{
				DynamicConfUtils.getInstance().setPowerPeak(false);
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	// 피크알람전송
	private void doProcessSendPeak(int year, int month, int date, int hour, int minute, int second, 
			long ch1PowerSum, long contractPower, boolean isControl, int sensingTemp) throws Exception{
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[40];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x24);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = 0x0C;
		
		// 매장코드
		short storeCDLen = 20;
		String storeCD = DynamicConfUtils.getInstance().getStoreCD();
		String gwID    = DynamicConfUtils.getInstance().getGwID();
		
        try{
        	
    		String strCD = StringUtils.defaultIfEmpty(storeCD, "");
    		byte[] storeBytes = Utils.getFillNullByte(strCD.getBytes(), storeCDLen);
    		
    		for(int i = 0; i < storeBytes.length; i++){
    			writeBuffer[wIdx++] = storeBytes[i];
    		}
        }catch(ArrayIndexOutOfBoundsException e){
        	logger.error(e.getMessage(), e);
        	
        	for(int i = 0; i < storeCDLen; i++){
    			writeBuffer[wIdx++] = 0x20;
    		}
        }
        // GW ID
        byte[] gwIDs = ByteUtils.toUnsignedShortBytes(Integer.parseInt(StringUtils.defaultIfEmpty(gwID, "0")));
        writeBuffer[wIdx++] = gwIDs[0];
        writeBuffer[wIdx++] = gwIDs[1];
        
		// 년
        byte[] yearBytes = ByteUtils.toUnsignedShortBytes(year);
        writeBuffer[wIdx++] = yearBytes[0];
        writeBuffer[wIdx++] = yearBytes[1];
        // 월
        writeBuffer[wIdx++] = (byte)month;
        // 일
        writeBuffer[wIdx++] = (byte)date;
        // 시
        writeBuffer[wIdx++] = (byte)hour;
        // 분
        writeBuffer[wIdx++] = (byte)minute;
        // 초
        writeBuffer[wIdx++] = (byte)second;
        // 피크값
		byte[] peakBytes = ByteUtils.toUnsignedIntBytes(ch1PowerSum);
		writeBuffer[wIdx++] = peakBytes[0];
		writeBuffer[wIdx++] = peakBytes[1];
		writeBuffer[wIdx++] = peakBytes[2];
		writeBuffer[wIdx++] = peakBytes[3];
		// 계약전력
		byte[] contractBytes = ByteUtils.toUnsignedIntBytes(contractPower);
		writeBuffer[wIdx++] = contractBytes[0];
		writeBuffer[wIdx++] = contractBytes[1];
		writeBuffer[wIdx++] = contractBytes[2];
		writeBuffer[wIdx++] = contractBytes[3];
		// 제어여부(0x00:제어, 0x01:미제어)
		if(isControl)
			writeBuffer[wIdx++] = 0x00;
		else
			writeBuffer[wIdx++] = 0x01;
		// 센싱온도
		writeBuffer[wIdx++] = (byte)sensingTemp;
		
		// CRC
		byte[] crc = Utils.getBytes(writeBuffer, 3, writeBuffer.length-6);
		
		short sCRC = CRC16.getInstance().getCRC(crc);
		
		ByteBuffer wBuffer = ByteBuffer.allocate(2);
		wBuffer.putShort(sCRC);
		wBuffer.flip();
		
		byte crc1 = wBuffer.get();
		byte crc2 = wBuffer.get();
		
		writeBuffer[wIdx++] = crc1;
		writeBuffer[wIdx++] = crc2;
		writeBuffer[wIdx++] = ETX;
		
		IFClient client = IFClientProxyManager.getInstance().getIFClient();
		
		if(client != null){
			client.write(writeBuffer);
		}
	}
	
	// 냉난방온도제어결과 전송
	private void doProcessSendHAC(int year, int month, int date, int hour, int minute, int second, int sensingTemp) throws Exception{
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[45];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x29);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = 0x0A;
		writeBuffer[wIdx++] = NORMAL;
		
		// 매장코드
		short storeCDLen = 20;
		String storeCD = DynamicConfUtils.getInstance().getStoreCD();
		String gwID    = DynamicConfUtils.getInstance().getGwID();
		
        try{
        	
    		String strCD = StringUtils.defaultIfEmpty(storeCD, "");
    		byte[] storeBytes = Utils.getFillNullByte(strCD.getBytes(), storeCDLen);
    		
    		for(int i = 0; i < storeBytes.length; i++){
    			writeBuffer[wIdx++] = storeBytes[i];
    		}
        }catch(ArrayIndexOutOfBoundsException e){
        	logger.error(e.getMessage(), e);
        	
        	for(int i = 0; i < storeCDLen; i++){
    			writeBuffer[wIdx++] = 0x20;
    		}
        }
        // GW ID
        byte[] gwIDs = ByteUtils.toUnsignedShortBytes(Integer.parseInt(StringUtils.defaultIfEmpty(gwID, "0")));
        writeBuffer[wIdx++] = gwIDs[0];
        writeBuffer[wIdx++] = gwIDs[1];
		
		// 년
        byte[] yearBytes = ByteUtils.toUnsignedShortBytes(year);
        writeBuffer[wIdx++] = yearBytes[0];
        writeBuffer[wIdx++] = yearBytes[1];
        // 월
        writeBuffer[wIdx++] = (byte)month;
        // 일
        writeBuffer[wIdx++] = (byte)date;
        // 시
        writeBuffer[wIdx++] = (byte)hour;
        // 분
        writeBuffer[wIdx++] = (byte)minute;
        // 초
        writeBuffer[wIdx++] = (byte)second;
        // 하콘ID(0x00:전체, 0x01:하콘1…0x05:하콘5)
     	writeBuffer[wIdx++] = 0x00; // 전체
        // 센싱온도
		writeBuffer[wIdx++] = (byte)sensingTemp;
		// 제어온도
		writeBuffer[wIdx++] = 0x00;
		// 주체(0x00:SEMS, 0x01:Man, 0x02:Peak)
		writeBuffer[wIdx++] = 0x00;
		// 제어종류(0:사람제어, 1:REMS ON, 2:REMS OFF,  3:REMS 온도 제어)
		writeBuffer[wIdx++] = 0x02;
		// ON/OFF(0x00:ON, 0x01:OFF)
		writeBuffer[wIdx++] = 0x01;
		// 냉난방(0x00:냉방, 0x01:난방)
		writeBuffer[wIdx++] = (byte)getHACPolicy(month);
		// 제어주체(0x00:SEMS, 0x01:Mobile)
		writeBuffer[wIdx++] = 0x00;
		
		// CRC
		byte[] crc = Utils.getBytes(writeBuffer, 3, writeBuffer.length-6);
		
		short sCRC = CRC16.getInstance().getCRC(crc);
		
		ByteBuffer wBuffer = ByteBuffer.allocate(2);
		wBuffer.putShort(sCRC);
		wBuffer.flip();
		
		byte crc1 = wBuffer.get();
		byte crc2 = wBuffer.get();
		
		writeBuffer[wIdx++] = crc1;
		writeBuffer[wIdx++] = crc2;
		writeBuffer[wIdx++] = ETX;
		
		IFClient client = IFClientProxyManager.getInstance().getIFClient();
		
		if(client != null){
			client.write(writeBuffer);
		}
	}
	
	// 티센서 평균온도
	private int getAverageTemp(){
		
		int temp = 0, deviceCnt = 0;
		
		try{
			
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
					String gubun = map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".1.0");
					
					// Major가 1 이면 상온에 있는 센서
					if(gubun.startsWith("1")){
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
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		return temp;
	}
	
	// 냉난방정책
	private int getHACPolicy(int month){
		
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
}
