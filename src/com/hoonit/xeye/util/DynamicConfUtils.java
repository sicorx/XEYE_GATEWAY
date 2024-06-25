package com.hoonit.xeye.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;

public class DynamicConfUtils {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static DynamicConfUtils instance = new DynamicConfUtils();
	
	// 위치 정보
	//private String locationName1; // 시/도
	//private String locationName2; // 구/시
	//private String locationName3; // 동/면
	//private String storeName;     // 지점명
	
	// Network 정보
	//private String address;
	//private String netmask;
	//private String gateway;
	
	// IF Server 정보
	private String ifServerHost;
	private int    ifServerPort;
	private int    ifServerSendDelayTime;
	private int    clientBufferSize;
	
	// 버전
	private String version;
	
	// 지점정보
	// 표시지점코드
	private String viewStoreCD;
	// 지점코드
	private String storeCD;
	// GW ID
	private String gwID;
	// 일출시간
	private String sunRiseTime;
	//일출분
	private String sunRiseMinute;
	// 일몰시간
	private String sunSetTime;
	// 일몰분
	private String sunSetMinute;
	// 날씨코드
	private String weatherCD;
	// 외기온도
	private String forecastTemp;
	// 계약전력
	private String contractPower;
	// 냉난방기 제조사
	private String hacManufacture;
	// 냉난방기 월병 정책
	private String hac1MPolicy;
	private String hac2MPolicy;  
	private String hac3MPolicy;  
	private String hac4MPolicy;
	private String hac5MPolicy;
	private String hac6MPolicy;
	private String hac7MPolicy;
	private String hac8MPolicy;
	private String hac9MPolicy;
	private String hac10MPolicy;
	private String hac11MPolicy;
	private String hac12MPolicy;
	// 냉난방기 월별 권장온도
	private String hac1MTemp;
	private String hac2MTemp;
	private String hac3MTemp;
	private String hac4MTemp;
	private String hac5MTemp;
	private String hac6MTemp;
	private String hac7MTemp;
	private String hac8MTemp;
	private String hac9MTemp;
	private String hac10MTemp;
	private String hac11MTemp;
	private String hac12MTemp;
	
	// 하콘제어여부
	private boolean isHaconControl = false;
	
	// 전력피크상태
	private boolean isPowerPeak = false;
	
	// 전력피크 발생시간
	private long PowerPeakTime = 0L;
	
	// 데이터 최초 전송여부
	private boolean isDataSendAtFirst = false;
	
	// CH1 누적전력량
	private long ch1AccPower = 0L;
	
	private DynamicConfUtils(){
		
		// 위치정보 로딩
		//loadLoacation();
		
		// Network정보 로딩
		//loadNetwork();
		
		// IF Server정보 로딩
		loadIFServer();
		
		// 버전 로딩
		loadVersion();
				
		// 지점정보 로딩
		loadStoreInfo();
	}
	
	public static DynamicConfUtils getInstance(){
		
		if(instance == null){
			instance = new DynamicConfUtils();
		}
		
		return instance;
	}
	
