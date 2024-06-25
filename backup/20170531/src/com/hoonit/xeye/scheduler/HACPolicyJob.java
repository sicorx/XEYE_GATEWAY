package com.hoonit.xeye.scheduler;

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

public class HACPolicyJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private final byte STX              = 0x02;
	private final byte ETX              = 0x03;
	
	private final byte NORMAL           = 0x01; // 정상
	private final byte ERR_CTRL         = 0x06;
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		logger.info("HAC Policy job is executing...");
		
		if(!"".equals(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getStoreCD(), ""))){
			
			// 전력피크치가 아니면
			if(!DynamicConfUtils.getInstance().isPowerPeak()){
			
				// 하콘 제어중이 아니면
				if(!DynamicConfUtils.getInstance().isHaconControl()){
				
					DynamicConfUtils.getInstance().setHaconControl(true);
					
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
								int temp = Utils.getAverageTemp();
								temp = temp / 100;
								
								logger.info("TSensor Average Temp="+temp);
						
								Calendar nowCal = Calendar.getInstance();
								int month = nowCal.get(Calendar.MONTH) + 1;
								
								logger.info("month="+month);
								
								// 냉난방정책
								int hacPolicy = Utils.getHACPolicy(month);
								
								logger.info("HAC Policy="+hacPolicy);
								
								if(hacPolicy < 255){
									
									// 권장온도
									int hacTemp = Utils.getHACTemp(month);
									
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
						
					}catch(Exception e){
						logger.info(e.getMessage(), e);
					}finally{
						DynamicConfUtils.getInstance().setHaconControl(false);
					}
				}
			}else{
				logger.info("Job is not execute because of peak alarm.");
			}
		}
		
		logger.info("HAC Policy job is executed...");
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
		writeBuffer[wIdx++] = (byte)Utils.getHACTemp(month);
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
