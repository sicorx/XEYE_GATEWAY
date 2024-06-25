package com.hoonit.xeye.device.vidcom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import com.hoonit.xeye.device.ModbusBase;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.net.NetworkBase;
import com.hoonit.xeye.net.NetworkError;
import com.hoonit.xeye.util.Utils;

public class PMS8CH extends ModbusBase {
	
	private long ch1RAccPwrAmtTotal = 0L;   // Ch1 R 총 누적사용량
	private long ch1SAccPwrAmtTotal = 0L;   // Ch1 S 총 누적사용량
	private long ch1TAccPwrAmtTotal = 0L;   // Ch1 T 총 누적사용량
	private long ch1RAccPwrAmtPre = 0L;     // Ch1 R 이전 누적사용량
	private long ch1SAccPwrAmtPre = 0L;     // Ch1 S 이전 누적사용량
	private long ch1TAccPwrAmtPre = 0L;     // Ch1 T 이전 누적사용량
	private String ch1AccPwrAmtTotal = "0"; // Ch1 R/S/T 누적사용량 
	private String ch1PwrAmtTotal = "0";    // Ch1 R/S/T 순간사용량
	private String ch1CurrentTotal = "0";   // Ch1 R/S/T 전류
	
	private long ch2RAccPwrAmtTotal = 0L;   // Ch2 R 총 누적사용량
	private long ch2SAccPwrAmtTotal = 0L;   // Ch2 S 총 누적사용량
	private long ch2TAccPwrAmtTotal = 0L;   // Ch2 T 총 누적사용량
	private long ch2RAccPwrAmtPre = 0L;     // Ch2 R 이전 누적사용량
	private long ch2SAccPwrAmtPre = 0L;     // Ch2 S 이전 누적사용량
	private long ch2TAccPwrAmtPre = 0L;     // Ch2 T 이전 누적사용량
	private String ch2AccPwrAmtTotal = "0"; // Ch2 R/S/T 누적사용량 
	private String ch2PwrAmtTotal = "0";    // Ch2 R/S/T 순간사용량
	private String ch2CurrentTotal = "0";   // Ch2 R/S/T 전류
	
	private long ch3RAccPwrAmtTotal = 0L;   // Ch3 R 총 누적사용량
	private long ch3SAccPwrAmtTotal = 0L;   // Ch3 S 총 누적사용량
	private long ch3TAccPwrAmtTotal = 0L;   // Ch3 T 총 누적사용량
	private long ch3RAccPwrAmtPre = 0L;     // Ch3 R 이전 누적사용량
	private long ch3SAccPwrAmtPre = 0L;     // Ch3 S 이전 누적사용량
	private long ch3TAccPwrAmtPre = 0L;     // Ch3 T 이전 누적사용량
	private String ch3AccPwrAmtTotal = "0"; // Ch3 R/S/T 누적사용량 
	private String ch3PwrAmtTotal = "0";    // Ch3 R/S/T 순간사용량
	private String ch3CurrentTotal = "0";   // Ch3 R/S/T 전류
	
	private long ch4RAccPwrAmtTotal = 0L;   // Ch4 R 총 누적사용량
	private long ch4SAccPwrAmtTotal = 0L;   // Ch4 S 총 누적사용량
	private long ch4TAccPwrAmtTotal = 0L;   // Ch4 T 총 누적사용량
	private long ch4RAccPwrAmtPre = 0L;     // Ch4 R 이전 누적사용량
	private long ch4SAccPwrAmtPre = 0L;     // Ch4 S 이전 누적사용량
	private long ch4TAccPwrAmtPre = 0L;     // Ch4 T 이전 누적사용량
	private String ch4AccPwrAmtTotal = "0"; // Ch4 R/S/T 누적사용량 
	private String ch4PwrAmtTotal = "0";    // Ch4 R/S/T 순간사용량
	private String ch4CurrentTotal = "0";   // Ch4 R/S/T 전류
	
	private long ch5RAccPwrAmtTotal = 0L;   // Ch5 R 총 누적사용량
	private long ch5SAccPwrAmtTotal = 0L;   // Ch5 S 총 누적사용량
	private long ch5TAccPwrAmtTotal = 0L;   // Ch5 T 총 누적사용량
	private long ch5RAccPwrAmtPre = 0L;     // Ch5 R 이전 누적사용량
	private long ch5SAccPwrAmtPre = 0L;     // Ch5 S 이전 누적사용량
	private long ch5TAccPwrAmtPre = 0L;     // Ch5 T 이전 누적사용량
	private String ch5AccPwrAmtTotal = "0"; // Ch5 R/S/T 누적사용량 
	private String ch5PwrAmtTotal = "0";    // Ch5 R/S/T 순간사용량
	private String ch5CurrentTotal = "0";   // Ch5 R/S/T 전류
	
	private long ch6RAccPwrAmtTotal = 0L;   // Ch6 R 총 누적사용량
	private long ch6SAccPwrAmtTotal = 0L;   // Ch6 S 총 누적사용량
	private long ch6TAccPwrAmtTotal = 0L;   // Ch6 T 총 누적사용량
	private long ch6RAccPwrAmtPre = 0L;     // Ch6 R 이전 누적사용량
	private long ch6SAccPwrAmtPre = 0L;     // Ch6 S 이전 누적사용량
	private long ch6TAccPwrAmtPre = 0L;     // Ch6 T 이전 누적사용량
	private String ch6AccPwrAmtTotal = "0"; // Ch6 R/S/T 누적사용량 
	private String ch6PwrAmtTotal = "0";    // Ch6 R/S/T 순간사용량
	private String ch6CurrentTotal = "0";   // Ch6 R/S/T 전류
	/*
	private String ch7AccPwrAmtTotal = "0"; // Ch7 R/S/T 누적사용량 
	private String ch7PwrAmtTotal = "0";    // Ch7 R/S/T 순간사용량
	
	private String ch8AccPwrAmtTotal = "0"; // Ch8 R/S/T 누적사용량 
	private String ch8PwrAmtTotal = "0";    // Ch8 R/S/T 순간사용량
	*/
	
