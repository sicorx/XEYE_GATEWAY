package com.hoonit.xeye.scheduler;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Calendar;

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
					
					logger.info("Contract Power="+contractPower);
					
					// 20000(W), 즉 20kW 이상이면
					if(contractPower >= 20000){
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
			
			// CH1 현재 전체누적사용량(Wh)
			long ch1CurrentPower = Long.parseLong(StringUtils.defaultIfEmpty(SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".10.1.0"), "0"));
			
			// 피크알람이 발생한 상황이면
			if(DynamicConfUtils.getInstance().isPowerPeak()){
				
				// 현재시간에서 피크발생시간을 뺀 시간
            	float elapsedTime = (float)((System.currentTimeMillis() - DynamicConfUtils.getInstance().getPowerPeakTime()) / 1000.0);
            	
            	logger.info("Power peak elapsed time="+elapsedTime);
            	
            	// 600초(10분) 경과 후 사용전력이 피크전력보다 작으면
            	// 냉난방이 OFF 이면 ON 한다
            	if( elapsedTime >= (600 - 10) ){
            		
            		logger.info("10Minute is passed...");
            		
            		// CH1 이전 전체누적사용량(Wh)
    				long ch1AccPower = DynamicConfUtils.getInstance().getCh1AccPower();
    				
    				logger.info("CH1 Acc Power="+ch1AccPower);
    				logger.info("CH1 Current Power="+ch1CurrentPower);
    				
    				if(ch1AccPower > 0 && ch1CurrentPower > 0){
    					
    					// 5분간 사용량(Wh)
    					long ch15MPower = ch1CurrentPower - ch1AccPower;
    					logger.info("5Min Use Wh="+ch15MPower);
    					
    					//================테스트==================
    					//BufferedReader br = new BufferedReader(new FileReader(new File("resource/conf/power_test.txt")));
    					//String temp5M = br.readLine();
    					//ch15MPower = Long.parseLong(StringUtils.defaultIfEmpty(temp5M, "0"));
    					//logger.info("Test 5Min Use Wh="+ch15MPower);
    					
    					// 계약전력의 5분간 사용량(Wh)(계약전력 / 12)
    					BigDecimal bd1 = new BigDecimal((contractPower / 1000));
    					BigDecimal bd2 = new BigDecimal("12");
    					
    					float temp = bd1.divide(bd2, 2, BigDecimal.ROUND_FLOOR).floatValue();
    					long contractPower5Min = new BigDecimal(temp * 1000).longValue();
    					logger.info("Contract Power 5Min Wh="+contractPower5Min);
    					
    					// 5분간 사용전력량이 피크전력보다 작으면
    					if(ch15MPower < contractPower5Min){
    						
		            		DynamicConfUtils.getInstance().setPowerPeak(false);
							DynamicConfUtils.getInstance().setPowerPeakTime(0L);
							
							Calendar cal = Calendar.getInstance();
							int year   = cal.get(Calendar.YEAR);
							int month  = cal.get(Calendar.MONTH) + 1;
							int date   = cal.get(Calendar.DATE);
							int hour   = cal.get(Calendar.HOUR_OF_DAY);
							int minute = cal.get(Calendar.MINUTE);
							int second = cal.get(Calendar.SECOND);
							
							// 센싱온도
							int sensingTemp = Utils.getAverageTemp();
							sensingTemp = sensingTemp / 100;
							
							for(short i = 0; i < 5; i++){
								
								// 하콘연결상태 
								String connStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".1.0");
								
								if("0".equals(connStatus)){
									
									String hacPolicy = String.valueOf(Utils.getHACPolicy(month));
									
									// 냉난방정책이 설정되어 있고, 환절기가 아니면
									if(!"255".equals(hacPolicy) && !"3".equals(hacPolicy)){
										
										String onOFF = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".7.0");
										
										// OFF 되어 있으면
										if("0".equals(onOFF)){
											
											// 제조사
											SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".4.0", DynamicConfUtils.getInstance().getHacManufacture());
											// 냉난방
											SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".5.0", hacPolicy);
											// 설정온도
											SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".6.0", String.valueOf(Utils.getHACTemp(month)));
											// ON
											SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".7.0", "1");
											// 제어
											SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".8.0", "1");
										}
									}
								}
							}
							
							// 냉난방온도제어결과 전송
							doProcessSendHAC(year, month, date, hour, minute, second, sensingTemp, (byte)0x00, (byte)0x01, (byte)0x00);
    					}
    				}
            	}
			}else{
			
				// CH1 이전 전체누적사용량(Wh)
				long ch1AccPower = DynamicConfUtils.getInstance().getCh1AccPower();
				
				logger.info("CH1 Acc Power="+ch1AccPower);
				logger.info("CH1 Current Power="+ch1CurrentPower);
				
				if(ch1AccPower > 0 && ch1CurrentPower > 0){
					
					// 5분간 사용량(Wh)
					long ch15MPower = ch1CurrentPower - ch1AccPower;
					logger.info("5Min Use Wh="+ch15MPower);
					
					//================테스트==================
					//BufferedReader br = new BufferedReader(new FileReader(new File("resource/conf/power_test.txt")));
					//String temp5M = br.readLine();
					//ch15MPower = Long.parseLong(StringUtils.defaultIfEmpty(temp5M, "0"));
					//logger.info("Test 5Min Use Wh="+ch15MPower);
					
					// 계약전력의 5분간 사용량(Wh)(계약전력 / 12)
					BigDecimal bd1 = new BigDecimal((contractPower / 1000));
					BigDecimal bd2 = new BigDecimal("12");
					
					float temp = bd1.divide(bd2, 2, BigDecimal.ROUND_FLOOR).floatValue();
					long contractPower5Min = new BigDecimal(temp * 1000).longValue();
					logger.info("Contract Power 5Min Wh="+contractPower5Min);
					
					// 5분간 사용전력량이 피크전력보다 크거나 같으면
					if(ch15MPower >= contractPower5Min){
						
						logger.info("Peak alarm is occured...");
						
						DynamicConfUtils.getInstance().setPowerPeak(true);
						DynamicConfUtils.getInstance().setPowerPeakTime(System.currentTimeMillis());
						
						Calendar cal = Calendar.getInstance();
						int year   = cal.get(Calendar.YEAR);
						int month  = cal.get(Calendar.MONTH) + 1;
						int date   = cal.get(Calendar.DATE);
						int hour   = cal.get(Calendar.HOUR_OF_DAY);
						int minute = cal.get(Calendar.MINUTE);
						int second = cal.get(Calendar.SECOND);
						
						// 센싱온도
						int sensingTemp = Utils.getAverageTemp();
						sensingTemp = sensingTemp / 100;
						
						boolean isControl = false;
						
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
								
								isControl = true;
							}
						}
						
						// 냉난방온도제어결과 전송
						doProcessSendHAC(year, month, date, hour, minute, second, sensingTemp, (byte)0x02, (byte)0x02, (byte)0x01);
						
						// 피크알람 서버전송
						doProcessSendPeak(year, month, date, hour, minute, second, ch15MPower, contractPower, isControl, sensingTemp);
					
					}else{
						DynamicConfUtils.getInstance().setPowerPeak(false);
						DynamicConfUtils.getInstance().setPowerPeakTime(0L);
					}
				}else{
					DynamicConfUtils.getInstance().setPowerPeak(false);
					DynamicConfUtils.getInstance().setPowerPeakTime(0L);
				}
			}
			
			DynamicConfUtils.getInstance().setCh1AccPower(ch1CurrentPower);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	// 피크알람전송
	private void doProcessSendPeak(int year, int month, int date, int hour, int minute, int second, 
			long ch1PeakPower, long contractPower, boolean isControl, int sensingTemp) throws Exception{
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[46];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x2A);
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
        // 피크전력
        logger.info("ch1PeakPower="+ch1PeakPower);
		byte[] peakBytes = ByteUtils.toUnsignedIntBytes(ch1PeakPower);
		writeBuffer[wIdx++] = peakBytes[0];
		writeBuffer[wIdx++] = peakBytes[1];
		writeBuffer[wIdx++] = peakBytes[2];
		writeBuffer[wIdx++] = peakBytes[3];
		// 계약전력
		logger.info("contractPower="+contractPower);
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
	private void doProcessSendHAC(int year, int month, int date, int hour, int minute, int second, int sensingTemp, 
			byte ctrlAgt, byte ctrlType, byte onOFF) throws Exception{
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[46];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x2A);
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
		// 권장온도
		writeBuffer[wIdx++] = (byte)Utils.getHACTemp(month);
		// 주체(0x00:SEMS, 0x01:Man, 0x02:Peak)
		writeBuffer[wIdx++] = ctrlAgt;
		// 제어종류(0x00:사람제어, 0x01:REMS ON, 0x02:REMS OFF, 0x03:REMS 온도 제어)
		writeBuffer[wIdx++] = ctrlType;
		// ON/OFF(0x00:ON, 0x01:OFF)
		writeBuffer[wIdx++] = onOFF;
		// 냉난방(0x00:냉방, 0x01:난방)
		writeBuffer[wIdx++] = (byte)Utils.getHACPolicy(month);
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
}
