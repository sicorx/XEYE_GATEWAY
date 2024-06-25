package com.hoonit.xeye.scheduler;

import java.text.DecimalFormat;
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
import com.hoonit.xeye.manager.SNMPAgentProxyManager;
import com.hoonit.xeye.util.DynamicConfUtils;

public class HACPolicyJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
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
							
							DecimalFormat format = new DecimalFormat("###");
							
							if(hacTemp < 255){
								
								for(short i = 0; i < 5; i++){
									
									logger.info("Hacon ID="+(i+1));
									
									// 하콘연결상태 
									String connStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".1.0");
									
									logger.info("Hacon Connect Status="+connStatus);
									
									if("0".equals(connStatus)){
										
										String tempStr = "";
										
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
												
												if(gubun.startsWith("1")){
													tempStr = map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".2.0");
													break;
												}
											}
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
												
												if("0".equals(gubun)){
													tempStr = map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".3.0");
													break;
												}
											}
										}
										
										logger.info("Hacon Temp="+tempStr);
										
										if(!"".equals(tempStr)){
											
											int currentTemp = Integer.parseInt(format.format(Double.parseDouble(tempStr) / 100.0D));
											
											// 난방
											if(hacPolicy == 1){
												
												// 티센서(유/무선)온도가 권장온도보다 높으면 권장온도로 제어
												if(currentTemp > hacTemp){
													
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
													}
												}
											}
											// 냉방
											else if(hacPolicy == 2){
												
												// 티센서(유/무선)온도가 권장온도보다 낮으면 권장온도로 제어
												if(currentTemp < hacTemp){
													
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
}
