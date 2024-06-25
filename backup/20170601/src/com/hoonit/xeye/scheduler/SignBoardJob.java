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

public class SignBoardJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private final byte STX              = 0x02;
	private final byte ETX              = 0x03;
	
	private final byte NORMAL           = 0x01; // 정상
	//private final byte ERR_STX_ETX      = 0x02; // STX, ETX 오류
	//private final byte ERR_CRC          = 0x03; // CRC 오류
	//private final byte ERR_INVALID_DATA = 0x04; // 유효하지 않은 데이터 오류
	//private final byte ERR_FILE_TRANS   = 0x05; // 파일전송 오류
	private final byte ERR_CTRL         = 0x06; // 제어오류
	//private final byte ERR_EXCEPTION    = 0x07; // Exception 발생 오류
	//private final byte ERR_STR_NOEXIST  = 0x08; // 매장정보 미존재 오류
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		logger.info("signboard job is executing...");
		
		if(!"".equals(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getStoreCD(), ""))){
		
			try{
				
				// 일출시분
				int t1 = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getSunRiseTime(), "255"));
				int m1 = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getSunRiseMinute(), "255"));
				
				// 일몰시분
				int t2 = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getSunSetTime(), "255"));
				int m2 = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getSunSetMinute(), "255"));
				
				logger.info("sunrise time="+t1);
				logger.info("sunrise minute="+m1);
				logger.info("sunset time="+t2);
				logger.info("sunset minute="+m2);
				
				// 일출일몰시간이 설정되어 있으면
				if(t1 < 255 && m1 < 255 && t2 < 255 && m2 < 255){
				
					String dataLoadComplete = SNMPAgentProxyManager.getInstance().getStaticOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".1.99.0");
					
					logger.info("Data Load Complete="+dataLoadComplete);
					
					// 데이터로딩이 끝났으면
					if("1".equals(dataLoadComplete)){
						
						String commStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".10.0.0");
						
						logger.info("Comm Status="+commStatus);
						
						// 통신상태가 정상이면
						if("0".equals(commStatus)){
							
							Calendar nowCal = Calendar.getInstance();
							
							int nowHour   = nowCal.get(Calendar.HOUR_OF_DAY);
							int nowMinute = nowCal.get(Calendar.MINUTE);
							
							logger.info("current time="+nowHour);
							logger.info("current minute="+nowMinute);
							
							List<String> list = new ArrayList<String>();
							list.add(EnterpriseOIDManager.getEnterpriseOID()+".20.2.0");
							list.add(EnterpriseOIDManager.getEnterpriseOID()+".20.3.0");
							
							Map<String, String> map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
							
							String status1 = map.get(EnterpriseOIDManager.getEnterpriseOID()+".20.2.0");
							String status2 = map.get(EnterpriseOIDManager.getEnterpriseOID()+".20.3.0");
							
							logger.info("간판1 상태="+status1);
							logger.info("간판2 상태="+status2);
							
							// 오후
							if(nowHour >= 12 && nowMinute >= 0){
								
								// 일몰시간 체크
								Calendar ssCal = Calendar.getInstance();
								ssCal.set(Calendar.HOUR_OF_DAY, t2);
								ssCal.set(Calendar.MINUTE, m2);
								
								logger.info("sunset time:"+ssCal.get(Calendar.HOUR_OF_DAY));
								logger.info("sunset minute:"+ssCal.get(Calendar.MINUTE));
								
								if((nowCal.get(Calendar.HOUR_OF_DAY) >= ssCal.get(Calendar.HOUR_OF_DAY)) 
										&& (nowCal.get(Calendar.MINUTE) >= ssCal.get(Calendar.MINUTE))){
									
									// 간판이 꺼져 있으면 켠다.
									if("0".equals(status1) && "0".equals(status2)){
										logger.info("SIGN ON...");
										doProcess((byte)0x01, status1, status2);
									}
								}
							}
							// 오전
							else{
								
								// 일출시간 체크
								Calendar srCal = Calendar.getInstance();
								srCal.set(Calendar.HOUR_OF_DAY, t1);
								srCal.set(Calendar.MINUTE, m1);
								
								logger.info("sunrise time:"+srCal.get(Calendar.HOUR_OF_DAY));
								logger.info("sunrise minute:"+srCal.get(Calendar.MINUTE));
								
								if((nowCal.get(Calendar.HOUR_OF_DAY) >= srCal.get(Calendar.HOUR_OF_DAY)) 
										&& (nowCal.get(Calendar.MINUTE) >= srCal.get(Calendar.MINUTE))){
									
									// 간판이 켜져 있으면 끈다.
									if("1".equals(status1) && "1".equals(status2)){
										logger.info("SIGN OFF...");
										doProcess((byte)0x00, status1, status2);
									}
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
	
	private void doProcess(byte ctlGubun, String status1, String status2){
		
		// Validation
		byte result = NORMAL;
		
		// 기존 간판상태
		byte statusOld = 0x00;
		// 현재 간판상태
		byte status = 0x00;
		
		try{
			
			if(!"".equals(DynamicConfUtils.getInstance().getStoreCD())){
				
				Calendar cal = Calendar.getInstance();
				int year   = cal.get(Calendar.YEAR);
				int month  = cal.get(Calendar.MONTH) + 1;
				int date   = cal.get(Calendar.DATE);
				int hour   = cal.get(Calendar.HOUR_OF_DAY);
				int minute = cal.get(Calendar.MINUTE);
				int second = cal.get(Calendar.SECOND);
				
				// 기존 간판상태
				if("0".equals(StringUtils.defaultIfEmpty(status1, "0")) && "0".equals(StringUtils.defaultIfEmpty(status2, "0"))){
					statusOld = 0x00;
				}else{
					statusOld = 0x01;
				}
				
				// 제어시작
				int res = SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID()+".20.1.0", String.valueOf(ctlGubun));
				
				logger.info("제어결과="+res);
				
				if(res == -1){
					result = ERR_CTRL;
				}
				
				List<String> list = new ArrayList<String>();
				list.add(EnterpriseOIDManager.getEnterpriseOID()+".20.2.0");
				list.add(EnterpriseOIDManager.getEnterpriseOID()+".20.3.0");
				
				Map<String, String> map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
				
				status1 = map.get(EnterpriseOIDManager.getEnterpriseOID()+".20.2.0");
				status2 = map.get(EnterpriseOIDManager.getEnterpriseOID()+".20.3.0");
				
				logger.info("현재간판1 상태="+status1);
				logger.info("현재간판2 상태="+status2);
				
				// 현재 간판상태
				if("0".equals(StringUtils.defaultIfEmpty(status1, "0")) && "0".equals(StringUtils.defaultIfEmpty(status2, "0"))){
					status = 0x00;
				}else{
					status = 0x01;
				}
				
				// 결과 전송
				short wIdx = 0;
				byte[] writeBuffer = new byte[41];
				writeBuffer[wIdx++] = STX;
				byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x25);
				writeBuffer[wIdx++] = lenBytes[0];
				writeBuffer[wIdx++] = lenBytes[1];
				writeBuffer[wIdx++] = 0x09;
				writeBuffer[wIdx++] = result;
				
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
				writeBuffer[wIdx++] = statusOld; // 기존 간판상태
				writeBuffer[wIdx++] = status;    // 현재 간판상태
				writeBuffer[wIdx++] = 0x00;      // 제어주체(0x00:SEMS, 0x01:Man)
				// 날씨코드
				writeBuffer[wIdx++] = ByteUtils.toByte(DynamicConfUtils.getInstance().getWeatherCD(), (byte)0x00);
				
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
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
}