	// 채널 상별 전체사용량
	private File ch1Tot;
	private File ch2Tot;
	private File ch3Tot;
	private File ch4Tot;
	private File ch5Tot;
	private File ch6Tot;
	
	// 채널 상별 이전사용량
	private File ch1Pre;
	private File ch2Pre;
	private File ch3Pre;
	private File ch4Pre;
	private File ch5Pre;
	private File ch6Pre;
	
	private BufferedReader ch1TotBr;
	private BufferedReader ch2TotBr;
	private BufferedReader ch3TotBr;
	private BufferedReader ch4TotBr;
	private BufferedReader ch5TotBr;
	private BufferedReader ch6TotBr;
	
	private BufferedReader ch1PreBr;
	private BufferedReader ch2PreBr;
	private BufferedReader ch3PreBr;
	private BufferedReader ch4PreBr;
	private BufferedReader ch5PreBr;
	private BufferedReader ch6PreBr;
	
	// 임시(채널1 상별 로그 데이터 저장하고 싶을 시)
	/*private String ch1ROri;
	private String ch1SOri;
	private String ch1TOri;*/
	
	public PMS8CH(String deviceName, int protocol, int deviceType, String deviceOID, int unitID, String channel, String baudRate, List<Map<String, Object>> tagList){
		super(deviceName, protocol, deviceType, deviceOID, unitID, channel, baudRate, tagList);
		
		ch1Tot = new File("resource/conf/ch1_acc_power_total.txt");
		ch2Tot = new File("resource/conf/ch2_acc_power_total.txt");
		ch3Tot = new File("resource/conf/ch3_acc_power_total.txt");
		ch4Tot = new File("resource/conf/ch4_acc_power_total.txt");
		ch5Tot = new File("resource/conf/ch5_acc_power_total.txt");
		ch6Tot = new File("resource/conf/ch6_acc_power_total.txt");
		
		ch1Pre = new File("resource/conf/ch1_acc_power_pre.txt");
		ch2Pre = new File("resource/conf/ch2_acc_power_pre.txt");
		ch3Pre = new File("resource/conf/ch3_acc_power_pre.txt");
		ch4Pre = new File("resource/conf/ch4_acc_power_pre.txt");
		ch5Pre = new File("resource/conf/ch5_acc_power_pre.txt");
		ch6Pre = new File("resource/conf/ch6_acc_power_pre.txt");
		
		try{
			
			if(!ch1Tot.exists())
				ch1Tot.createNewFile();
			
			if(!ch2Tot.exists())
				ch2Tot.createNewFile();
			
			if(!ch3Tot.exists())
				ch3Tot.createNewFile();
			
			if(!ch4Tot.exists())
				ch4Tot.createNewFile();
			
			if(!ch5Tot.exists())
				ch5Tot.createNewFile();
			
			if(!ch6Tot.exists())
				ch6Tot.createNewFile();
			
			
			if(!ch1Pre.exists())
				ch1Pre.createNewFile();
			
			if(!ch2Pre.exists())
				ch2Pre.createNewFile();
			
			if(!ch3Pre.exists())
				ch3Pre.createNewFile();
			
			if(!ch4Pre.exists())
				ch4Pre.createNewFile();
			
			if(!ch5Pre.exists())
				ch5Pre.createNewFile();
			
			if(!ch6Pre.exists())
				ch6Pre.createNewFile();
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public Object responseMessage(byte[] buffer) throws IOException{
		
		byte[] temp = getDataByte(buffer);
		
		if(temp == null){
			// 통신불량
			throw new IOException("Packet is null");
		}else{
			
			byte responseFC = getFunctionCode(buffer);
			
			Map<String, Object> tagGroup = reqTagGroup;
			
			Map<String, String> resultMap = new HashMap<String, String>();
			
			if(responseFC == 3 || responseFC == 4){
				
				int index = 0;
				
				List<Map<String, Object>> tagArr = (List<Map<String, Object>>)tagGroup.get("TAGS");
				
				Map<String, Object> tagMapTemp = tagArr.get(0);
				
				// 처음 주소가 40001이면
				if("40001".equals((String)tagMapTemp.get("ADDRESS"))){
					
					try{
						
						// 파일에서 채널 상별 총 전력사용량을 읽어온다.
						ch1TotBr = new BufferedReader(new FileReader(ch1Tot));  
						ch2TotBr = new BufferedReader(new FileReader(ch2Tot));
						ch3TotBr = new BufferedReader(new FileReader(ch3Tot));
						ch4TotBr = new BufferedReader(new FileReader(ch4Tot));
						ch5TotBr = new BufferedReader(new FileReader(ch5Tot));
						ch6TotBr = new BufferedReader(new FileReader(ch6Tot));
						
						String pwrStr = StringUtils.defaultIfEmpty(ch1TotBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch1RAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch1SAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch1TAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch2TotBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch2RAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch2SAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch2TAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch3TotBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch3RAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch3SAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch3TAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch4TotBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch4RAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch4SAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch4TAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch5TotBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch5RAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch5SAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch5TAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch6TotBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch6RAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch6SAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch6TAccPwrAmtTotal = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						// 파일에서 채널 상별 이전 전력사용량을 읽어온다.
						ch1PreBr = new BufferedReader(new FileReader(ch1Pre));  
						ch2PreBr = new BufferedReader(new FileReader(ch2Pre));
						ch3PreBr = new BufferedReader(new FileReader(ch3Pre));
						ch4PreBr = new BufferedReader(new FileReader(ch4Pre));
						ch5PreBr = new BufferedReader(new FileReader(ch5Pre));
						ch6PreBr = new BufferedReader(new FileReader(ch6Pre));
						
						pwrStr = StringUtils.defaultIfEmpty(ch1PreBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch1RAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch1SAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch1TAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch2PreBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch2RAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch2SAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch2TAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch3PreBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch3RAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch3SAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch3TAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch4PreBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch4RAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch4SAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch4TAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch5PreBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch5RAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch5SAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch5TAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
						pwrStr = StringUtils.defaultIfEmpty(ch6PreBr.readLine(), "");
						
						if(!"".equals(pwrStr)){
							
							StringTokenizer st = new StringTokenizer(pwrStr, ",");
							
							ch6RAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch6SAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
							ch6TAccPwrAmtPre = Long.parseLong(StringUtils.defaultIfEmpty(st.nextToken(), "0"));
						}
						
					}catch(Exception e){
						logger.error(e.getMessage(), e);
					}finally{
						
						if(ch1TotBr != null) ch1TotBr.close();
						if(ch2TotBr != null) ch2TotBr.close();
						if(ch3TotBr != null) ch3TotBr.close();
						if(ch4TotBr != null) ch4TotBr.close();
						if(ch5TotBr != null) ch5TotBr.close();
						if(ch6TotBr != null) ch6TotBr.close();
						
						if(ch1PreBr != null) ch1PreBr.close();
						if(ch2PreBr != null) ch2PreBr.close();
						if(ch3PreBr != null) ch3PreBr.close();
						if(ch4PreBr != null) ch4PreBr.close();
						if(ch5PreBr != null) ch5PreBr.close();
						if(ch6PreBr != null) ch6PreBr.close();
					}
					
					ch1AccPwrAmtTotal = "0";
					ch1PwrAmtTotal = "0";
					ch1CurrentTotal = "0";
					                               
					ch2AccPwrAmtTotal = "0";
					ch2PwrAmtTotal = "0";
					ch2CurrentTotal = "0";
					                               
					ch3AccPwrAmtTotal = "0";
					ch3PwrAmtTotal = "0";
					ch3CurrentTotal = "0";
					                               
					ch4AccPwrAmtTotal = "0";
					ch4PwrAmtTotal = "0";
					ch4CurrentTotal = "0";
					                               
					ch5AccPwrAmtTotal = "0";
					ch5PwrAmtTotal = "0";
					ch5CurrentTotal = "0";
					                               
					ch6AccPwrAmtTotal = "0";
					ch6PwrAmtTotal = "0";
					ch6CurrentTotal = "0";
					                               
					//ch7AccPwrAmtTotal = "0";
					//ch7PwrAmtTotal = "0";   
					                               
					//ch8AccPwrAmtTotal = "0";
					//ch8PwrAmtTotal = "0";   
				}
				
				for(Map<String, Object> tagMap : tagArr){
					
					try{
						
						String oid       = (String)tagMap.get("OID");
						String address   = (String)tagMap.get("ADDRESS");
						String dataType  = (String)tagMap.get("DATA_TYPE");
						String rate      = (String)tagMap.get("RATE");
						String monitorYN = ((String)tagMap.get("MONITOR_YN")).toUpperCase();
						
						if( "Y".equals(monitorYN) ){
							
							byte[] dest = null;
							
							if("INT16".equals(dataType) || "UINT16".equals(dataType)){
								
								dest = new byte[2];
						        System.arraycopy(temp, index, dest, 0, dest.length);
						        
						        index += 2;
						        
							}else if("INT32".equals(dataType)
									|| "UINT32".equals(dataType)
									|| "FLOAT32".equals(dataType)
									|| "SW_FLOAT32".equals(dataType)){
								
								dest = new byte[4];
								
								byte[] destTemp = new byte[4];
								
								System.arraycopy(temp, index, destTemp, 0, destTemp.length);
								
								dest[0] = destTemp[2];
								dest[1] = destTemp[3];
								dest[2] = destTemp[0];
								dest[3] = destTemp[1];
						        
						        index += 4;
							}
							
							String data = getData(dataType, rate, dest);
					        
							if(tagMap.get("BIT_TAG") == null){
								
								// Ch1 R/S/T 누적전력량
								if("40001".equals(address) || "40017".equals(address) || "40033".equals(address)){
									
									// Ch1 R 누적전력량
									if("40001".equals(address)){
										
										// 임시(채널1 상별 로그 데이터 저장하고 싶을 시)
										//ch1ROri = data;
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch1RAccPwrAmtPre){
												ch1RAccPwrAmtTotal += Long.parseLong(data) - ch1RAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch1RAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch1RAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch1RAccPwrAmtTotal);
									}
									// Ch1 S 누적전력량
									else if("40017".equals(address)){
										
										// 임시(채널1 상별 로그 데이터 저장하고 싶을 시)
										//ch1SOri = data;
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch1SAccPwrAmtPre){
												ch1SAccPwrAmtTotal += Long.parseLong(data) - ch1SAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch1SAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch1SAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch1SAccPwrAmtTotal);
									}
									// Ch1 T 누적전력량
									else{
										
										// 임시(채널1 상별 로그 데이터 저장하고 싶을 시)
										//ch1TOri = data;
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch1TAccPwrAmtPre){
												ch1TAccPwrAmtTotal += Long.parseLong(data) - ch1TAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch1TAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch1TAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch1TAccPwrAmtTotal);
									}
									
									BigDecimal bd1 = new BigDecimal(ch1AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch1AccPwrAmtTotal = bd1.add(bd2).toString();
									
								}
								// Ch2 R/S/T 누적전력량
								else if("40003".equals(address) || "40019".equals(address) || "40035".equals(address)){
									
									// Ch2 R 누적전력량
									if("40003".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch2RAccPwrAmtPre){
												ch2RAccPwrAmtTotal += Long.parseLong(data) - ch2RAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch2RAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch2RAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch2RAccPwrAmtTotal);
									}
									// Ch2 S 누적전력량
									else if("40019".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch2SAccPwrAmtPre){
												ch2SAccPwrAmtTotal += Long.parseLong(data) - ch2SAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch2SAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch2SAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch2SAccPwrAmtTotal);
									}
									// Ch2 T 누적전력량
									else{
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch2TAccPwrAmtPre){
												ch2TAccPwrAmtTotal += Long.parseLong(data) - ch2TAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch2TAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch2TAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch2TAccPwrAmtTotal);
									}
									
									BigDecimal bd1 = new BigDecimal(ch2AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch2AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch3 R/S/T 누적전력량
								else if("40005".equals(address) || "40021".equals(address) || "40037".equals(address)){
									
									// Ch3 R 누적전력량
									if("40005".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
										
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch3RAccPwrAmtPre){
												ch3RAccPwrAmtTotal += Long.parseLong(data) - ch3RAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch3RAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch3RAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch3RAccPwrAmtTotal);
									}
									// Ch3 S 누적전력량
									else if("40021".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch3SAccPwrAmtPre){
												ch3SAccPwrAmtTotal += Long.parseLong(data) - ch3SAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch3SAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch3SAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch3SAccPwrAmtTotal);
									}
									// Ch3 T 누적전력량
									else{
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch3TAccPwrAmtPre){
												ch3TAccPwrAmtTotal += Long.parseLong(data) - ch3TAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch3TAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch3TAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch3TAccPwrAmtTotal);
									}
									
									BigDecimal bd1 = new BigDecimal(ch3AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch3AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch4 R/S/T 누적전력량
								else if("40007".equals(address) || "40023".equals(address) || "40039".equals(address)){
									
									// Ch4 R 누적전력량
									if("40007".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch4RAccPwrAmtPre){
												ch4RAccPwrAmtTotal += Long.parseLong(data) - ch4RAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch4RAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch4RAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch4RAccPwrAmtTotal);
									}
									// Ch4 S 누적전력량
									else if("40023".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch4SAccPwrAmtPre){
												ch4SAccPwrAmtTotal += Long.parseLong(data) - ch4SAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch4SAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch4SAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch4SAccPwrAmtTotal);
									}
									// Ch4 T 누적전력량
									else{
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch4TAccPwrAmtPre){
												ch4TAccPwrAmtTotal += Long.parseLong(data) - ch4TAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch4TAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch4TAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch4TAccPwrAmtTotal);
									}
									
									BigDecimal bd1 = new BigDecimal(ch4AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch4AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch5 R/S/T 누적전력량
								else if("40009".equals(address) || "40025".equals(address) || "40041".equals(address)){
									
									// Ch5 R 누적전력량
									if("40009".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
										
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch5RAccPwrAmtPre){
												ch5RAccPwrAmtTotal += Long.parseLong(data) - ch5RAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch5RAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch5RAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch5RAccPwrAmtTotal);
									}
									// Ch5 S 누적전력량
									else if("40025".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch5SAccPwrAmtPre){
												ch5SAccPwrAmtTotal += Long.parseLong(data) - ch5SAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch5SAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch5SAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch5SAccPwrAmtTotal);
									}
									// Ch5 T 누적전력량
									else{
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch5TAccPwrAmtPre){
												ch5TAccPwrAmtTotal += Long.parseLong(data) - ch5TAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch5TAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch5TAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch5TAccPwrAmtTotal);
									}
									
									BigDecimal bd1 = new BigDecimal(ch5AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch5AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch6 R/S/T 누적전력량
								else if("40011".equals(address) || "40027".equals(address) || "40043".equals(address)){
									
									// Ch6 R 누적전력량
									if("40011".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch6RAccPwrAmtPre){
												ch6RAccPwrAmtTotal += Long.parseLong(data) - ch6RAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch6RAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch6RAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch6RAccPwrAmtTotal);
									}
									// Ch6 S 누적전력량
									else if("40027".equals(address)){
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch6SAccPwrAmtPre){
												ch6SAccPwrAmtTotal += Long.parseLong(data) - ch6SAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch6SAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch6SAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch6SAccPwrAmtTotal);
									}
									// Ch6 T 누적전력량
									else{
										
										// 0은 무시
										if(Long.parseLong(data) > 0){
											
											// 현재누적전력량이 이전누적전력량보다 크거나 같으면
											// 총사용전력량 += 현재누적사용량 - 이전누적사용량
											if(Long.parseLong(data) >= ch6TAccPwrAmtPre){
												ch6TAccPwrAmtTotal += Long.parseLong(data) - ch6TAccPwrAmtPre;
											}
											// 현재누적사용량이 이전누적사용량보다 작으면 초기화(리셋) 됐다고 판단
							        		// 초기화 후 최초로 수집된 값을 더해준다.
							        		// 총사용전력량 += 현재누적사용량
											else{
												ch6TAccPwrAmtTotal += Long.parseLong(data);
											}
											
											// 이전누적량을 현재누적량으로 설정 
											ch6TAccPwrAmtPre = Long.parseLong(data);
										}
										
										data = String.valueOf(ch6TAccPwrAmtTotal);
									}
									
									BigDecimal bd1 = new BigDecimal(ch6AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch6AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								/*// Ch7 R/S/T 누적전력량
								else if("40013".equals(address) || "40029".equals(address) || "40045".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch7AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch7AccPwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch8 R/S/T 누적전력량
								else if("40015".equals(address) || "40031".equals(address) || "40047".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch8AccPwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch8AccPwrAmtTotal = bd1.add(bd2).toString();
								}*/
								// Ch1 R/S/T 순간전력량
								else if("40049".equals(address) || "40065".equals(address) || "40081".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch1PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch1PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch2 R/S/T 순간전력량
								else if("40051".equals(address) || "40067".equals(address) || "40083".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch2PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch2PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch3 R/S/T 순간전력량
								else if("40053".equals(address) || "40069".equals(address) || "40085".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch3PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch3PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch4 R/S/T 순간전력량
								else if("40055".equals(address) || "40071".equals(address) || "40087".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch4PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch4PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch5 R/S/T 순간전력량
								else if("40057".equals(address) || "40073".equals(address) || "40089".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch5PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch5PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch6 R/S/T 순간전력량
								else if("40059".equals(address) || "40075".equals(address) || "40091".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch6PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch6PwrAmtTotal = bd1.add(bd2).toString();
								}
								/*// Ch7 R/S/T 순간전력량
								else if("40061".equals(address) || "40077".equals(address) || "40093".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch7PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch7PwrAmtTotal = bd1.add(bd2).toString();
								}
								// Ch8 R/S/T 순간전력량
								else if("40063".equals(address) || "40079".equals(address) || "40095".equals(address)){
										
									BigDecimal bd1 = new BigDecimal(ch8PwrAmtTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch8PwrAmtTotal = bd1.add(bd2).toString();
								}*/
								// CH1 R/S/T 전류
								else if("40097".equals(address) || "40105".equals(address) || "40113".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch1CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch1CurrentTotal = bd1.add(bd2).toString();
								}
								// CH2 R/S/T 전류
								else if("40098".equals(address) || "40106".equals(address) || "40114".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch2CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch2CurrentTotal = bd1.add(bd2).toString();
								}
								// CH3 R/S/T 전류
								else if("40099".equals(address) || "40107".equals(address) || "40115".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch3CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch3CurrentTotal = bd1.add(bd2).toString();
								}
								// CH4 R/S/T 전류
								else if("40100".equals(address) || "40108".equals(address) || "40116".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch4CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch4CurrentTotal = bd1.add(bd2).toString();
								}
								// CH5 R/S/T 전류
								else if("40101".equals(address) || "40109".equals(address) || "40117".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch5CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch5CurrentTotal = bd1.add(bd2).toString();
								}
								// CH6 R/S/T 전류
								else if("40102".equals(address) || "40110".equals(address) || "40118".equals(address)){
									
									BigDecimal bd1 = new BigDecimal(ch5CurrentTotal);
									BigDecimal bd2 = new BigDecimal(data);
									
									ch5CurrentTotal = bd1.add(bd2).toString();
								}
								
								logger.info((String)tagMap.get("NAME") + "(" + (String)tagMap.get("OID") + ")=" + data);
							        
							    // SNMP
							    resultMap.put(deviceOID + "." + oid + ".0", data);
							}else{
								
								int dword = Integer.parseInt(data);
	
								byte[] result = null;
	
								if ("INT32".equals(tagMap.get("DATA_TYPE")) || "UINT32".equals(tagMap.get("DATA_TYPE"))) {
	
									result = new byte[4];
									
									result[0] = (byte) ((dword & 0xFF000000) >> 24);
									result[1] = (byte) ((dword & 0x00FF0000) >> 16);
									result[2] = (byte) ((dword & 0x0000FF00) >> 8);
									result[3] = (byte) ((dword & 0x000000FF) >> 0);
								}else {
	
									result = new byte[2];
	
									result[0] = (byte) ((dword & 0x0000FF00) >> 8);
									result[1] = (byte) ((dword & 0x000000FF) >> 0);
								}
	
								if(result != null){
	
									StringBuffer sb = new StringBuffer();
	
									for(int i = 0; i < result.length; i++){
										sb.append(String.format("%8s", Integer.toBinaryString((result[i] + 256) % 256)).replace(' ', '0'));
									}
	
									data = sb.toString();
								}
								
								short bitCount = 16;
	
								if ("INT32".equals(tagMap.get("DATA_TYPE")) || "UINT32".equals(tagMap.get("DATA_TYPE"))) {
									bitCount = 32;
								}
								
								StringBuffer sb = new StringBuffer();
	
								if(data.length() == bitCount){
									for(short i = 0; i < bitCount; i+=8){
										sb.insert(0, new StringBuffer().append(data.substring(i, i+8)).reverse());
									}
								}
	
								data = sb.toString();
								
								char[] achar = data.toCharArray();
								
								List<Map<String, Object>> bitTagList = (List<Map<String, Object>>)tagMap.get("BIT_TAG");
								
								short idx = 0;
								
								for(Map<String, Object> bitTagMap : bitTagList){
									
									if("Y".equals( ((String)bitTagMap.get("MONITOR_YN")).toUpperCase() )){
										
										char val = achar[idx++];
										
										logger.info((String)bitTagMap.get("NAME") + "(" + (String)bitTagMap.get("OID") + ")=" + String.valueOf(val));
								        
								        // SNMP
								        resultMap.put(deviceOID + "." + (String)bitTagMap.get("OID") + ".0", String.valueOf(val));
									}else{
										idx++;
									}
								}
							}
						}
					}catch(Exception e){
						logger.error(e.getMessage(), e);
					}
				} // end for
				
				Map<String, Object> tagMap = tagArr.get(0);
				
				// 처음 주소가 40001이면
				if("40001".equals((String)tagMap.get("ADDRESS"))){
					
					// Ch1 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH1 전체 누적전력량(1)=" + ch1AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".1.0", ch1AccPwrAmtTotal);
				    
				    // Ch1 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH1 전체 순간전력량(2)=" + ch1PwrAmtTotal);
				    resultMap.put(deviceOID + ".2.0", ch1PwrAmtTotal);
				    
				    // Ch1 R/S/T 전류 SNMP 갱신
					logger.info("CH1 전체 전류(3)=" + ch1CurrentTotal);
				    resultMap.put(deviceOID + ".3.0", ch1CurrentTotal);
				    
				    // Ch2 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH2 전체 누적전력량(4)=" + ch2AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".4.0", ch2AccPwrAmtTotal);
				    
				    // Ch2 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH2 전체 순간전력량(5)=" + ch2PwrAmtTotal);
				    resultMap.put(deviceOID + ".5.0", ch2PwrAmtTotal);
				    
				    // Ch2 R/S/T 전류 SNMP 갱신
					logger.info("CH3 전체 전류(6)=" + ch2CurrentTotal);
				    resultMap.put(deviceOID + ".6.0", ch2CurrentTotal);
				    
				    // Ch3 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH3 전체 누적전력량(7)=" + ch3AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".7.0", ch3AccPwrAmtTotal);
				    
				    // Ch3 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH3 전체 순간전력량(8)=" + ch3PwrAmtTotal);
				    resultMap.put(deviceOID + ".8.0", ch3PwrAmtTotal);
				    
				    // Ch3 R/S/T 전류 SNMP 갱신
					logger.info("CH3 전체 전류(9)=" + ch3CurrentTotal);
				    resultMap.put(deviceOID + ".9.0", ch3CurrentTotal);
				    
				    // Ch4 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH4 전체 누적전력량(10)=" + ch4AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".10.0", ch4AccPwrAmtTotal);
				    
				    // Ch4 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH4 전체 순간전력량(11)=" + ch4PwrAmtTotal);
				    resultMap.put(deviceOID + ".11.0", ch4PwrAmtTotal);
				    
				    // Ch4 R/S/T 전류 SNMP 갱신
					logger.info("CH4 전체 전류(12)=" + ch4CurrentTotal);
				    resultMap.put(deviceOID + ".12.0", ch4CurrentTotal);
				    
				    // Ch5 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH5 전체 누적전력량(13)=" + ch5AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".13.0", ch5AccPwrAmtTotal);
				    
				    // Ch5 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH5 전체 순간전력량(14)=" + ch5PwrAmtTotal);
				    resultMap.put(deviceOID + ".14.0", ch5PwrAmtTotal);
				    
				    // Ch5 R/S/T 전류 SNMP 갱신
					logger.info("CH5 전체 전류(15)=" + ch5CurrentTotal);
				    resultMap.put(deviceOID + ".15.0", ch5CurrentTotal);
				    
				    // Ch6 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH6 전체 누적전력량(16)=" + ch6AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".16.0", ch6AccPwrAmtTotal);
				    
				    // Ch6 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH6 전체 순간전력량(17)=" + ch6PwrAmtTotal);
				    resultMap.put(deviceOID + ".17.0", ch6PwrAmtTotal);
				    
				    // Ch6 R/S/T 전류 SNMP 갱신
					logger.info("CH6 전체 전류(18)=" + ch6CurrentTotal);
				    resultMap.put(deviceOID + ".18.0", ch6CurrentTotal);
				    
				    /*
				    // Ch7 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH7 전체 누적전력량(122)=" + ch7AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".122.0", ch7AccPwrAmtTotal);
				    
				    // Ch7 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH7 전체 순간전력량(123)=" + ch7PwrAmtTotal);
				    resultMap.put(deviceOID + ".123.0", ch7PwrAmtTotal);
				    
				    // Ch8 R/S/T 누적전력량 SNMP 갱신
					logger.info("CH8 전체 누적전력량(124)=" + ch8AccPwrAmtTotal);
				    resultMap.put(deviceOID + ".124.0", ch8AccPwrAmtTotal);
				    
				    // Ch8 R/S/T 순간전력량 SNMP 갱신
					logger.info("CH8 전체 순간전력량(125)=" + ch8PwrAmtTotal);
				    resultMap.put(deviceOID + ".125.0", ch8PwrAmtTotal);*/
				    
				    BufferedWriter ch1TotBw = null;
				    BufferedWriter ch2TotBw = null;
				    BufferedWriter ch3TotBw = null;
				    BufferedWriter ch4TotBw = null;
				    BufferedWriter ch5TotBw = null;
				    BufferedWriter ch6TotBw = null;
				    
				    BufferedWriter ch1PreBw = null;
				    BufferedWriter ch2PreBw = null;
				    BufferedWriter ch3PreBw = null;
				    BufferedWriter ch4PreBw = null;
				    BufferedWriter ch5PreBw = null;
				    BufferedWriter ch6PreBw = null;
				    
				    // 임시(채널1 상별 로그 데이터 저장하고 싶을 시)
				    //BufferedWriter ch1OriBw = null;
				    
				    try{
				    	
					    // 채널 총사용량 저장
				    	ch1TotBw = new BufferedWriter(new FileWriter(ch1Tot));
				    	ch2TotBw = new BufferedWriter(new FileWriter(ch2Tot));
				    	ch3TotBw = new BufferedWriter(new FileWriter(ch3Tot));
				    	ch4TotBw = new BufferedWriter(new FileWriter(ch4Tot));
				    	ch5TotBw = new BufferedWriter(new FileWriter(ch5Tot));
				    	ch6TotBw = new BufferedWriter(new FileWriter(ch6Tot));
				        
				        StringBuffer sb = new StringBuffer();
				        sb.append(String.valueOf(ch1RAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch1SAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch1TAccPwrAmtTotal));
				        
				        ch1TotBw.write(sb.toString());
				        ch1TotBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch2RAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch2SAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch2TAccPwrAmtTotal));
				        
				        ch2TotBw.write(sb.toString());
				        ch2TotBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch3RAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch3SAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch3TAccPwrAmtTotal));
				        
				        ch3TotBw.write(sb.toString());
				        ch3TotBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch4RAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch4SAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch4TAccPwrAmtTotal));
				        
				        ch4TotBw.write(sb.toString());
				        ch4TotBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch5RAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch5SAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch5TAccPwrAmtTotal));
				        
