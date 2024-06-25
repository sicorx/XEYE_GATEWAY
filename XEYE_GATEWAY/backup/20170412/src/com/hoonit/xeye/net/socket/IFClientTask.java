package com.hoonit.xeye.net.socket;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;
import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.DynamicConfUtils;
import com.hoonit.xeye.util.Utils;

public class IFClientTask extends TimerTask {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private IFClient client;
	
	private List<String> oidList;
	
	// 5분 전력사용량
	private long ch1M5  = 0L;
	private long ch2M5  = 0L;
	private long ch3M5  = 0L;
	private long ch4M5  = 0L;
	private long ch5M5  = 0L;
	private long ch6M5  = 0L;
	private long ch7RM5 = 0L;
	private long ch7SM5 = 0L;
	private long ch7TM5 = 0L;
	private long ch8RM5 = 0L;
	private long ch8SM5 = 0L;
	private long ch8TM5 = 0L;
	
	public IFClientTask(IFClient client){
		this.client = client;
		this.oidList = new ArrayList<String>();
		
		// 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.0.0");
		
		// PMC
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.110.0"); // 1CH 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.111.0"); // 1CH 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.112.0"); // 2CH 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.113.0"); // 2CH 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.114.0"); // 3CH 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.115.0"); // 3CH 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.116.0"); // 4CH 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.117.0"); // 4CH 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.118.0"); // 5CH 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.119.0"); // 5CH 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.120.0"); // 6CH v
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.121.0"); // 6CH 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0");   // 7CH R 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.31.0");  // 7CH R 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.15.0");  // 7CH S 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.39.0");  // 7CH S 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.23.0");  // 7CH T 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.47.0");  // 7CH T 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.8.0");   // 8CH R 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.32.0");  // 8CH R 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0");  // 8CH S 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.40.0");  // 8CH S 순시전력
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.24.0");  // 8CH T 누적량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".10.48.0");  // 8CH T 순시전력
		
		// 테몬
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.1.0");  // 1CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.2.0");  // 2CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.3.0");  // 3CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.4.0");  // 4CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.5.0");  // 5CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.6.0");  // 6CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.7.0");  // 7CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.8.0");  // 8CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.9.0");  // 9CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.10.0"); // 10CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.11.0"); // 11CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.12.0"); // 12CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.13.0"); // 13CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.14.0"); // 14CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.15.0"); // 15CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".40.16.0"); // 16CH
		
		// 알몬
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".30.1.0"); // 1CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".30.2.0"); // 2CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".30.3.0"); // 3CH
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".30.4.0"); // 4CH
		
		// 하콘
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".60.1.0"); // 1번 자체온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.1.0"); // 1번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".61.1.0"); // 2번 자체온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.2.0"); // 2번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".62.1.0"); // 3번 자체온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.3.0"); // 3번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".63.1.0"); // 4번 자체온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.4.0"); // 4번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".64.1.0"); // 5번 자체온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.5.0"); // 5번 통신상태
		
		// 티센서(무선)
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".70.1.0"); // 개수
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".70.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".70.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".70.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".70.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".71.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".71.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".71.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".71.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".72.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".72.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".72.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".72.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".73.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".73.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".73.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".73.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".74.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".74.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".74.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".74.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".75.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".75.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".75.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".75.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".76.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".76.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".76.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".76.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".77.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".77.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".77.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".77.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".78.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".78.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".78.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".78.5.0"); // 배터리잔량
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".79.2.0"); // 구분
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".79.3.0"); // 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".79.4.0"); // 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".79.5.0"); // 배터리잔량
		
		// 티센서(유선)
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.6.0");  // 1번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".50.1.0");  // 1번 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".50.2.0");  // 1번 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.7.0");  // 2번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".51.1.0");  // 2번 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".51.2.0");  // 2번 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.8.0");  // 3번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".52.1.0");  // 3번 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".52.2.0");  // 3번 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.9.0");  // 4번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".53.1.0");  // 4번 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".53.2.0");  // 4번 습도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".80.10.0"); // 5번 통신상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".54.1.0");  // 5번 온도
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".54.2.0");  // 5번 습도
		
		// 간판상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".20.2.0"); // 1번 상태
		this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".20.3.0"); // 2번 상태
		
		// 인버터허브
		for(int i = 0; i < ((36*8)+1); i++){
			this.oidList.add(EnterpriseOIDManager.getEnterpriseOID() + ".90."+(i+1)+".0");
		}
	}
	
	@Override
	public void run() {
		
		try{
			
			String storeCD = DynamicConfUtils.getInstance().getStoreCD();
			
			if(client.isSocketConnected() && !"".equals(storeCD)){
				
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
					
					int idx = 0;
					
					byte[] buffer = new byte[689];
					
					// STX
					buffer[idx++] = 0x02;
					// LEN
			        byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x2AD);
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
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.110.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 1CH 5분 전력사용량
			        	if(ch1M5 > 0){
			        		try{
			        			ch1M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.110.0")) - ch1M5;
			        			temp = ByteUtils.toUnsignedIntBytes(ch1M5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch1M5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        		ch1M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.110.0"));
			        	}
			        	// 1CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.111.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 2CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.112.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 2CH 5분 전력사용량
			        	if(ch2M5 > 0){
			        		try{
				        		ch2M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.112.0")) - ch2M5;
				        		temp = ByteUtils.toUnsignedIntBytes(ch2M5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch2M5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        		ch2M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.112.0"));
			        	}
			        	// 2CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.113.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 3CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.114.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 3CH 5분 전력사용량
			        	if(ch3M5 > 0){
			        		try{
			        			ch3M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.114.0")) - ch3M5;
			        			temp = ByteUtils.toUnsignedIntBytes(ch3M5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch3M5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        		ch3M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.114.0"));
			        	}
			        	// 3CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.115.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 4CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.116.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 4CH 5분 전력사용량
			        	if(ch4M5 > 0){
			        		try{
			        			ch4M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.116.0")) - ch4M5;
			        			temp = ByteUtils.toUnsignedIntBytes(ch4M5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch4M5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        		ch4M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.116.0"));
			        	}
			        	// 4CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.117.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 5CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.118.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 5CH 5분 전력사용량
			        	if(ch5M5 > 0){
			        		try{
			        			ch5M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.118.0")) - ch5M5;
			        			temp = ByteUtils.toUnsignedIntBytes(ch5M5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch5M5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        		ch5M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.118.0"));
			        	}
			        	// 5CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.119.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 6CH 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.120.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 6CH 5분 전력사용량
			        	if(ch6M5 > 0){
			        		try{
				        		ch6M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.120.0")) - ch6M5;
				        		temp = ByteUtils.toUnsignedIntBytes(ch6M5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch6M5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        		ch6M5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.120.0"));
			        	}
			        	// 6CH 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.121.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH R 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH R 5분 전력사용량
			        	if(ch7RM5 > 0){
			        		try{
				        		ch7RM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0")) - ch7RM5;
				        		temp = ByteUtils.toUnsignedIntBytes(ch7RM5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch7RM5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	ch7RM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.7.0"));
			        	}
			        	// 7CH R 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.31.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH S 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.15.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH S 5분 전력사용량
			        	if(ch7SM5 > 0){
			        		try{
			        			ch7SM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.15.0")) - ch7SM5;
			        			temp = ByteUtils.toUnsignedIntBytes(ch7SM5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch7SM5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	ch7SM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.15.0"));
			        	}
			        	// 7CH S 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.39.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH T 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.23.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 7CH T 5분 전력사용량
			        	if(ch7TM5 > 0){
			        		try{
				        		ch7TM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.23.0")) - ch7TM5;
				        		temp = ByteUtils.toUnsignedIntBytes(ch7TM5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch7TM5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	ch7TM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.23.0"));
			        	}
			        	// 7CH T 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.47.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH R 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.8.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH R 5분 전력사용량
			        	if(ch8RM5 > 0){
			        		try{
			        			ch8RM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.8.0")) - ch8RM5;
			        			temp = ByteUtils.toUnsignedIntBytes(ch8RM5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch8RM5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	ch8RM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.8.0"));
			        	}
			        	// 8CH R 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.32.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH S 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH S 5분 전력사용량
			        	if(ch8SM5 > 0){
			        		try{
				        		ch8SM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0")) - ch8SM5;
				        		temp = ByteUtils.toUnsignedIntBytes(ch8SM5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch8SM5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	ch8SM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.16.0"));
			        	}
			        	// 8CH S 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.40.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH T 누적사용량
			        	try{
			        		temp = ByteUtils.toBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.24.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	// 8CH T 5분 전력사용량
			        	if(ch8TM5 > 0){
			        		try{
			        			ch8TM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.24.0")) - ch8TM5;
			        			temp = ByteUtils.toUnsignedIntBytes(ch8TM5);
			        		}catch(Exception e){
			        			temp = ByteUtils.toUnsignedIntBytes(0L);
			        		}
			        		
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}else{
			        		temp = ByteUtils.toUnsignedIntBytes(ch8TM5);
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	ch8TM5 = Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.24.0"));
			        	}
			        	// 8CH T 순간사용량
			        	try{
			        		temp = ByteUtils.toUnsignedIntBytes(Long.parseLong(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".10.48.0")));
			        	}catch(Exception e){
			        		temp = ByteUtils.toUnsignedIntBytes(0L);
			        	}
			        	
			        	for(byte b : temp)
			        		buffer[idx++] = b;
			        	
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
			        		
			        		// 상태
				        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".80."+(i+1)+".0"), (byte)0x00);
				        	
			        		// 온도
				        	try{
					        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid++)+".1.0"));
					        	temp = ByteUtils.toBytes(val);
				        	}catch(Exception e){
				        		temp = ByteUtils.toBytes((short)-9999);
				        	}
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
			        	}
			        	
			        }else{
			        	
			        	for(int i = 0; i < 5; i++){
			        		
			        		// 상태
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
			        		
			        		// 상태
				        	buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + ".80."+(i+6)+".0"), (byte)0x00);
				        	
			        		// 온도
				        	try{
					        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".1.0"));
					        	temp = ByteUtils.toBytes(val);
				        	}catch(Exception e){
				        		temp = ByteUtils.toBytes((short)-9999);
				        	}
				        	
				        	for(byte b : temp)
				        		buffer[idx++] = b;
				        	
				        	// 습도
				        	try{
					        	val = Short.parseShort(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".2.0"));
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
			        		
			        		// 상태
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
			        	
			        	byte bleCnt = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "70.1.0"), (byte)0x00);
			        	
			        	// BLE 개수
		        		buffer[idx++] = bleCnt;
		        		
			        	// 개수가 0보다 크면
			        	if(bleCnt > 0x00){
			        		
			        		short val = 0;
				        	
				        	short doid = 70;
				        	
			        		for(int i = 0; i < bleCnt; i++){
			        			
			        			// 구분
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".2.0"), (byte)0x00);
			        			
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
					        	
					        	// 배터리 잔량
			        			buffer[idx++] = ByteUtils.toByte(dataMap.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(doid)+".5.0"), (byte)0x00);
			        			
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
			        
			        client.write(buffer);
				}
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
}
