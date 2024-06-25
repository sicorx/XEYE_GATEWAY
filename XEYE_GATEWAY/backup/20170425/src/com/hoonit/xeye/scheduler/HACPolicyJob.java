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

public class HACPolicyJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private final byte STX              = 0x02;
	private final byte ETX              = 0x03;
	
	private final byte NORMAL           = 0x01; // 정상
	private final byte ERR_CTRL         = 0x06;
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		if(!"".equals(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getStoreCD(), ""))){
			
			// 전력피크치가 아니면
			if(!DynamicConfUtils.getInstance().isPowerPeak()){
			
				// 하콘 제어중이 아니면
				if(!DynamicConfUtils.getInstance().isHaconControl()){
				
					DynamicConfUtils.getInstance().setHaconControl(true);
					
					logger.info("HAC Policy job is executing...");
					
					try{
						
						String dataLoadComplete = SNMPAgentProxyManager.getInstance().getStaticOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".1.99.0");
						
						logger.info("Data Load Complete="+dataLoadComplete);
						
						// 데이터로딩이 끝났으면
						if("1".equals(dataLoadComplete)){
							
							String commStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".10.0.0");
							
							logger.info("Comm Status="+commStatus);
							
							// 통신상태가 정상이면
							if("0".equals(commStatus)){
								
								// 티센서 평균온도
								int temp = getAverageTemp();
								temp = temp / 100;
								
								logger.info("TSensor Average Temp="+temp);
						
								Calendar nowCal = Calendar.getInstance();
								int month = nowCal.get(Calendar.MONTH) + 1;
								
								logger.info("month="+month);
								
								// 냉난방정책
								int hacPolicy = getHACPolicy(month);
								
								logger.info("HAC Policy="+hacPolicy);
								
								if(hacPolicy < 255){
									
									// 권장온도
									int hacTemp = getHACTemp(month);
									
									logger.info("HAC Recommand Temp="+hacTemp);
									
									//DecimalFormat format = new DecimalFormat("###");
									
									if(hacTemp < 255){
										
										// 하콘 5개에 대해서
										for(short i = 0; i < 5; i++){
											
											logger.info("Hacon ID="+(i+1));
											
											// 하콘연결상태 
											String connStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".1.0");
											
											logger.info("Hacon Connect Status="+connStatus);
											
											if("0".equals(connStatus)){
												
												if(temp > 0){
													
													//int currentTemp = Integer.parseInt(format.format(Double.parseDouble(temp) / 100.0D));
													//int currentTemp = temp / 100;
													
													// 난방
													if(hacPolicy == 1){
														
														// 티센서(유/무선)온도가 권장온도보다 높으면 권장온도로 제어
														if(temp > hacTemp){
															
															int manufacture = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHacManufacture(), "255"));
															
															logger.info("Manufacture="+manufacture);
															
															if(manufacture < 255){
																
																logger.info("Heat Control Start...");
																
																// 제조사
																SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".4.0", String.valueOf(manufacture));
																// 난방
																SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".5.0", "0");
																// 설정온도
																SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".6.0", String.valueOf(hacTemp));
																// ON
																SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".7.0", "1");
																// 제어
																int res = SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".8.0", "1");
																
																doProcessSend(res, (byte)0x00, (byte)temp, (byte)hacTemp, (byte)0x01);
															}
														}
													}
													// 냉방
													else if(hacPolicy == 2){
														
														// 티센서(유/무선)온도가 권장온도보다 낮으면 권장온도로 제어
														if(temp < hacTemp){
															
															int manufacture = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHacManufacture(), "255"));
															
															logger.info("Manufacture="+manufacture);
															
															if(manufacture < 255){
																
																logger.info("Cool Control Start...");
																
																// 제조사
																SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".4.0", String.valueOf(manufacture));
																// 냉방
																SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".5.0", "1");
																// 설정온도
																SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".6.0", String.valueOf(hacTemp));
																// ON
																SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".7.0", "1");
																// 제어
																int res = SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".8.0", "1");
																
																doProcessSend(res, (byte)0x00, (byte)temp, (byte)hacTemp, (byte)0x00);
															}
														}
													}
													// 환절기
													else{
														
														// 냉난방기 OFF 한다
														/*int manufacture = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHacManufacture(), "255"));
														
														logger.info("Manufacture="+manufacture);
														
														if(manufacture < 255){
															
															logger.info("Change Season Control Start...");
															
															// 제조사
															SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".4.0", String.valueOf(manufacture));
															// OFF
															SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".7.0", "0");
															// 제어
															int res = SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".8.0", "1");
														}*/
													}
												}
											}
										}
									}
								}
							}
						}
						
						logger.info("HAC Policy job is executed...");
						
					}catch(Exception e){
						logger.info(e.getMessage(), e);
					}finally{
						DynamicConfUtils.getInstance().setHaconControl(false);
					}
				}
			}else{
				logger.info("Peak alarm is occured...");
			}
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
	
	// 권장온도
	private int getHACTemp(int month){
		
		int result;
		
		switch(month){
			case 1 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac1MTemp(), "255"));
				break;
			case 2 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac2MTemp(), "255"));
				break;
			case 3 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac3MTemp(), "255"));
				break;
			case 4 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac4MTemp(), "255"));
				break;
			case 5 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac5MTemp(), "255"));
				break;
			case 6 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac6MTemp(), "255"));
				break;
			case 7 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac7MTemp(), "255"));
				break;
			case 8 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac8MTemp(), "255"));
				break;
			case 9 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac9MTemp(), "255"));
				break;
			case 10 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac10MTemp(), "255"));
				break;
			case 11 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac11MTemp(), "255"));
				break;
			case 12 : 
				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac12MTemp(), "255"));
				break;
			default : 
				result = 255;
				break;
		}
		
		return result;
	}
	
	// 서버에 온도제어 전송
	private void doProcessSend(int res, byte haconID, byte sensingTemp, byte controlTemp, byte coolNheat) throws Exception{
		
		Calendar cal = Calendar.getInstance();
		int year   = cal.get(Calendar.YEAR);
		int month  = cal.get(Calendar.MONTH) + 1;
		int date   = cal.get(Calendar.DATE);
		int hour   = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[46];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x2A);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = 0x0A;
		
		if(res == 0){
			writeBuffer[wIdx++] = NORMAL;
		}else{
			writeBuffer[wIdx++] = ERR_CTRL;
		}
		
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
     	writeBuffer[wIdx++] = haconID; // 전체
        // 센싱온도
		writeBuffer[wIdx++] = sensingTemp;
		// 제어온도
		writeBuffer[wIdx++] = controlTemp;
		// 권장온도
		writeBuffer[wIdx++] = (byte)getHACTemp(month);
		// 주체(0x00:SEMS, 0x01:Man, 0x02:Peak)
		writeBuffer[wIdx++] = 0x00;
		// 제어종류(0:사람제어, 1:REMS ON, 2:REMS OFF,  3:REMS 온도 제어)
		writeBuffer[wIdx++] = 0x03;
		// ON/OFF(0x00:ON, 0x01:OFF)
		writeBuffer[wIdx++] = 0x00;
		// 냉난방(0x00:냉방, 0x01:난방)
		writeBuffer[wIdx++] = coolNheat;
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