	/**
	 * 최초의 CH1 누적전력량을 세팅한다
	 */
	public void loadCH1AccPower(){
		try{
			this.ch1AccPower = Long.parseLong(StringUtils.defaultIfEmpty(SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".10.1.0"), "0"));
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 위치정보 로딩
	 */
	/*private void loadLoacation(){
		
		LinkedProperties prop = new LinkedProperties();
		InputStream is = null;
		
		try {

			File dynamicConfPropFile = new File("resource/conf/location.properties");
			
			if(dynamicConfPropFile.exists()){
				
				is = new FileInputStream(dynamicConfPropFile);
	
				// load a properties file
				prop.load(is);
				
				// 위치정보
				locationName1 = prop.getProperty("location.name1");
				locationName2 = prop.getProperty("location.name2");
				locationName3 = prop.getProperty("location.name3");
				storeName     = prop.getProperty("location.store.name");
	
				logger.info("locationName1 : " + locationName1);
				logger.info("locationName2 : " + locationName2);
				logger.info("locationName3 : " + locationName3);
				logger.info("storeName     : " + storeName);
				
			}else{
				
				locationName1 = "";
				locationName2 = "";
				locationName3 = "";
				storeName     = "";
				
				logger.info("location infomation is not exist.");
			}
			
		} catch (IOException ex) {
			
			logger.error(ex.getMessage(), ex);
			
			locationName1 = "";
			locationName2 = "";
			locationName3 = "";
			storeName     = "";
			
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}*/
	
	/**
	 * 위치 정보 설정
	 */
	/*public boolean doSetLocation(String locationName1, String locationName2, String locationName3, String storeName){
		
		synchronized(this){
			
			logger.info("Location infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			OutputStream os = null;
	
			try {
				
				logger.info("locationName1 : " + locationName1);
				logger.info("locationName2 : " + locationName2);
				logger.info("locationName3 : " + locationName3);
				logger.info("storeName : " + storeName);
	
				File dynamicConfPropFile = new File("resource/conf/location.properties");
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
	
				// set the properties value
				prop.setProperty("location.name1", locationName1);
				prop.setProperty("location.name2", locationName2);
				prop.setProperty("location.name3", locationName3);
				prop.setProperty("location.store.name", storeName);
	
				// save properties
				prop.store(os, null);
				
				result = true;
	
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadLoacation();
			}
			
			return result;
		}
	}*/
	
	/**
	 * Network정보 로딩
	 */
	/*private void loadNetwork(){
		
		LinkedProperties prop = new LinkedProperties();
		InputStream is = null;
		
		try {

			File dynamicConfPropFile = new File("resource/conf/network.properties");
			
			if(dynamicConfPropFile.exists()){
				
				is = new FileInputStream(dynamicConfPropFile);
	
				// load a properties file
				prop.load(is);
				
				// Network 정보
				address = prop.getProperty("address");
				netmask = prop.getProperty("netmask");
				gateway = prop.getProperty("gateway");
	
				// get the property value and print it out
				logger.info("address : " + address);
				logger.info("netmask : " + netmask);
				logger.info("gateway : " + gateway);
				
			}else{
				
				address = "";
				netmask = "";
				gateway = "";
				
				logger.info("Network infomation is not exist.");
			}
			
		} catch (IOException ex) {
			
			logger.error(ex.getMessage(), ex);
			
			address = "";
			netmask = "";
			gateway = "";
			
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}*/
	
	/**
	 * 네트워크 설정
	 */
	/*public boolean doSetNetworkConfig(String address, String netmask, String gateway){
		
		synchronized(this){
			
			logger.info("Network infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			OutputStream os = null;
	
			try {
				
				File dynamicConfPropFile = new File("resource/conf/network.properties");
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
	
				// set the properties value
				prop.setProperty("address", address);
				prop.setProperty("netmask", netmask);
				prop.setProperty("gateway", gateway);
	
				// save properties
				prop.store(os, null);
				
				result = true;
	
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadNetwork();
			}
			
			return result;
		}
	}*/
	
	/**
	 * IF Server정보 로딩
	 */
	private void loadIFServer(){
		
		LinkedProperties prop = new LinkedProperties();
		InputStream is = null;
		
		try {

			File dynamicConfPropFile = new File("resource/conf/ifserver.properties");
			
			if(dynamicConfPropFile.exists()){
				
				is = new FileInputStream(dynamicConfPropFile);
	
				// load a properties file
				prop.load(is);
				
				ifServerHost          = prop.getProperty("server.host");         
				ifServerPort          = Integer.parseInt(prop.getProperty("server.port"));         
				ifServerSendDelayTime = Integer.parseInt(prop.getProperty("server.send.delay.time"));
				clientBufferSize      = Integer.parseInt(prop.getProperty("client.buffer.size"));
				
				logger.info("serverHost : " + ifServerHost);
				logger.info("serverPort : " + ifServerPort);
				logger.info("sendDelay  : " + ifServerSendDelayTime);
				logger.info("bufferSize : " + clientBufferSize);
				
			}else{
				
				ifServerHost          = "";
				ifServerPort          = 0;
				ifServerSendDelayTime = 0;
				clientBufferSize      = 0;
				
				logger.info("IFServer infomation is not exist.");
			}
			
		} catch (IOException ex) {
			
			logger.error(ex.getMessage(), ex);
			
			ifServerHost          = "";
			ifServerPort          = 0;
			ifServerSendDelayTime = 0;
			clientBufferSize      = 0;
			
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * IF Server 정보 설정
	 */
	public boolean doSetIFServer(String ifServerHost, String ifServerPort){
		
		synchronized(this){
			
			logger.info("IFServer infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			InputStream is = null;
			OutputStream os = null;
			
			File dynamicConfPropFile = new File("resource/conf/ifserver.properties");
			
			// 먼저 기존 값을 로딩한다.
			try{
				
				if(dynamicConfPropFile.exists()){
					
					is = new FileInputStream(dynamicConfPropFile);
					
					// load a properties file
					prop.load(is);
					
					Iterator<Object> it = prop.keySet().iterator();
					
					while(it.hasNext()){
						String key   = (String) it.next();
					    String value = (String) prop.getProperty(key);
					    
					    prop.setProperty(key, value);
					}
				}
				
			}catch(IOException e){
				logger.error(e.getMessage(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
			
			try {
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
	
				// set the properties value
				prop.setProperty("server.host", ifServerHost);
				prop.setProperty("server.port", ifServerPort);
	
				// save properties
				prop.store(os, null);
				
				result = true;
	
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadIFServer();
			}
			
			return result;
		}
	}
	
	/**
	 * 버전 로딩
	 */
	public void loadVersion(){
		
		LinkedProperties prop = new LinkedProperties();
		InputStream is = null;
		
		try {

			File propFile = new File("resource/conf/version.properties");
			
			if(propFile.exists()){
				
				is = new FileInputStream(propFile);
	
				// load a properties file
				prop.load(is);
				
				version = prop.getProperty("version");
				
				logger.info("version : " + version);
				
			}else{
				
				version = "";
				
				logger.info("version infomation is not exist.");
			}
			
		} catch (IOException ex) {
			
			logger.error(ex.getMessage(), ex);
			
			version = "";
			
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * 버전 설정
	 */
	public boolean doSetVersion(String version){
		
		synchronized(this){
			
			logger.info("Version infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			OutputStream os = null;
	
			try {
				
				File dynamicConfPropFile = new File("resource/conf/version.properties");
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
	
				// set the properties value
				prop.setProperty("version", version);               
				
				// save properties
				prop.store(os, null);
				
				result = true;
	
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadVersion();
			}
			
			return result;
		}
	}
	
	/**
	 * 지점정보 로딩
	 */
	private void loadStoreInfo(){
		
		LinkedProperties prop = new LinkedProperties();
		InputStream is = null;
		
		try {

			File dynamicConfPropFile = new File("resource/conf/store.properties");
			
			if(dynamicConfPropFile.exists()){
				
				is = new FileInputStream(dynamicConfPropFile);
	
				// load a properties file
				prop.load(is);
				
				viewStoreCD    = prop.getProperty("view.store.cd");
				storeCD        = prop.getProperty("store.cd");
				gwID           = prop.getProperty("gw.id");
				sunRiseTime    = prop.getProperty("sunrise.time");
				sunRiseMinute  = prop.getProperty("sunrise.minute");
				sunSetTime     = prop.getProperty("sunset.time");
				sunSetMinute   = prop.getProperty("sunset.minute");
				weatherCD      = prop.getProperty("weather.cd");
				forecastTemp   = prop.getProperty("forecast.temp");
				contractPower  = prop.getProperty("contract.power");
				hacManufacture = prop.getProperty("hac.manufacture");
				hac1MPolicy    = prop.getProperty("hac.1m.policy");
				hac2MPolicy    = prop.getProperty("hac.2m.policy");
				hac3MPolicy    = prop.getProperty("hac.3m.policy");
				hac4MPolicy    = prop.getProperty("hac.4m.policy");
				hac5MPolicy    = prop.getProperty("hac.5m.policy");
				hac6MPolicy    = prop.getProperty("hac.6m.policy");
				hac7MPolicy    = prop.getProperty("hac.7m.policy");
				hac8MPolicy    = prop.getProperty("hac.8m.policy");
				hac9MPolicy    = prop.getProperty("hac.9m.policy");
				hac10MPolicy   = prop.getProperty("hac.10m.policy");
				hac11MPolicy   = prop.getProperty("hac.11m.policy");
				hac12MPolicy   = prop.getProperty("hac.12m.policy");
				hac1MTemp      = prop.getProperty("hac.1m.temp");
				hac2MTemp 	   = prop.getProperty("hac.2m.temp");
				hac3MTemp 	   = prop.getProperty("hac.3m.temp");
				hac4MTemp 	   = prop.getProperty("hac.4m.temp");
				hac5MTemp 	   = prop.getProperty("hac.5m.temp");
				hac6MTemp 	   = prop.getProperty("hac.6m.temp");
				hac7MTemp 	   = prop.getProperty("hac.7m.temp");
				hac8MTemp 	   = prop.getProperty("hac.8m.temp");
				hac9MTemp 	   = prop.getProperty("hac.9m.temp");
				hac10MTemp 	   = prop.getProperty("hac.10m.temp");
				hac11MTemp 	   = prop.getProperty("hac.11m.temp");
				hac12MTemp 	   = prop.getProperty("hac.12m.temp");
				
				logger.info("viewStoreCD    : " + viewStoreCD);
				logger.info("storeCD        : " + storeCD);
				logger.info("gwID           : " + gwID);
				logger.info("sunRiseTime    : " + sunRiseTime);
				logger.info("sunRiseMinute  : " + sunRiseMinute);
				logger.info("sunSetTime     : " + sunSetTime);
				logger.info("sunSetMinute   : " + sunSetMinute);
				logger.info("weatherCD      : " + weatherCD);
				logger.info("forecastTemp   : " + forecastTemp);
				logger.info("contractPower  : " + contractPower);
				logger.info("hacManufacture : " + hacManufacture);
				logger.info("hac1MPolicy    : " + hac1MPolicy);
				logger.info("hac2MPolicy    : " + hac2MPolicy);
				logger.info("hac3MPolicy    : " + hac3MPolicy);
				logger.info("hac4MPolicy    : " + hac4MPolicy);
				logger.info("hac5MPolicy    : " + hac5MPolicy);
				logger.info("hac6MPolicy    : " + hac6MPolicy);
				logger.info("hac7MPolicy    : " + hac7MPolicy);
				logger.info("hac8MPolicy    : " + hac8MPolicy);
				logger.info("hac9MPolicy    : " + hac9MPolicy);
				logger.info("hac10MPolicy   : " + hac10MPolicy);
				logger.info("hac11MPolicy   : " + hac11MPolicy);
				logger.info("hac12MPolicy   : " + hac12MPolicy);
				logger.info("hac1MTemp      : " + hac1MTemp);
				logger.info("hac2MTemp      : " + hac2MTemp);
				logger.info("hac3MTemp      : " + hac3MTemp);
				logger.info("hac4MTemp      : " + hac4MTemp);
				logger.info("hac5MTemp      : " + hac5MTemp);
				logger.info("hac6MTemp      : " + hac6MTemp);
				logger.info("hac7MTemp      : " + hac7MTemp);
				logger.info("hac8MTemp      : " + hac8MTemp);
				logger.info("hac9MTemp      : " + hac9MTemp);
				logger.info("hac10MTemp     : " + hac10MTemp);
				logger.info("hac11MTemp     : " + hac11MTemp);
				logger.info("hac12MTemp     : " + hac12MTemp);
				
			}else{
				
				viewStoreCD    = "";
				storeCD        = "";
				gwID           = "";
				sunRiseTime    = "";
				sunRiseMinute  = "";
				sunSetTime     = "";
				sunSetMinute   = "";
				weatherCD      = "";
				forecastTemp   = "";
				contractPower  = "";
				hacManufacture = "";
				hac1MPolicy    = "";
				hac2MPolicy    = "";
				hac3MPolicy    = "";
				hac4MPolicy    = "";
				hac5MPolicy    = "";
				hac6MPolicy    = "";
				hac7MPolicy    = "";
				hac8MPolicy    = "";
				hac9MPolicy    = "";
				hac10MPolicy   = "";
				hac11MPolicy   = "";
				hac12MPolicy   = "";
				hac1MTemp      = "";
				hac2MTemp 	   = "";
				hac3MTemp 	   = "";
				hac4MTemp 	   = "";
				hac5MTemp 	   = "";
				hac6MTemp 	   = "";
				hac7MTemp 	   = "";
				hac8MTemp 	   = "";
				hac9MTemp 	   = "";
				hac10MTemp 	   = "";
				hac11MTemp 	   = "";
				hac12MTemp 	   = "";
				
				logger.info("Store code infomation is not exist.");
			}
			
		} catch (IOException ex) {
			
			logger.error(ex.getMessage(), ex);
			
			storeCD = "";
			
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}
	
	/**
	 * 표시지점정보 설정
	 */
	public boolean doSetViewStoreCDInfo(String viewStoreCD){
		
		synchronized(this){
			
			logger.info("View store code infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			InputStream is = null;
			OutputStream os = null;
			
			File dynamicConfPropFile = new File("resource/conf/store.properties");
			
			// 먼저 기존 값을 로딩한다.
			try{
				
				if(dynamicConfPropFile.exists()){
					
					is = new FileInputStream(dynamicConfPropFile);
					
					// load a properties file
					prop.load(is);
					
					Iterator<Object> it = prop.keySet().iterator();
					
					while(it.hasNext()){
						String key   = (String) it.next();
					    String value = (String) prop.getProperty(key);
					    
					    prop.setProperty(key, value);
					}
				}
				
			}catch(IOException e){
				logger.error(e.getMessage(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
	
			try {
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
				
				// 표시지점코드
				if(!"".equals(viewStoreCD))
					prop.setProperty("view.store.cd", viewStoreCD);
				
				result = true;
				
				// save properties
				prop.store(os, null);
				
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadStoreInfo();
			}
			
			return result;
		}
	}
	
	/**
	 * 지점정보 설정
	 */
	public boolean doSetStoreInfo(String storeCD, String gwID){
		
		synchronized(this){
			
			logger.info("Store code infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			InputStream is = null;
			OutputStream os = null;
			
			File dynamicConfPropFile = new File("resource/conf/store.properties");
			
			// 먼저 기존 값을 로딩한다.
			try{
				
				if(dynamicConfPropFile.exists()){
					
					is = new FileInputStream(dynamicConfPropFile);
					
					// load a properties file
					prop.load(is);
					
					Iterator<Object> it = prop.keySet().iterator();
					
					while(it.hasNext()){
						String key   = (String) it.next();
					    String value = (String) prop.getProperty(key);
					    
					    prop.setProperty(key, value);
					}
				}
				
			}catch(IOException e){
				logger.error(e.getMessage(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
	
			try {
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
				
				// 지점코드
				if(!"".equals(storeCD))
					prop.setProperty("store.cd", storeCD);
				
				// GW ID
				if(!"".equals(gwID))
					prop.setProperty("gw.id", gwID);
				
				result = true;
				
				// save properties
				prop.store(os, null);
				
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadStoreInfo();
			}
			
			return result;
		}
	}
	
	/**
	 * 일출/일몰시간 설정
	 */
	public boolean doSetSunRisetInfo(String sunRiseTime,
									 String sunRiseMinute,
									 String sunSetTime,
									 String sunSetMinute,
									 String weatherCD,
									 String forecastTemp){
		
		synchronized(this){
			
			logger.info("SunRise and SunSet code infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			InputStream is = null;
			OutputStream os = null;
	
			File dynamicConfPropFile = new File("resource/conf/store.properties");
			
			// 먼저 기존 값을 로딩한다.
			try{
				
				if(dynamicConfPropFile.exists()){
					
					is = new FileInputStream(dynamicConfPropFile);
					
					// load a properties file
					prop.load(is);
					
					Iterator<Object> it = prop.keySet().iterator();
					
					while(it.hasNext()){
						String key   = (String) it.next();
					    String value = (String) prop.getProperty(key);
					    
					    prop.setProperty(key, value);
					}
				}
				
			}catch(IOException e){
				logger.error(e.getMessage(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
			
			try {
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
				
				if(Integer.parseInt(sunRiseTime) < 255
						&& Integer.parseInt(sunRiseMinute) < 255
						&& Integer.parseInt(sunSetTime) < 255
						&& Integer.parseInt(sunSetMinute) < 255){
					
					// 일출시간
					prop.setProperty("sunrise.time", sunRiseTime);
					// 일출분
					prop.setProperty("sunrise.minute", sunRiseMinute);
					// 일몰시간
					prop.setProperty("sunset.time", sunSetTime);
					// 일몰분
					prop.setProperty("sunset.minute", sunSetMinute);
				}
				
				if(!"0".equals(weatherCD)){
					// 날씨코드
					prop.setProperty("weather.cd", weatherCD);
				}
				
				if(Integer.parseInt(forecastTemp) < 255){
					// 외기온도
					prop.setProperty("forecast.temp", forecastTemp);
				}
				
				result = true;
				
				// save properties
				prop.store(os, null);
				
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadStoreInfo();
			}
			
			return result;
		}
	}
	
	/**
	 * 계약전력
	 */
	public boolean doSetContractPowerInfo(String contractPower){
		
		synchronized(this){
			
			logger.info("Contract Power code infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			InputStream is = null;
			OutputStream os = null;
			
			File dynamicConfPropFile = new File("resource/conf/store.properties");
	
			// 먼저 기존 값을 로딩한다.
			try{
				
				if(dynamicConfPropFile.exists()){
					
					is = new FileInputStream(dynamicConfPropFile);
					
					// load a properties file
					prop.load(is);
					
					Iterator<Object> it = prop.keySet().iterator();
					
					while(it.hasNext()){
						String key   = (String) it.next();
					    String value = (String) prop.getProperty(key);
					    
					    prop.setProperty(key, value);
					}
				}
				
			}catch(IOException e){
				logger.error(e.getMessage(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
			
			try {
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
				
				if(Integer.parseInt(contractPower) > 0){
					
					// 계약전력
					prop.setProperty("contract.power", contractPower);
					
					result = true;
				}
				
				// save properties
				prop.store(os, null);
	
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadStoreInfo();
			}
			
			return result;
		}
	}
	
	/**
	 * 냉난방정책
	 */
	public boolean doSetHACMonthPolicyInfo(String hacManufacture,
			                               String hac1MPolicy,
										   String hac2MPolicy,
										   String hac3MPolicy,
										   String hac4MPolicy,
										   String hac5MPolicy,
										   String hac6MPolicy,
										   String hac7MPolicy,
										   String hac8MPolicy,
										   String hac9MPolicy,
										   String hac10MPolicy,
										   String hac11MPolicy,
										   String hac12MPolicy){
		
		synchronized(this){
			
			logger.info("HAC Monthly Policy code infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			InputStream is = null;
			OutputStream os = null;
			
			File dynamicConfPropFile = new File("resource/conf/store.properties");
	
			// 먼저 기존 값을 로딩한다.
			try{
				
				if(dynamicConfPropFile.exists()){
					
					is = new FileInputStream(dynamicConfPropFile);
					
					// load a properties file
					prop.load(is);
					
					Iterator<Object> it = prop.keySet().iterator();
					
					while(it.hasNext()){
						String key   = (String) it.next();
					    String value = (String) prop.getProperty(key);
					    
					    prop.setProperty(key, value);
					}
				}
				
			}catch(IOException e){
				logger.error(e.getMessage(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
						
			try {
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
				
				// 제조사
				if(Integer.parseInt(StringUtils.defaultIfEmpty(hacManufacture, "255")) < 255){
					prop.setProperty("hac.manufacture", hacManufacture);
				}
				
				// 냉난방정책
				int sum = Integer.parseInt(hac1MPolicy) +
						  Integer.parseInt(hac2MPolicy) +
						  Integer.parseInt(hac3MPolicy) +
						  Integer.parseInt(hac4MPolicy) +
						  Integer.parseInt(hac5MPolicy) +
						  Integer.parseInt(hac6MPolicy) +
						  Integer.parseInt(hac7MPolicy) +
						  Integer.parseInt(hac8MPolicy) +
						  Integer.parseInt(hac9MPolicy) +
						  Integer.parseInt(hac10MPolicy) +
						  Integer.parseInt(hac11MPolicy) +
						  Integer.parseInt(hac12MPolicy);
				
				// 값이 미존재시 255가 넘어오니 전체합이 12개월에 255를 곱한값보다 작으면
				if(sum < (12 * 255)){
					
					// 1월
					prop.setProperty("hac.1m.policy", hac1MPolicy);
					// 2월
					prop.setProperty("hac.2m.policy", hac2MPolicy);
					// 3월
					prop.setProperty("hac.3m.policy", hac3MPolicy);
					// 4월
					prop.setProperty("hac.4m.policy", hac4MPolicy);
					// 5월
					prop.setProperty("hac.5m.policy", hac5MPolicy);
					// 6월
					prop.setProperty("hac.6m.policy", hac6MPolicy);
					// 7월
					prop.setProperty("hac.7m.policy", hac7MPolicy);
					// 8월
					prop.setProperty("hac.8m.policy", hac8MPolicy);
					// 9월
					prop.setProperty("hac.9m.policy", hac9MPolicy);
					// 10월
					prop.setProperty("hac.10m.policy", hac10MPolicy);
					// 11월
					prop.setProperty("hac.11m.policy", hac11MPolicy);
					// 12월
					prop.setProperty("hac.12m.policy", hac12MPolicy);
					
					result = true;
				}
				// save properties
				prop.store(os, null);
	
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadStoreInfo();
			}
			
			return result;
		}
	}
	
	/**
	 * 냉난방 권장온도
	 */
	public boolean doSetHACMonthTempInfo(String hac1MTemp,
										 String hac2MTemp,
										 String hac3MTemp,
										 String hac4MTemp,
										 String hac5MTemp,
										 String hac6MTemp,
										 String hac7MTemp,
										 String hac8MTemp,
										 String hac9MTemp,
										 String hac10MTemp,
										 String hac11MTemp,
										 String hac12MTemp){
		
		synchronized(this){
			
			logger.info("HAC Monthly Temperature code infomation is writing...");
			
			boolean result = false;
			
			LinkedProperties prop = new LinkedProperties();
			InputStream is = null;
			OutputStream os = null;
			
			File dynamicConfPropFile = new File("resource/conf/store.properties");
	
			// 먼저 기존 값을 로딩한다.
			try{
				
				if(dynamicConfPropFile.exists()){
					
					is = new FileInputStream(dynamicConfPropFile);
					
					// load a properties file
					prop.load(is);
					
					Iterator<Object> it = prop.keySet().iterator();
					
					while(it.hasNext()){
						String key   = (String) it.next();
					    String value = (String) prop.getProperty(key);
					    
					    prop.setProperty(key, value);
					}
				}
				
			}catch(IOException e){
				logger.error(e.getMessage(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
						
			try {
				
				if(!dynamicConfPropFile.exists()){
					dynamicConfPropFile.createNewFile();
				}
				
				os = new FileOutputStream(dynamicConfPropFile);
				
				int sum = Integer.parseInt(hac1MTemp) + 
						  Integer.parseInt(hac2MTemp) + 
						  Integer.parseInt(hac3MTemp) + 
						  Integer.parseInt(hac4MTemp) + 
						  Integer.parseInt(hac5MTemp) + 
						  Integer.parseInt(hac6MTemp) + 
						  Integer.parseInt(hac7MTemp) + 
						  Integer.parseInt(hac8MTemp) + 
						  Integer.parseInt(hac9MTemp) + 
						  Integer.parseInt(hac10MTemp) + 
						  Integer.parseInt(hac11MTemp) + 
						  Integer.parseInt(hac12MTemp);
				
				if(sum < (12 * 255)){
					
					// 1월
					prop.setProperty("hac.1m.temp", hac1MTemp);
					// 2월
					prop.setProperty("hac.2m.temp", hac2MTemp);
					// 3월
					prop.setProperty("hac.3m.temp", hac3MTemp);
					// 4월
					prop.setProperty("hac.4m.temp", hac4MTemp);
					// 5월
					prop.setProperty("hac.5m.temp", hac5MTemp);
					// 6월
					prop.setProperty("hac.6m.temp", hac6MTemp);
					// 7월
					prop.setProperty("hac.7m.temp", hac7MTemp);
					// 8월
					prop.setProperty("hac.8m.temp", hac8MTemp);
					// 9월
					prop.setProperty("hac.9m.temp", hac9MTemp);
					// 10월
					prop.setProperty("hac.10m.temp", hac10MTemp);
					// 11월
					prop.setProperty("hac.11m.temp", hac11MTemp);
					// 12월
					prop.setProperty("hac.12m.temp", hac12MTemp);
					
					result = true;
				}
				
				// save properties
				prop.store(os, null);
	
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				
				loadStoreInfo();
			}
			
			return result;
		}
	}
	
	/*public String getLocationName1() {
		return locationName1;
	}

	public String getLocationName2() {
		return locationName2;
	}

	public String getLocationName3() {
		return locationName3;
	}

	public String getStoreName() {
		return storeName;
	}

	public String getAddress() {
		return address;
	}

	public String getNetmask() {
		return netmask;
	}

	public String getGateway() {
		return gateway;
	}*/
	
	public String getIfServerHost() {
		return ifServerHost;
	}

	public void setIfServerHost(String ifServerHost) {
		this.ifServerHost = ifServerHost;
	}

	public int getIfServerPort() {
		return ifServerPort;
	}

	public void setIfServerPort(int ifServerPort) {
		this.ifServerPort = ifServerPort;
	}

	public int getIfServerSendDelayTime() {
		return ifServerSendDelayTime;
	}

	public void setIfServerSendDelayTime(int ifServerSendDelayTime) {
		this.ifServerSendDelayTime = ifServerSendDelayTime;
	}

	public int getClientBufferSize() {
		return clientBufferSize;
	}

	public void setClientBufferSize(int clientBufferSize) {
		this.clientBufferSize = clientBufferSize;
	}
	
	public String getViewStoreCD() {
		return viewStoreCD;
	}

	public void setViewStoreCD(String viewStoreCD) {
		this.viewStoreCD = viewStoreCD;
	}

	public String getStoreCD() {
		return storeCD;
	}

	public void setStoreCD(String storeCD) {
		this.storeCD = storeCD;
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getGwID() {
		return gwID;
	}

	public void setGwID(String gwID) {
		this.gwID = gwID;
	}

	public String getSunRiseTime() {
		return sunRiseTime;
	}

	public void setSunRiseTime(String sunRiseTime) {
		this.sunRiseTime = sunRiseTime;
	}

	public String getSunRiseMinute() {
		return sunRiseMinute;
	}

	public void setSunRiseMinute(String sunRiseMinute) {
		this.sunRiseMinute = sunRiseMinute;
	}

	public String getSunSetTime() {
		return sunSetTime;
	}

	public void setSunSetTime(String sunSetTime) {
		this.sunSetTime = sunSetTime;
	}

	public String getSunSetMinute() {
		return sunSetMinute;
	}

	public void setSunSetMinute(String sunSetMinute) {
		this.sunSetMinute = sunSetMinute;
	}
	
	public String getContractPower() {
		return contractPower;
	}

	public void setContractPower(String contractPower) {
		this.contractPower = contractPower;
	}

	public String getHacManufacture() {
		return hacManufacture;
	}

	public void setHacManufacture(String hacManufacture) {
		this.hacManufacture = hacManufacture;
	}

	public String getHac1MPolicy() {
		return hac1MPolicy;
	}

	public void setHac1MPolicy(String hac1mPolicy) {
		hac1MPolicy = hac1mPolicy;
	}

	public String getHac2MPolicy() {
		return hac2MPolicy;
	}

	public void setHac2MPolicy(String hac2mPolicy) {
		hac2MPolicy = hac2mPolicy;
	}

	public String getHac3MPolicy() {
		return hac3MPolicy;
	}

	public void setHac3MPolicy(String hac3mPolicy) {
		hac3MPolicy = hac3mPolicy;
	}

	public String getHac4MPolicy() {
		return hac4MPolicy;
	}

	public void setHac4MPolicy(String hac4mPolicy) {
		hac4MPolicy = hac4mPolicy;
	}

	public String getHac5MPolicy() {
		return hac5MPolicy;
	}

	public void setHac5MPolicy(String hac5mPolicy) {
		hac5MPolicy = hac5mPolicy;
	}

	public String getHac6MPolicy() {
		return hac6MPolicy;
	}

	public void setHac6MPolicy(String hac6mPolicy) {
		hac6MPolicy = hac6mPolicy;
	}

	public String getHac7MPolicy() {
		return hac7MPolicy;
	}

	public void setHac7MPolicy(String hac7mPolicy) {
		hac7MPolicy = hac7mPolicy;
	}

	public String getHac8MPolicy() {
		return hac8MPolicy;
	}

	public void setHac8MPolicy(String hac8mPolicy) {
		hac8MPolicy = hac8mPolicy;
	}

	public String getHac9MPolicy() {
		return hac9MPolicy;
	}

	public void setHac9MPolicy(String hac9mPolicy) {
		hac9MPolicy = hac9mPolicy;
	}

	public String getHac10MPolicy() {
		return hac10MPolicy;
	}

	public void setHac10MPolicy(String hac10mPolicy) {
		hac10MPolicy = hac10mPolicy;
	}

	public String getHac11MPolicy() {
		return hac11MPolicy;
	}

	public void setHac11MPolicy(String hac11mPolicy) {
		hac11MPolicy = hac11mPolicy;
	}

	public String getHac12MPolicy() {
		return hac12MPolicy;
	}

	public void setHac12MPolicy(String hac12mPolicy) {
		hac12MPolicy = hac12mPolicy;
	}

	public String getHac1MTemp() {
		return hac1MTemp;
	}

	public void setHac1MTemp(String hac1mTemp) {
		hac1MTemp = hac1mTemp;
	}

	public String getHac2MTemp() {
		return hac2MTemp;
	}

	public void setHac2MTemp(String hac2mTemp) {
		hac2MTemp = hac2mTemp;
	}

	public String getHac3MTemp() {
		return hac3MTemp;
	}

	public void setHac3MTemp(String hac3mTemp) {
		hac3MTemp = hac3mTemp;
	}

	public String getHac4MTemp() {
		return hac4MTemp;
	}

	public void setHac4MTemp(String hac4mTemp) {
		hac4MTemp = hac4mTemp;
	}

	public String getHac5MTemp() {
		return hac5MTemp;
	}

	public void setHac5MTemp(String hac5mTemp) {
		hac5MTemp = hac5mTemp;
	}

	public String getHac6MTemp() {
		return hac6MTemp;
	}

	public void setHac6MTemp(String hac6mTemp) {
		hac6MTemp = hac6mTemp;
	}

	public String getHac7MTemp() {
		return hac7MTemp;
	}

	public void setHac7MTemp(String hac7mTemp) {
		hac7MTemp = hac7mTemp;
	}

	public String getHac8MTemp() {
		return hac8MTemp;
	}

	public void setHac8MTemp(String hac8mTemp) {
		hac8MTemp = hac8mTemp;
	}

	public String getHac9MTemp() {
		return hac9MTemp;
	}

	public void setHac9MTemp(String hac9mTemp) {
		hac9MTemp = hac9mTemp;
	}

	public String getHac10MTemp() {
		return hac10MTemp;
	}

	public void setHac10MTemp(String hac10mTemp) {
		hac10MTemp = hac10mTemp;
	}

	public String getHac11MTemp() {
		return hac11MTemp;
	}

	public void setHac11MTemp(String hac11mTemp) {
		hac11MTemp = hac11mTemp;
	}

	public String getHac12MTemp() {
		return hac12MTemp;
	}

	public void setHac12MTemp(String hac12mTemp) {
		hac12MTemp = hac12mTemp;
	}

	public boolean isHaconControl() {
		return isHaconControl;
	}

	public void setHaconControl(boolean isHaconControl) {
		this.isHaconControl = isHaconControl;
	}

	public boolean isPowerPeak() {
		return isPowerPeak;
	}

	public void setPowerPeak(boolean isPowerPeak) {
		this.isPowerPeak = isPowerPeak;
	}

	public boolean isDataSendAtFirst() {
		return isDataSendAtFirst;
	}

	public void setDataSendAtFirst(boolean isDataSendAtFirst) {
		this.isDataSendAtFirst = isDataSendAtFirst;
	}

	public long getCh1AccPower() {
		return ch1AccPower;
	}

	public void setCh1AccPower(long ch1AccPower) {
		this.ch1AccPower = ch1AccPower;
	}

	public long getPowerPeakTime() {
		return PowerPeakTime;
	}

	public void setPowerPeakTime(long powerPeakTime) {
		PowerPeakTime = powerPeakTime;
	}

	public String getWeatherCD() {
		return weatherCD;
	}

	public void setWeatherCD(String weatherCD) {
		this.weatherCD = weatherCD;
	}

	public String getForecastTemp() {
		return forecastTemp;
	}

	public void setForecastTemp(String forecastTemp) {
		this.forecastTemp = forecastTemp;
	}
}
