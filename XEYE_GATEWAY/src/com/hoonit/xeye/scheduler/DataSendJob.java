package com.hoonit.xeye.scheduler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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

public class DataSendJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private List<String> oidList;
	
	// 이전 누적사용량
	private long ch1PreAcc  = 0L;
	private long ch2PreAcc  = 0L;
	private long ch3PreAcc  = 0L;
	private long ch4PreAcc  = 0L;
	private long ch5PreAcc  = 0L;
	private long ch6PreAcc  = 0L;
	private long ch7RPreAcc = 0L;
	private long ch7SPreAcc = 0L;
	private long ch7TPreAcc = 0L;
	private long ch8RPreAcc = 0L;
	private long ch8SPreAcc = 0L;
	private long ch8TPreAcc = 0L;
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		logger.info("Data Send job is executing...");
		
		initOIDs();
		
		doProcess();
		
		logger.info("Data Send job is executed...");
	}
	
	private void initOIDs(){
		
		this.oidList = new ArrayList<String>();
		
		// 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.0.0");
		
		// PMC
		for(short i = 0; i < 36; i++){
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10."+(i+1)+".0");
		}
		
		// 테몬
		for(short i = 0; i < 16; i++){
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40."+(i+1)+".0");
		}
		
		// 알몬
		for(short i = 0; i < 4; i++){
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".30."+(i+1)+".0");
		}
		
		// 하콘
		for(short i = 0; i < 5; i++){
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".1.0"); // 연결상태
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".2.0"); // 통신상태
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".3.0"); // 자체온도
		}
		
		// 티센서(무선)
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".69.1.0"); // 개수
		for(short i = 0; i < 10; i++){
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+70)+".1.0"); // 구분
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+70)+".2.0"); // 온도
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+70)+".3.0"); // 습도
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+70)+".4.0"); // 배터리잔량
		}
		
		// 티센서(유선)
		for(short i = 0; i < 5; i++){
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+50)+".1.0"); // 연결상태
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+50)+".2.0"); // 통신상태
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+50)+".3.0"); // 온도
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+50)+".4.0"); // 습도
		}
		
		// 간판상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".20.2.0"); // 1번 상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".20.3.0"); // 2번 상태
		
		// 인버터허브
		for(int i = 0; i < ((36*8)+1); i++){
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(i+1)+".0");
		}
	}
	
	private void doProcess(){
		
		BufferedWriter bw = null;
		
		try{
			
			String storeCD = DynamicConfUtils.getInstance().getStoreCD();
			
			if(!"".equals(storeCD)){
				
				String dataLoadComplete = SNMPAgentProxyManager.getInstance().getStaticOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".1.99.0");
				
				logger.info("Data Load Completed="+dataLoadComplete);
				
				if("1".equals(dataLoadComplete)){
				
					Calendar cal = Calendar.getInstance();
					int year   = cal.get(Calendar.YEAR);
					int month  = cal.get(Calendar.MONTH) + 1;
					int date   = cal.get(Calendar.DATE);
					int hour   = cal.get(Calendar.HOUR_OF_DAY);
					int minute = cal.get(Calendar.MINUTE);
					int second = cal.get(Calendar.SECOND);
					
					logger.info("Current Date="+year+"-"+month+"-"+date+" "+hour+":"+minute+":"+second);
					
					// 5분마다 저장하는 이전 누적전력량
					File preAccPowerFile = new File("resource/conf/5m_acc_power.txt");
					
					if(!preAccPowerFile.exists()){
						preAccPowerFile.createNewFile();
					}else{
					
						BufferedReader br = null;
						
						try{
							
							br = new BufferedReader(new FileReader(preAccPowerFile));
							
							StringTokenizer st = new StringTokenizer(br.readLine(), ",");
							
							if(st.countTokens() == 13){
								
								StringTokenizer st2 = new StringTokenizer(st.nextToken(), "-");
								
								String pyear   = st2.nextToken();
								String pmonth  = st2.nextToken();
								String pdate   = st2.nextToken();
								String phour   = st2.nextToken();
								String pminute = st2.nextToken();
								String psecond = st2.nextToken();
								
								logger.info("Previous 5m acc power save date="+pyear+"-"+pmonth+"-"+pdate+" "+phour+":"+pminute+":"+psecond);
								
								// 원칙적으로는 5분전 누적데이터가 맞는데 공사 등으로 시간이 더 걸릴 수 있으니
								// 시간 부분을 예외로 처리함으로 주석처리
								/*Calendar cal2 = Calendar.getInstance();
								cal2.set(Calendar.YEAR, Integer.parseInt(pyear));
								cal2.set(Calendar.MONTH, Integer.parseInt(pmonth)-1);
								cal2.set(Calendar.DATE, Integer.parseInt(pdate));
								cal2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(phour));
								cal2.set(Calendar.MINUTE, Integer.parseInt(pminute));
								cal2.set(Calendar.SECOND, Integer.parseInt(psecond));
								
								float elapsedTime = (float)((cal.getTimeInMillis() - cal2.getTimeInMillis()) / 1000.0);
							
								logger.info("Elapsed Time="+elapsedTime);*/
								
								// 300초(5분) 전 누적데이터이면
				            	//if(elapsedTime > 250 && elapsedTime < 350){
				            		
									ch1PreAcc  = Long.parseLong(st.nextToken());
									ch2PreAcc  = Long.parseLong(st.nextToken());
									ch3PreAcc  = Long.parseLong(st.nextToken());
									ch4PreAcc  = Long.parseLong(st.nextToken());
									ch5PreAcc  = Long.parseLong(st.nextToken());
									ch6PreAcc  = Long.parseLong(st.nextToken());
									ch7RPreAcc = Long.parseLong(st.nextToken());
									ch7SPreAcc = Long.parseLong(st.nextToken());
									ch7TPreAcc = Long.parseLong(st.nextToken());
									ch8RPreAcc = Long.parseLong(st.nextToken());
									ch8SPreAcc = Long.parseLong(st.nextToken());
									ch8TPreAcc = Long.parseLong(st.nextToken());
				            	//}
							}
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}finally{
							
							try{
								if(br!= null) br.close();
							}catch(Exception e){
								logger.error(e.getMessage(), e);
							}
						}
					}
					
					int idx = 0;
					
					byte[] buffer = new byte[701];
					
					// STX
					buffer[idx++] = 0x02;
					// LEN
			        byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x2B9);
			        buffer[idx++] = lenBytes[0];
			        buffer[idx++] = lenBytes[1];
			        // CMD
			        buffer[idx++] = 0x02;
			        // 매장코드
			        int storeCDLen = 20;
			        try{
			        	
			    		byte[] storeBytes = Utils.getFillNullByte(storeCD.getBytes(), storeCDLen);
			    		
			    		for(int i = 0; i < storeBytes.length; i++){
			    			buffer[idx++] = storeBytes[i];
			    		}
			        }catch(ArrayIndexOutOfBoundsException e){
			        	logger.error(e.getMessage(), e);
			        	
			        	for(int i = 0; i < storeCDLen; i++){
			        		buffer[idx++] = 0x20;
			    		}
			        }
			        // GW ID
			        byte[] gwID = ByteUtils.toUnsignedShortBytes(Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getGwID(), "0")));
			        buffer[idx++] = gwID[0];
			        buffer[idx++] = gwID[1];
			        // 년
			        byte[] yearBytes = ByteUtils.toUnsignedShortBytes(year);
			        buffer[idx++] = yearBytes[0];
			        buffer[idx++] = yearBytes[1];
			        // 월
			        buffer[idx++] = (byte)month;
			        // 일
			        buffer[idx++] = (byte)date;
			        // 시
			        buffer[idx++] = (byte)hour;
			        // 분
			        buffer[idx++] = (byte)minute;
			        // 초
			        buffer[idx++] = (byte)second;
			        
			        Map<String, String> dataMap = SNMPAgentProxyManager.getInstance().getOIDValues(oidList);
			        
			        // 통신상태
			        String commStatus = dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.0.0");
			        
			        //=============PMC=============
			        byte[] temp;
			        
			        // 통신상태
		        	buffer[idx++] = ByteUtils.toByte(commStatus, (byte)0x01);
		        	
			        // 통신샃태가 정상이면
			        if("0".equals(commStatus)){
			        	
			        	// 1CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.1.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 1CH 5분 전력사용량
			        	long use5M = 0;
		        		if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.1.0")) >= ch1PreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.1.0")) - ch1PreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch1PreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.1.0"));
			        	// 1CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.2.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 1CH 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.3.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 2CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.4.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 2CH 5분 전력사용량
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.4.0")) >= ch2PreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.4.0")) - ch2PreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch2PreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.4.0"));
			        	// 2CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.5.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 2CH 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.6.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 3CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 3CH 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0")) - ch3PreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0")) >= ch3PreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0")) - ch3PreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch3PreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0"));
			        	// 3CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.8.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 3CH 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.9.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 4CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.10.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 4CH 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.10.0")) - ch4PreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.10.0")) >= ch4PreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.10.0")) - ch4PreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch4PreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.10.0"));
			        	// 4CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.11.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 4CH 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.12.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 5CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.13.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 5CH 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.13.0")) - ch5PreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.13.0")) >= ch5PreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.13.0")) - ch5PreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch5PreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.13.0"));
			        	// 5CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.14.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 5CH 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.13.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 6CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 6CH 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0")) - ch6PreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0")) >= ch6PreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0")) - ch6PreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch6PreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0"));
			        	// 6CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.17.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 6CH 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.18.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 7CH R 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.19.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH R 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.19.0")) - ch7RPreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.19.0")) >= ch7RPreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.19.0")) - ch7RPreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch7RPreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.19.0"));
			        	// 7CH R 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.20.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH R 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.21.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 7CH S 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.22.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH S 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.22.0")) - ch7SPreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.22.0")) >= ch7SPreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.22.0")) - ch7SPreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch7SPreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.22.0"));
			        	// 7CH S 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.23.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH S 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.24.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 7CH T 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.25.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH T 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.25.0")) - ch7TPreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.25.0")) >= ch7TPreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.25.0")) - ch7TPreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch7TPreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.25.0"));
			        	// 7CH T 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.26.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH T 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.27.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 8CH R 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.28.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH R 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.28.0")) - ch8RPreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.28.0")) >= ch8RPreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.28.0")) - ch8RPreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch8RPreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.28.0"));
			        	// 8CH R 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.29.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH R 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.30.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 8CH S 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.31.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH S 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.31.0")) - ch8SPreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.31.0")) >= ch8SPreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.31.0")) - ch8SPreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch8SPreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.31.0"));
			        	// 8CH S 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.32.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH S 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.33.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	// 8CH T 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.34.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH T 5분 전력사용량
			        	//use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.34.0")) - ch8TPreAcc;
			        	use5M = 0;
			        	if(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.34.0")) >= ch8TPreAcc){
		        			use5M = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.34.0")) - ch8TPreAcc;
		        		}
			        	temp = ByteUtils.toUnsignedIntBytes(use5M);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	ch8TPreAcc = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.34.0"));
			        	// 8CH T 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.35.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH T 상태
			        	if("0".equals(StringUtils.defaultIfEmpty(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.36.0"), "0"))){
			        		buffer[idx++] = 0x00;
			        	}else{
			        		buffer[idx++] = 0x01;
			        	}
			        	
			        }
			        // 통신불량이면
			        else{
			        	
			        	// 1CH 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 1CH 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 1CH 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 1CH 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 2CH 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 2CH 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 2CH 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 2CH 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 3CH 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 3CH 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 3CH 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 3CH 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 4CH 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 4CH 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 4CH 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 4CH 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 5CH 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 5CH 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 5CH 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 5CH 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 6CH 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 6CH 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 6CH 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 6CH 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 7CH R 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH R 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH R 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH R 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 7CH S 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH S 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH S 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH S 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 7CH T 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH T 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH T 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH T 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 8CH R 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH R 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH R 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH R 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 8CH S 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH S 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH S 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH S 상태
			        	buffer[idx++] = 0x00;
			        	
			        	// 8CH T 누적사용량
			        	temp = ByteUtils.toBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH T 5분 전력사용량
		        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH T 순간사용량
			        	temp = ByteUtils.toUnsignedIntBytes(0L);
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH T 상태
			        	buffer[idx++] = 0x00;
			        }
			        
			        //=============테몬=============
			        if("0".equals(commStatus)){
			        	
			        	short val = 0;
			        	
			        	for(int i = 0; i < 16; i++){
			        		
				        	try{
					        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".40."+(i+1)+".0"));
					        	temp = ByteUtils.toBytes(val);
				        	}catch(Exception e){
				        		temp = ByteUtils.toBytes((short)-9999);
				        	}
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}
			        	
			        }else{
			        	
			        	for(int i = 0; i < 16; i++){
			        		
				        	temp = ByteUtils.toBytes((short)-9999);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}
			        }
			        
			        //=============간판상태=============
			        if("0".equals(commStatus)){
			        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".20.2.0"), (byte)0x00);
			        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".20.3.0"), (byte)0x00);
			        }else{
			        	buffer[idx++] = 0x00;
			        	buffer[idx++] = 0x00;
			        }
			        
			        //=============알몬=============
			        if("0".equals(commStatus)){
			        	for(int i = 0; i < 4; i++){
			        		buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".30."+(i+1)+".0"), (byte)0x00);
			        	}
			        }else{
			        	for(int i = 0; i < 4; i++){
			        		buffer[idx++] = 0x00;
			        	}
			        }
			        
			        //=============하콘=============
			        if("0".equals(commStatus)){
			        	
			        	short val = 0;
			        	
			        	short doid = 60;
			        	
			        	for(int i = 0; i < 5; i++){
			        		
			        		// 연결상태
				        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".1.0"), (byte)0x01);
				        	
			        		// 온도
				        	try{
					        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".3.0"));
					        	temp = ByteUtils.toBytes(val);
				        	}catch(Exception e){
				        		temp = ByteUtils.toBytes((short)-9999);
				        	}
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	
				        	doid++;
			        	}
			        	
			        }else{
			        	
			        	for(int i = 0; i < 5; i++){
			        		
			        		// 연결상태
				        	buffer[idx++] = (byte)0x01;
				        	
				        	temp = ByteUtils.toBytes((short)-9999);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}
			        }
			        
			        //=============유선티센서=============
			        if("0".equals(commStatus)){
			        	
			        	short val = 0;
			        	
			        	short doid = 50;
			        	
			        	for(int i = 0; i < 5; i++){
			        		
			        		// 연결상태
				        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".1.0"), (byte)0x01);
				        	
			        		// 온도
				        	try{
					        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".3.0"));
					        	temp = ByteUtils.toBytes(val);
				        	}catch(Exception e){
				        		temp = ByteUtils.toBytes((short)-9999);
				        	}
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	
				        	// 습도
				        	try{
					        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".4.0"));
					        	temp = ByteUtils.toBytes(val);
				        	}catch(Exception e){
				        		temp = ByteUtils.toBytes((short)-9999);
				        	}
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        		
				        	doid++;
			        	}
			        	
			        }else{
			        	
			        	for(int i = 0; i < 5; i++){
			        		
			        		// 연결상태
				        	buffer[idx++] = (byte)0x01;
				        	
				        	// 온도
			        		temp = ByteUtils.toBytes((short)-9999);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	
				        	// 습도
			        		temp = ByteUtils.toBytes((short)-9999);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}
			        }
			        
			        //=============무선티센서(BLE)=============
			        if("0".equals(commStatus)){
			        	
			        	byte bleCnt = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".69.1.0"), (byte)0x00);
			        	
			        	// BLE 개수
		        		buffer[idx++] = bleCnt;
		        		
			        	// 개수가 0보다 크면
			        	if(bleCnt > 0x00){
			        		
			        		short val = 0;
				        	
				        	short doid = 70;
				        	
			        		for(int i = 0; i < bleCnt; i++){
			        			
			        			// 구분
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".1.0"), (byte)0x00);
			        			
			        			// 온도
					        	try{
						        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".2.0"));
						        	temp = ByteUtils.toBytes(val);
					        	}catch(Exception e){
					        		temp = ByteUtils.toBytes((short)-9999);
					        	}
					        	
					        	for(byte b : temp)
					        		buffer[idx++] = b;
					        	
					        	// 습도
					        	try{
						        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".3.0"));
						        	temp = ByteUtils.toBytes(val);
					        	}catch(Exception e){
					        		temp = ByteUtils.toBytes((short)-9999);
					        	}
					        	
					        	for(byte b : temp)
					        		buffer[idx++] = b;
					        	
					        	// 배터리 잔량
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".4.0"), (byte)0x00);
			        			
			        			doid++;
			        		}
			        	}
			        	
			        	// 나머지 미연결 정보 세팅
			        	for(int i = 0; i < (10-bleCnt); i++){
			        		
			        		// 구분
			        		buffer[idx++] = 0x00;
			        		
			        		// 온도
			        		temp = ByteUtils.toBytes((short)-9999);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	
				        	// 습도
			        		temp = ByteUtils.toBytes((short)-9999);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	
				        	// 배터리잔량
			        		buffer[idx++] = 0x00;
			        	}
			        	
			        }else{
			        	
			        	// BLE 개수
		        		buffer[idx++] = 0x00;
		        		
			        	for(int i = 0; i < 10; i++){
			        		
			        		// 구분
			        		buffer[idx++] = 0x00;
			        		
			        		// 온도
			        		temp = ByteUtils.toBytes((short)-9999);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	
				        	// 습도
			        		temp = ByteUtils.toBytes((short)-9999);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	
				        	// 배터리잔량
			        		buffer[idx++] = 0x00;
			        	}
			        }
			        
			        //=============인버터허브=============
			        if("0".equals(commStatus)){
			        	
			        	byte hubCnt = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "90.289.0"), (byte)0x00);
			        	
			        	// 냉장장비 개수
		        		buffer[idx++] = hubCnt;
		        		
			        	// 냉장장비개수가 0보다 크면
			        	if(hubCnt > 0x00){
			        		
			        		short val = 0;
				        	
				        	short toid = 1;
				        	
			        		for(int i = 0; i < hubCnt; i++){
			        			
			        			// Addr
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// F/W 버전
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 타입
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 모델
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 정격용량
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람MASK1 - 실내고온알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람MASK1 - 실내저온알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람MASK1 - 실내온도센서
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람MASK1 - 실내제상온도센서
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람MASK1 - 고압알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람MASK1 - 저압알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람MASK1 - 압축기알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람MASK1 - RESERVED
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람1 - 실내고온알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람1 - 실내저온알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람1 - 실내온도센서
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람1 - 실내제상온도센서
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람1 - 고압알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람1 - 저압알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람1 - 알축기알람
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 알람1 - RESERVED
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 에러코드
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 운전모드
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 실내온도
					        	try{
						        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"));
						        	temp = ByteUtils.toBytes(val);
					        	}catch(Exception e){
					        		temp = ByteUtils.toBytes((short)0);
					        	}
					        	
					        	for(byte b : temp)
					        		buffer[idx++] = b;
					        	// 실내설정온도
					        	try{
						        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"));
						        	temp = ByteUtils.toBytes(val);
					        	}catch(Exception e){
					        		temp = ByteUtils.toBytes((short)0);
					        	}
					        	
					        	for(byte b : temp)
					        		buffer[idx++] = b;
					        	// 실외기온도
					        	try{
						        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"));
						        	temp = ByteUtils.toBytes(val);
					        	}catch(Exception e){
					        		temp = ByteUtils.toBytes((short)0);
					        	}
					        	
					        	for(byte b : temp)
					        		buffer[idx++] = b;
					        	// 고온경보 사용유무
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 고온경보 설정온도
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 저온경보 사용유무
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 저온경보 설정온도
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 실내제상온도
					        	try{
						        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"));
						        	temp = ByteUtils.toBytes(val);
					        	}catch(Exception e){
					        		temp = ByteUtils.toBytes((short)0);
					        	}
					        	
					        	for(byte b : temp)
					        		buffer[idx++] = b;
					        	// 제상/제수 동작기준
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 제상복귀온도 설정
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 제상간격
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 제상시간
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
			        			// 제수시간
					        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(toid++)+".0"), (byte)0x00);
					        	
			        		} // end for
			        	}
			        	
			        	// 나머지 미연결 정보 세팅
			        	for(int i = 0; i < (8-hubCnt); i++){
			        		
			        		// Addr
		        			buffer[idx++] = 0x00;
			        		// F/W 버전
		        			buffer[idx++] = 0x00;
		        			// 타입
		        			buffer[idx++] = 0x00;
		        			// 모델
		        			buffer[idx++] = 0x00;
		        			// 정격용량
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 실내고온알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 실내저온알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 실내온도센서
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 실내제상온도센서
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 고압알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 저압알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 압축기알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - RESERVED
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 실내고온알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 실내저온알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 실내온도센서
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 실내제상온도센서
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 고압알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 저압알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 알축기알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - RESERVED
		        			buffer[idx++] = 0x00;
		        			// 에러코드
		        			buffer[idx++] = 0x00;
		        			// 운전모드
		        			buffer[idx++] = 0x00;
		        			// 실내온도
				        	temp = ByteUtils.toBytes((short)0);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	// 실내설정온도
				        	temp = ByteUtils.toBytes((short)0);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	// 실외기온도
				        	temp = ByteUtils.toBytes((short)0);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	// 고온경보 사용유무
		        			buffer[idx++] = 0x00;
		        			// 고온경보 설정온도
		        			buffer[idx++] = 0x00;
		        			// 저온경보 사용유무
		        			buffer[idx++] = 0x00;
		        			// 저온경보 설정온도
		        			buffer[idx++] = 0x00;
		        			// 실내제상온도
				        	temp = ByteUtils.toBytes((short)0);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	// 제상/제수 동작기준
		        			buffer[idx++] = 0x00;
		        			// 제상복귀온도 설정
		        			buffer[idx++] = 0x00;
		        			// 제상간격
		        			buffer[idx++] = 0x00;
		        			// 제상시간
		        			buffer[idx++] = 0x00;
		        			// 제수시간
		        			buffer[idx++] = 0x00;
			        	}
			        	
			        }else{
			        	
			        	// 냉장장비 개수
		        		buffer[idx++] = 0x00;
		        		
			        	for(int i = 0; i < 8; i++){
			        		
			        		// Addr
		        			buffer[idx++] = 0x00;
			        		// F/W 버전
		        			buffer[idx++] = 0x00;
		        			// 타입
		        			buffer[idx++] = 0x00;
		        			// 모델
		        			buffer[idx++] = 0x00;
		        			// 정격용량
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 실내고온알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 실내저온알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 실내온도센서
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 실내제상온도센서
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 고압알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 저압알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - 압축기알람
		        			buffer[idx++] = 0x00;
		        			// 알람MASK1 - RESERVED
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 실내고온알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 실내저온알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 실내온도센서
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 실내제상온도센서
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 고압알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 저압알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - 압축기알람
		        			buffer[idx++] = 0x00;
		        			// 알람1 - RESERVED
		        			buffer[idx++] = 0x00;
		        			// 에러코드
		        			buffer[idx++] = 0x00;
		        			// 운전모드
		        			buffer[idx++] = 0x00;
		        			// 실내온도
				        	temp = ByteUtils.toBytes((short)0);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	// 실내설정온도
				        	temp = ByteUtils.toBytes((short)0);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	// 실외기온도
				        	temp = ByteUtils.toBytes((short)0);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	// 고온경보 사용유무
		        			buffer[idx++] = 0x00;
		        			// 고온경보 설정온도
		        			buffer[idx++] = 0x00;
		        			// 저온경보 사용유무
		        			buffer[idx++] = 0x00;
		        			// 저온경보 설정온도
		        			buffer[idx++] = 0x00;
		        			// 실내제상온도
				        	temp = ByteUtils.toBytes((short)0);
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	// 제상/제수 동작기준
		        			buffer[idx++] = 0x00;
		        			// 제상복귀온도 설정
		        			buffer[idx++] = 0x00;
		        			// 제상간격
		        			buffer[idx++] = 0x00;
		        			// 제상시간
		        			buffer[idx++] = 0x00;
		        			// 제수시간
		        			buffer[idx++] = 0x00;
			        	}
			        }
			        
			        // CRC
					byte[] crc = Utils.getBytes(buffer, 3, buffer.length-6);
					
					short sCRC = CRC16.getInstance().getCRC(crc);
					
					ByteBuffer wBuffer = ByteBuffer.allocate(2);
					wBuffer.putShort(sCRC);
					wBuffer.flip();
					
					byte crc1 = wBuffer.get();
					byte crc2 = wBuffer.get();
					
					buffer[idx++] = crc1;
					buffer[idx++] = crc2;
					buffer[idx++] = 0x03;
			        
					try{
						
						IFClient client = IFClientProxyManager.getInstance().getIFClient();
						
						if(client != null){
							if(client.isSocketConnected()){
								
								client.write(buffer);
									
								// 데이터 최초 전송여부
						        if(!DynamicConfUtils.getInstance().isDataSendAtFirst())
						        	DynamicConfUtils.getInstance().setDataSendAtFirst(true);
							}
						}
					}catch(Exception e){
						logger.error(e.getMessage(), e);
					}
					
			        // 이전 누적데이터 저장
			        bw = new BufferedWriter(new FileWriter(preAccPowerFile));
			        
			        StringBuffer sb = new StringBuffer();
			        sb.append(String.valueOf(year));
			        sb.append("-");
			        sb.append(String.valueOf(month));
			        sb.append("-");
			        sb.append(String.valueOf(date));
			        sb.append("-");
			        sb.append(String.valueOf(hour));
			        sb.append("-");
			        sb.append(String.valueOf(minute));
			        sb.append("-");
			        sb.append(String.valueOf(second));
			        sb.append(",");
			        sb.append(String.valueOf(ch1PreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch2PreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch3PreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch4PreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch5PreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch6PreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch7RPreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch7SPreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch7TPreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch8RPreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch8SPreAcc));
			        sb.append(",");
			        sb.append(String.valueOf(ch8TPreAcc));
			        
			        bw.write(sb.toString());
			        bw.flush();
				}
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}finally{
			
			try{
				if(bw != null) bw.close();
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
	}
}
