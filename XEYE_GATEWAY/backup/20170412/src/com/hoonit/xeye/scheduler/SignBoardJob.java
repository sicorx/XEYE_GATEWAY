package com.hoonit.xeye.scheduler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.InformationManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;

public class SignBoardJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		logger.info("signboard job is executing...");
		
		try{
			
			String signBoardCtlOID = EnterpriseOIDManager.getEnterpriseOID() + ".20.1.0"; // 제어
			String signBoard1OID   = EnterpriseOIDManager.getEnterpriseOID() + ".20.2.0"; // 간판1 상태
			String signBoard2OID   = EnterpriseOIDManager.getEnterpriseOID() + ".20.3.0"; // 간판2 상태
			
			List<String> list = new ArrayList<String>();
			list.add(signBoard1OID);
			list.add(signBoard2OID);
			
			Map<String, String> resultMap = SNMPAgentProxyManager.getInstance().getOIDValues(list);
			
			if(resultMap.get(signBoard1OID) != null && resultMap.get(signBoard2OID) != null){
				
				// 간판 상태
				String signBoard1 = resultMap.get(signBoard1OID);
				String signBoard2 = resultMap.get(signBoard2OID);
				
				// 일출시분
				String t1 = InformationManager.getInstance().getSunRiseTime();
				String m1 = InformationManager.getInstance().getSunRiseMinute();
				
				// 일몰시분
				String t2 = InformationManager.getInstance().getSunSetTime();
				String m2 = InformationManager.getInstance().getSunSetMinute();
				
				if(t1 != null && m1 != null && t2 != null && m2 != null){
					
					Calendar nowCal = Calendar.getInstance();
					
					int nowHour   = nowCal.get(Calendar.HOUR_OF_DAY);
					int nowMinute = nowCal.get(Calendar.MINUTE);
					
					logger.info("current time:"+nowHour);
					logger.info("current minute:"+nowMinute);
					
					// 오후
					if(nowHour > 12 && nowMinute > 0){
						
						// 일몰시간 체크
						Calendar ssCal = Calendar.getInstance();
						ssCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t2));
						ssCal.set(Calendar.MINUTE, Integer.parseInt(m2));
						
						logger.info("sunset time:"+ssCal.get(Calendar.HOUR_OF_DAY));
						logger.info("sunset minute:"+ssCal.get(Calendar.MINUTE));
						
						if((nowCal.get(Calendar.HOUR_OF_DAY) >= ssCal.get(Calendar.HOUR_OF_DAY)) 
								&& (nowCal.get(Calendar.MINUTE) >= ssCal.get(Calendar.MINUTE))){
							
							logger.info("signboard1 status:"+signBoard1);
							logger.info("signboard2 status:"+signBoard2);
							
							// 간판이 꺼져 있으면 켠다.
							if("0".equals(signBoard1) && "0".equals(signBoard2)){
								
								logger.info("signboard is now on");
								
								SNMPAgentProxyManager.getInstance().setOIDValue(signBoardCtlOID, "1");
							}
						}
					}
					// 오전
					else{
						
						// 일출시간 체크
						Calendar srCal = Calendar.getInstance();
						srCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t1));
						srCal.set(Calendar.MINUTE, Integer.parseInt(m1));
						
						logger.info("sunrise time:"+srCal.get(Calendar.HOUR_OF_DAY));
						logger.info("sunrise minute:"+srCal.get(Calendar.MINUTE));
						
						if((nowCal.get(Calendar.HOUR_OF_DAY) >= srCal.get(Calendar.HOUR_OF_DAY)) 
								&& (nowCal.get(Calendar.MINUTE) >= srCal.get(Calendar.MINUTE))){
							
							logger.info("signboard1 status:"+signBoard1);
							logger.info("signboard2 status:"+signBoard2);
							
							// 간판이 켜져 있으면 끈다.
							if("1".equals(signBoard1) && "1".equals(signBoard2)){
								
								logger.info("signboard is now off");
								
								SNMPAgentProxyManager.getInstance().setOIDValue(signBoardCtlOID, "0");
							}
						}
					}
				}
			}
			
			logger.info("signboard job is executed...");
			
		}catch(Exception e){
			logger.info(e.getMessage(), e);
		}
	}
}