				        ch5TotBw.write(sb.toString());
				        ch5TotBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch6RAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch6SAccPwrAmtTotal));
				        sb.append(",");
				        sb.append(String.valueOf(ch6TAccPwrAmtTotal));
				        
				        ch6TotBw.write(sb.toString());
				        ch6TotBw.flush();
				        
				        // 채널 이전 사용량 저장
				    	ch1PreBw = new BufferedWriter(new FileWriter(ch1Pre));
				    	ch2PreBw = new BufferedWriter(new FileWriter(ch2Pre));
				    	ch3PreBw = new BufferedWriter(new FileWriter(ch3Pre));
				    	ch4PreBw = new BufferedWriter(new FileWriter(ch4Pre));
				    	ch5PreBw = new BufferedWriter(new FileWriter(ch5Pre));
				    	ch6PreBw = new BufferedWriter(new FileWriter(ch6Pre));
				    	
				    	sb.setLength(0);
				        sb.append(String.valueOf(ch1RAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch1SAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch1TAccPwrAmtPre));
				        
				        ch1PreBw.write(sb.toString());
				        ch1PreBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch2RAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch2SAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch2TAccPwrAmtPre));
				        
				        ch2PreBw.write(sb.toString());
				        ch2PreBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch3RAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch3SAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch3TAccPwrAmtPre));
				        
				        ch3PreBw.write(sb.toString());
				        ch3PreBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch4RAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch4SAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch4TAccPwrAmtPre));
				        
				        ch4PreBw.write(sb.toString());
				        ch4PreBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch5RAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch5SAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch5TAccPwrAmtPre));
				        
				        ch5PreBw.write(sb.toString());
				        ch5PreBw.flush();
				        
				        sb.setLength(0);
				        sb.append(String.valueOf(ch6RAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch6SAccPwrAmtPre));
				        sb.append(",");
				        sb.append(String.valueOf(ch6TAccPwrAmtPre));
				        
				        ch6PreBw.write(sb.toString());
				        ch6PreBw.flush();
				        
				        // 임시(채널1 상별 로그 데이터 저장하고 싶을 시)
				        /*Calendar cal = Calendar.getInstance();
						int year   = cal.get(Calendar.YEAR);
						int month  = cal.get(Calendar.MONTH) + 1;
						int date   = cal.get(Calendar.DATE);
						int hour   = cal.get(Calendar.HOUR_OF_DAY);
						int minute = cal.get(Calendar.MINUTE);
						int second = cal.get(Calendar.SECOND);
						
				        ch1OriBw = new BufferedWriter(new FileWriter(new File("resource/conf/backup/ch1_log.txt"), true));
				        sb.setLength(0);
				        sb.append(year);
				        sb.append("-");
				        sb.append(month);
				        sb.append("-");
				        sb.append(date);
				        sb.append("-");
				        sb.append(hour);
				        sb.append("-");
				        sb.append(minute);
				        sb.append("-");
				        sb.append(second);
				        sb.append("=");
				        sb.append(ch1ROri);
				        sb.append(",");
				        sb.append(ch1SOri);
				        sb.append(",");
				        sb.append(ch1TOri);
				        sb.append(System.lineSeparator());
				        
				        ch1OriBw.write(sb.toString());
				        ch1OriBw.flush();*/
				        
				    }catch(Exception e){
				    	logger.error(e.getMessage(), e);
				    }finally{
				    	if(ch1TotBw != null) ch1TotBw.close();
				    	if(ch2TotBw != null) ch2TotBw.close();
				    	if(ch3TotBw != null) ch3TotBw.close();
				    	if(ch4TotBw != null) ch4TotBw.close();
				    	if(ch5TotBw != null) ch5TotBw.close();
				    	if(ch6TotBw != null) ch6TotBw.close();
				    	
				    	if(ch1PreBw != null) ch1PreBw.close();
				    	if(ch2PreBw != null) ch2PreBw.close();
				    	if(ch3PreBw != null) ch3PreBw.close();
				    	if(ch4PreBw != null) ch4PreBw.close();
				    	if(ch5PreBw != null) ch5PreBw.close();
				    	if(ch6PreBw != null) ch6PreBw.close();
				    	
				    	// 임시(채널1 상별 로그 데이터 저장하고 싶을 시)
				    	//if(ch1OriBw != null) ch1OriBw.close();
				    }
				}
			}
			
			// 통신불량 - 정상
			Map<String, String> etcResultMap = new HashMap<String, String>();
			etcResultMap.put(deviceOID + ".0.0", NetworkError.NORMAL);
			
			notifyData(resultMap, etcResultMap);
		}
		
		return null;
	}
	
	@Override
	public Object responseMessageForWrite(byte[] buffer) throws IOException, Exception{
		
		String data = "";
		
		byte[] temp = getDataByte(buffer);
		
		if(temp != null){
			
			byte responseFC = getFunctionCode(buffer);
			
			if(responseFC == 3 || responseFC == 4){
				
				String oid       = (String)reqTagMap.get("OID");
				String dataType  = (String)reqTagMap.get("DATA_TYPE");
				String rate      = (String)reqTagMap.get("RATE");
				String monitorYN = ((String)reqTagMap.get("MONITOR_YN")).toUpperCase();
				
				if( "Y".equals(monitorYN) ){
					
					byte[] dest = null;
					
					if("INT16".equals(dataType) || "UINT16".equals(dataType)){
						
						dest = new byte[2];
						System.arraycopy(temp, 0, dest, 0, dest.length);
				        
					}else if("INT32".equals(dataType)
							|| "UINT32".equals(dataType)
							|| "FLOAT32".equals(dataType)
							|| "SW_FLOAT32".equals(dataType)){
						
						dest = new byte[4];
						
						if("SW_FLOAT32".equals(dataType)){
							
							dest[0] = temp[2];
							dest[1] = temp[3];
							dest[2] = temp[0];
							dest[3] = temp[1];
						}else{
							System.arraycopy(temp, 0, dest, 0, dest.length);
						}
					}
					
					data = getData(dataType, rate, dest);
			        
					if(reqTagMap.get("BIT_TAG") != null){
						
						int dword = Integer.parseInt(data);
	
						byte[] result = null;
						
						if ("INT32".equals(reqTagMap.get("DATA_TYPE")) || "UINT32".equals(reqTagMap.get("DATA_TYPE"))) {
	
							result = new byte[4];
							
							result[0] = (byte) ((dword & 0xFF000000) >> 24);
							result[1] = (byte) ((dword & 0x00FF0000) >> 16);
							result[2] = (byte) ((dword & 0x0000FF00) >> 8);
							result[3] = (byte) ((dword & 0x000000FF) >> 0);
						}else {
	
							result = new byte[2];
	
							result[0] = (byte) ((dword & 0x0000FF00) >> 8);
							result[1] = (byte) ((dword & 0x000000FF) >> 0);
						}
	
						if(result != null){
	
							StringBuffer sb = new StringBuffer();
	
							for(int i = 0; i < result.length; i++){
								sb.append(String.format("%8s", Integer.toBinaryString((result[i] + 256) % 256)).replace(' ', '0'));
							}
	
							data = sb.toString();
						}
						
						short bitCount = 16;
	
						if ("INT32".equals(reqTagMap.get("DATA_TYPE")) || "UINT32".equals(reqTagMap.get("DATA_TYPE"))) {
							bitCount = 32;
						}
						
						StringBuffer sb = new StringBuffer();
	
						if(data.length() == bitCount){
							for(short i = 0; i < bitCount; i+=8){
								sb.insert(0, new StringBuffer().append(data.substring(i, i+8)).reverse());
							}
						}
	
						data = sb.toString();
						
						char[] achar = data.toCharArray();
						
						sb.setLength(0);
						
						for(int i = 0; i < achar.length; i++){
							sb.append(String.valueOf(achar[i]));
						}
						
						data = sb.toString();
					}
				}
			}else{
				
				if(responseFC == 2 || responseFC == 1){
					
					StringBuffer bitSB = new StringBuffer();
					StringBuffer tempSB = null;
					
					for (byte b : temp) {
						
						tempSB = new StringBuffer();
						tempSB.append(Utils.convertByteToBit(b));
						
						bitSB.append(tempSB.reverse());
					}
					
					char[] achar = bitSB.toString().toCharArray();
					
					data = String.valueOf(achar[0]);
				}
			}
		}
        
        return data;
	}
	
	/**
	 * SNMP Write
	 */
	public boolean notifySet(SetEvent evt){
		
		boolean result = false;
		
		NetworkBase transObject = (NetworkBase)getTransObject();
		
		if(transObject.tryWriteLock()){
			
			try{
		
				logger.info("=====Control start=====");
				
				String oid 	= evt.getOid();
		    	String data = evt.getData();
		    	
		    	logger.info("oid=" + oid);
		    	logger.info("data=" + data);
		    	
		    	reqTagMap = getReqTagMap(oid);
				
				if(reqTagMap != null){
					
					//recentData = getRecentData(reqTagMap);
					
					String address  = (String)reqTagMap.get("ADDRESS");
					String dataType = (String)reqTagMap.get("DATA_TYPE");
					//String access   = (String)reqTagMap.get("ACCESS");
					String writeData = "";
					
					if("40151".equals(address)){
						
						short bitCnt = 16;
						
						BitSet bs = new BitSet();
						
						if("0".equals(data)){
							
							for(short i = 0; i < bitCnt; i++){
								bs.set(i, false);
							}
							
						}else{
							
							for(short i = 0; i < bitCnt; i++){
								
								if(i < 2){
									bs.set(i, true);
								}else{
									bs.set(i, false);
								}
							}
						}
						
						if(bs.toByteArray() != null){
							
							int newData = 0;
							
							for(byte bt : bs.toByteArray()){
								newData = (newData << 8) + (bt & 0xFF);
							}
							
							writeData = String.valueOf(newData);
						}
					}
					
					if(!"".equals(writeData)){
						
						result = doWriteData(data, recentData, writeData, address, dataType);
						
						try{
							
							Thread.sleep(1000);
							
							execute();
							
						}catch(Exception e){
						}
					}
				}
			}catch(Exception e){
				result = false;
			}finally{
				transObject.unWritelock();
			}
		}
		
		return result;
	}
}
