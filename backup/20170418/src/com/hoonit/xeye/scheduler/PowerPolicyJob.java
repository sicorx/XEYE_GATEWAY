package com.hoonit.xeye.scheduler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

public class PowerPolicyJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		logger.info("Power Policy job is executing...");
		
		try{
			
			String dataLoadComplete = SNMPAgentProxyManager.getInstance().getStaticOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".1.99.0");
			String commStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".10.0.0");
		
			// 데이터가 전부 로딩되고 통신상태가 정상이면
			if("1".equals(dataLoadComplete) && "0".equals(commStatus)){
				
				// 계약전력(W)
				long contractPower = Long.parseLong(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getContractPower(), "0"));
				
				if(contractPower > 0){
					
					// 20,000W(20kW) 미안이면
					if(contractPower < 20.0D){
						doProcessUnder20(contractPower);
					}
					// 20,000W(20kW) 이상이면
					else{
						doProcessOver20(contractPower);
					}
				}
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		logger.info("Power Policy job is executing...");
	}
	
	// 20kW 이상
	private void doProcessOver20(long contractPower){
		
		BufferedReader br = null;
		BufferedWriter bw = null;
		
		try{
			String pmcCH1OIDVal = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".10.1.0");
			long pmcCH1AccuPower = Long.parseLong(StringUtils.defaultIfEmpty(pmcCH1OIDVal, "0")); // Wh
			
			if(pmcCH1AccuPower > 0){
				
				File file = new File("resource/conf/accpower.txt");
				
				if(!file.exists())
					file.createNewFile();
				
				br = new BufferedReader(new FileReader(file));
				
				// 파일에서 읽어온 누적전력량
				String readAccuPower = br.readLine();
				br.close();
				
				long accuPower = Long.parseLong(StringUtils.defaultIfEmpty(readAccuPower, "0")); // Wh
				
				// 기존에 쌓인 전력량이 존재하면
				if(accuPower > 0){
					
					// PMC 누적량이 파일 누적량보다 작으면
					// UINT32의 최대값을 벗어났을 수 있다.
					// 그러므로 R/S/T 의 UINT32 최대값(4,294,967,295)을 더한 후 계산한다.
					/*if(pmcCH1AccuPower < accuPower){
						
						List<String> list = new ArrayList<String>();
						list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.37.0"); // CH1 R 누적전력량
						list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.43.0"); // CH1 S 누적전력량
						list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.49.0"); // CH1 T 누적전력량
						
						list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.73.0"); // CH1 R 전류
						list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.79.0"); // CH1 S 전류
						list.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.85.0"); // CH1 T 전류
						
						accuPower = ()
					}*/
					
					// PMC 누적량이 파일 누적량보다 크면
					if(pmcCH1AccuPower > accuPower){
						
						// 1분간 사용전력량
						long usePower1Min = pmcCH1AccuPower - accuPower; // Wh
						
						Calendar cal = Calendar.getInstance();
						
						int minute = cal.get(Calendar.MINUTE);
						int second = cal.get(Calendar.SECOND);
						
						// 매시간 15, 30, 45, 정각일 경우
						if( (minute == 15 && (second > 0 || second < 10)) &&
								(minute == 30 && (second > 0 || second < 10)) &&
								(minute == 45 && (second > 0 || second < 10)) &&
								(minute == 0 && (second > 0 || second < 10)) ){
							
							// 1분간 사용전력량을 저장
							long sum = accuPower + usePower1Min;
							
							bw = new BufferedWriter(new FileWriter(file));
							bw.write(String.valueOf(sum));
							bw.flush();
							
							DynamicConfUtils.getInstance().setPowerPeakConsecutiveTime(0L); // 발생연속시간 초기화
							DynamicConfUtils.getInstance().setPowerPeak(false); // 전력피크 발생
							
						}else{
							
							// 계약전력을 4로 나누면 15분간 최대수요전력량
							long usePower15 = (contractPower / 4);
							// 1분간 사용전력량(kW)을 60으로 나누면 W
							long usePower1MinW = (usePower1Min / 60);
							
							double ratePower = (usePower1MinW / usePower15) * 100.0D;
							
							long cTime = cal.getTimeInMillis();
							long pTime = DynamicConfUtils.getInstance().getPowerPeakConsecutiveTime();
							
							// 경과시간
							long period = (cTime - pTime) / 1000; // 초
							
							// 사용전력량이 계약전력량의 80%이상이고
							// 10분이상 발생하면 피크알람
							if(ratePower > 80.0D){
								
								DynamicConfUtils.getInstance().setPowerPeak(true); // 전력피크 발생
								
								if(period >= (1000 * 60 * 10)){
									
									// 이전에 피크치가 발생한 상태이면
									if(DynamicConfUtils.getInstance().isPowerPeak()){
									
										// 피크알람 전송
									}
								}
							}
						}
					}
				}
				// 기존에 쌓인 전력량이 존재하지 않으면 파일에 기록한다.
				else{
					
					bw = new BufferedWriter(new FileWriter(file));
					bw.write(String.valueOf(pmcCH1AccuPower));
					bw.flush();
					
					DynamicConfUtils.getInstance().setPowerPeakConsecutiveTime(0L); // 발생연속시간 초기화
					DynamicConfUtils.getInstance().setPowerPeak(false); // 전력피크 발생
				}
			}
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}finally{
			try{
				if(br != null) br.close();
				if(bw != null) bw.close();
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	// 20kW 미만
	private void doProcessUnder20(long contractPower){
		
	}
}
