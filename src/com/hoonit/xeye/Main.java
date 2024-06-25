package com.hoonit.xeye;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import com.hoonit.xeye.device.DeviceBase;
import com.hoonit.xeye.device.DeviceType;
import com.hoonit.xeye.device.ProtocolType;
import com.hoonit.xeye.event.IPChangeEvent;
import com.hoonit.xeye.event.IPChangeListener;
import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.IFClientProxyManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;
import com.hoonit.xeye.manager.XMLLoadManager;
import com.hoonit.xeye.net.serial.GPIOSerial;
import com.hoonit.xeye.net.serial.Serial;
import com.hoonit.xeye.net.snmp.DataLoadCompleteCheckOID;
import com.hoonit.xeye.net.snmp.DeviceMIB;
import com.hoonit.xeye.net.snmp.EtherGatewayOID;
import com.hoonit.xeye.net.snmp.EtherIPChangeOID;
import com.hoonit.xeye.net.snmp.EtherIPOID;
import com.hoonit.xeye.net.snmp.EtherMacOID;
import com.hoonit.xeye.net.snmp.EtherSubNetMaskOID;
import com.hoonit.xeye.net.snmp.GatewayRestartOID;
import com.hoonit.xeye.net.snmp.GatewayVersionOID;
import com.hoonit.xeye.net.snmp.IOBoardResetOID;
import com.hoonit.xeye.net.snmp.ModuleVersionOID;
import com.hoonit.xeye.net.snmp.StaticMIB;
import com.hoonit.xeye.net.snmp.SystemRebootOID;
import com.hoonit.xeye.net.snmp.SystemTimeOID;
import com.hoonit.xeye.net.snmp.ViewStoreCDOID;
import com.hoonit.xeye.net.snmp.XEYESNMPAgent;
import com.hoonit.xeye.net.socket.IFClient;
import com.hoonit.xeye.net.tcp.TCP;
import com.hoonit.xeye.scheduler.DataSendJob;
import com.hoonit.xeye.scheduler.HACPolicyJob;
import com.hoonit.xeye.scheduler.InformationReqJob;
import com.hoonit.xeye.scheduler.PingCheckJob;
import com.hoonit.xeye.scheduler.PowerPeakPolicyJob;
import com.hoonit.xeye.scheduler.SignBoardJob;
import com.hoonit.xeye.scheduler.TimeSyncJob;
import com.hoonit.xeye.util.DynamicConfUtils;
import com.hoonit.xeye.util.IOBoardUtils;
import com.hoonit.xeye.util.IPUtils;
import com.hoonit.xeye.util.ResourceBundleHandler;

public class Main implements IPChangeListener{

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private final String devicePackageName = "com.hoonit.xeye.device.";
	
	private XEYESNMPAgent xeyeSnmpAgent;
	
	private GPIOSerial gpioSerial;
	
	private Serial serial;
	
	private Thread serialThread;
	
	private TCP tcp;
	
	private static List<Runnable> threadList;
	
	// Gateway 구동여부
	//private boolean isWork = false;
	
	// 고정 MIB OIDs
	private EtherMacOID etherMacOID;
	private EtherIPOID etherIPOID;
	private GatewayVersionOID gwVersionOID;
	private ModuleVersionOID moduleVersionOID;
	private SystemTimeOID systemTimeOID;
	//private ProcessIDOID processIDOID;
	//private HeartBeatOID heartBeatOID;
	//private RestartOID restartOID;
	//private UpdateOID updateOID;
	//private UpdateIPOID updateIPOID;
	//private UpdatePortOID updatePortOID;
	private GatewayRestartOID gatewayRestartOID;
	private SystemRebootOID systemRebootOID;
	private DataLoadCompleteCheckOID dataLoadCompleteCheckOID;
	//private LastUpdateOID lastUpdateOID;
	private ViewStoreCDOID viewStoreCDOID;
	private IOBoardResetOID ioBoardResetOID;
	private EtherSubNetMaskOID etherSubNetMaskOID;
	private EtherGatewayOID etherGatewayOID;
	private EtherIPChangeOID etherIPChangeOID;
	
	// 임시
	//private IOBoardUploadOID ioBoardUploadOID;
	
	private Scheduler scheduler;
	
	public Main() throws Exception {
		
		if(XMLLoadManager.getInstance().getOverlappedList().size() > 0){
			
			List<String> overlappedList = XMLLoadManager.getInstance().getOverlappedList();
			
			for(String info : overlappedList){
				logger.info(info);
			}
			
			throw new Exception("SNMP OID가 중복된게 존재합니다.");
			
		}else{
			
			threadList = new ArrayList<Runnable>();
			
			initSNMPAgent();
			
			initStaticOID();
			
			initDevice();
			
			initScheduler();
			
			initTrigger();
			
			IOBoardUtils.getInstance().init();
			
			initNodeJS();
		}
	}
	
	public void initIFClient(){
		
		logger.info("init IFClient");
		
		IFClient client = new IFClient();
		client.setMain(this);
		IFClientProxyManager.getInstance().setIFClient(client);
		
		client.start();
	}
	
	private void initSNMPAgent() throws Exception {
		
		logger.info("Init SNMP Agent...");
		
		try{
			
			File bootCounterFile = new File("resource/conf/XEYEAgentBC.cfg");
			
			File config = new File("resource/conf/XEYEAgentConfig.cfg");
			
			if(bootCounterFile.exists()){
				bootCounterFile.delete();
			}
			
			if(config.exists()){
				config.delete();
			}
			
			xeyeSnmpAgent = new XEYESNMPAgent(bootCounterFile, config);
	    	xeyeSnmpAgent.doStart();
	    	
	    	SNMPAgentProxyManager.getInstance().setXeyeSnmpAgent(xeyeSnmpAgent);
	    	
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
	
	private void initStaticOID() throws Exception{
		
		logger.info("Init Static MIB...");
		
		try{
			
			StaticMIB staticMIB = new StaticMIB();
			
			List<MOScalar> oidList = new ArrayList<MOScalar>();
			
			// Ethernet Mac
	    	String etherMacOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.1.0";
	    	etherMacOID = new EtherMacOID(new OID(etherMacOIDStr), MOAccessImpl.ACCESS_READ_ONLY);
	    	etherMacOID.setValue(new OctetString(IPUtils.getInstance().getEtherMacAddress()));
			oidList.add(etherMacOID);
			
			// Ethernet IP
	    	String etherIPOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.2.0";
	    	etherIPOID = new EtherIPOID(new OID(etherIPOIDStr), MOAccessImpl.ACCESS_READ_ONLY);
	    	etherIPOID.setValue(new OctetString(IPUtils.getInstance().getEtherIP()));
			oidList.add(etherIPOID);
			
			// G/W Version
			String gwVersionOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.3.0";
			gwVersionOID = new GatewayVersionOID(new OID(gwVersionOIDStr), MOAccessImpl.ACCESS_READ_ONLY);
			gwVersionOID.setValue(new OctetString(DynamicConfUtils.getInstance().getVersion()));
			oidList.add(gwVersionOID);
			
			// Module Version
			String moduleVersionOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.4.0";
			moduleVersionOID = new ModuleVersionOID(new OID(moduleVersionOIDStr), MOAccessImpl.ACCESS_READ_WRITE);
			oidList.add(moduleVersionOID);
			
			// System Time
			String systemTimeOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.5.0";
			systemTimeOID = new SystemTimeOID(new OID(systemTimeOIDStr), MOAccessImpl.ACCESS_READ_WRITE);
			oidList.add(systemTimeOID);
			
			// 프로세스 ID
			//String processIDOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.6.0";
			//processIDOID = new ProcessIDOID(new OID(processIDOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			//oidList.add(processIDOID);
			
			// HeartBeat 일시
			//String heartBeatOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.7.0";
			//heartBeatOID = new HeartBeatOID(new OID(heartBeatOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			//oidList.add(heartBeatOID);
			
			// 재실행 요청
			//String restartOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.8.0";
			//restartOID = new RestartOID(new OID(restartOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			//oidList.add(restartOID);
			
			// 업데이트 요청
			//String updateOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.9.0";
			//updateOID = new UpdateOID(new OID(updateOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			//oidList.add(updateOID);
			
			// 업데이트서버 IP
			//String updateIPOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.10.0";
			//updateIPOID = new UpdateIPOID(new OID(updateIPOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			//oidList.add(updateIPOID);
			
			// 업데이트서버 Port
			//String updatePortOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.11.0";
			//updatePortOID = new UpdatePortOID(new OID(updatePortOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			//oidList.add(updatePortOID);
			
			// Gateway 재시작
			String gatewayRestartOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.12.0";
			gatewayRestartOID = new GatewayRestartOID(new OID(gatewayRestartOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(gatewayRestartOID);
			
			// System 재시작
			String systemRebootOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.13.0";
			systemRebootOID = new SystemRebootOID(new OID(systemRebootOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(systemRebootOID);
			
			// 마지막 업데이트 결과
			//String lastUpdateOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.14.0";
			//lastUpdateOID = new LastUpdateOID(new OID(lastUpdateOIDStr), MOAccessImpl.ACCESS_READ_ONLY);
			//oidList.add(lastUpdateOID);
			
			// View 매장코드
			String viewStoreCDOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.15.0";
			viewStoreCDOID = new ViewStoreCDOID(new OID(viewStoreCDOIDStr), MOAccessImpl.ACCESS_READ_WRITE);
			viewStoreCDOID.setValue(new OctetString(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getViewStoreCD(), "")));
			oidList.add(viewStoreCDOID);
			
			// I/O 보드 리셋
			String ioBoardResetOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.16.0";
			ioBoardResetOID = new IOBoardResetOID(new OID(ioBoardResetOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(ioBoardResetOID);
			
			// Subnet Mask
			String etherSubNetMaskOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.17.0";
			etherSubNetMaskOID = new EtherSubNetMaskOID(new OID(etherSubNetMaskOIDStr), MOAccessImpl.ACCESS_READ_ONLY);
			etherSubNetMaskOID.setValue(new OctetString(IPUtils.getInstance().getSubnetMask()));
			oidList.add(etherSubNetMaskOID);
			
			// Gateway
			String etherGatewayOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.18.0";
			etherGatewayOID = new EtherGatewayOID(new OID(etherGatewayOIDStr), MOAccessImpl.ACCESS_READ_ONLY);
			etherGatewayOID.setValue(new OctetString(IPUtils.getInstance().getGatewayIP()));
			oidList.add(etherGatewayOID);
			
			// IP, Subnet Mask, Gateway 변경
			String etherIPChangeOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.19.0";
			etherIPChangeOID = new EtherIPChangeOID(new OID(etherIPChangeOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			etherIPChangeOID.addIPChangeListener(this);
			oidList.add(etherIPChangeOID);
			
			// 임시(파일업데이트)
			/*String ioBoardUploadOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.98.0";
			ioBoardUploadOID = new IOBoardUploadOID(new OID(ioBoardUploadOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(ioBoardUploadOID);*/
			
			// 데이터 로딩 완료 확인
			String dataLoadCompleteCheckOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.99.0";
			dataLoadCompleteCheckOID = new DataLoadCompleteCheckOID(new OID(dataLoadCompleteCheckOIDStr), MOAccessImpl.ACCESS_READ_WRITE);
			dataLoadCompleteCheckOID.setValue(new OctetString("0")); // 0:로딩중, 1:로딩완료
			oidList.add(dataLoadCompleteCheckOID);
			
			staticMIB.setOIDList(oidList);
			
			xeyeSnmpAgent.registerMIB(staticMIB);
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
	
	private void initDevice() throws Exception {
		
		logger.info("Init Device...");
		
		try{
			
			List<Map<String, Object>> inventoryList = XMLLoadManager.getInstance().getInventoryList();
			
			for(Map<String, Object> inventoryMap : inventoryList){
				
				long readInterval = Long.parseLong((String)inventoryMap.get("READ_INTERVAL")) * 1000L;
				// 1:Serial(GPIO), 2:TCP, 3:Serial(RS232, RS485), 4:BACNet, 5:UDP
				String comType    = (String)inventoryMap.get("COM_TYPE");
				
				String logName           = (String)inventoryMap.get("NAME");
				String logLevel          = ResourceBundleHandler.getInstance().getString("log.level");
				String logMaxFileSize    = ResourceBundleHandler.getInstance().getString("log.maxfilesize");
				String logMaxBackupIndex = ResourceBundleHandler.getInstance().getString("log.maxbackupindex");
				
				Logger logger2 = null;
				
				// Serial(GPIO)일 경우
				if("1".equals(comType)){
					
					if(gpioSerial == null){
						
						gpioSerial = new GPIOSerial();
						gpioSerial.initLog(logName, logLevel, logMaxFileSize, logMaxBackupIndex);
						gpioSerial.setReadInterval(readInterval);
						
						logger2 = gpioSerial.getLogger();
					}
				}
				// TCP일 경우
				else if("2".equals(comType)){
					
					String ip = (String)inventoryMap.get("IP");
					int port  = Integer.parseInt((String)inventoryMap.get("PORT"));
					
					tcp = new TCP(ip, port);
					tcp.initLog(logName, logLevel, logMaxFileSize, logMaxBackupIndex);
					tcp.setReadInterval(readInterval);
					
					logger2 = tcp.getLogger();
					
					threadList.add(tcp);
				}
				// Serial(RS232, RS485)일 경우
				else if("3".equals(comType)){
					
					String portName = (String)inventoryMap.get("PORTNAME");
					int baudRate    = Integer.parseInt((String)inventoryMap.get("BAUDRATE"));
					int dataBits    = Integer.parseInt((String)inventoryMap.get("DATABITS"));
					int stopBits    = Integer.parseInt((String)inventoryMap.get("STOPBITS"));
					String parity   = (String)inventoryMap.get("PARITY");
					
					serial = new Serial(portName, baudRate, dataBits, stopBits, parity);
					serial.initLog(logName, logLevel, logMaxFileSize, logMaxBackupIndex);
					serial.setReceiveTimeout(2000);
					serial.setReadInterval(readInterval);
					
					logger2 = serial.getLogger();
					
					threadList.add(serial);
				}
				
				List<Map<String, Object>> deviceList = (List<Map<String, Object>>)inventoryMap.get("DEVICE");
			
				if(deviceList.size() > 0){
					
					for(Map<String, Object> deviceMap : deviceList){
						
						int deviceType    = Integer.parseInt((String)deviceMap.get("TYPE"));
						int protocol      = Integer.parseInt((String)deviceMap.get("PROTOCOL"));
						String useYN      = (String)deviceMap.get("USE_YN");
						String deviceName = (String)deviceMap.get("NAME");
						String objectName = (String)deviceMap.get("OBJECT");
						
						// Digital Output이면
						if(deviceType == DeviceType.DO){
							
							String oid  = (String)deviceMap.get("OID");
							List<Map<String, Object>> tagList = (List<Map<String, Object>>)deviceMap.get("TAG");
							
							Class clz = Class.forName(devicePackageName + objectName);
							Class[] constructParameterTypes = {String.class, int.class, int.class, String.class, java.util.List.class};
							Constructor constructor = clz.getDeclaredConstructor(constructParameterTypes);
							Object obj = constructor.newInstance(deviceName, protocol, deviceType, oid, tagList);
							
							if(obj instanceof DeviceBase){
								
								DeviceBase device = (DeviceBase)obj;
								device.setLogger(logger2);
								device.setTransObject(gpioSerial);
								
								DeviceMIB deviceMIB = device.getMIB();
								
								if(deviceMIB != null){
									xeyeSnmpAgent.registerMIB(deviceMIB);
									deviceMIB.addSetListener(device);
								}
							}
							
						}else{
							
							// Protocol이 Modbus가 아닐 경우
							if(protocol == ProtocolType.NON_MODBUS){
								
								// Serial(GPIO) 일 경우
								if("1".equals(comType)){
									
									String oid  = (String)deviceMap.get("OID");
									List<Map<String, Object>> tagList = (List<Map<String, Object>>)deviceMap.get("TAG");
									
									Class clz = Class.forName(devicePackageName + objectName);
									Class[] constructParameterTypes = {String.class, int.class, int.class, String.class, java.util.List.class};
									Constructor constructor = clz.getDeclaredConstructor(constructParameterTypes);
									Object obj = constructor.newInstance(deviceName, protocol, deviceType, oid, tagList);
									
									if(obj instanceof DeviceBase){
									
										DeviceBase device = (DeviceBase)obj;
										device.setLogger(logger2);
										device.setTransObject(gpioSerial);
										
										DeviceMIB deviceMIB = device.getMIB();
										
										if(deviceMIB != null){
											xeyeSnmpAgent.registerMIB(deviceMIB);
											
											// 장비가 사용으로 설정되어 있으면
											if("Y".equals(useYN)){
												deviceMIB.addSetListener(device);
												gpioSerial.setDevice(device);
											}
										}
									}
								}
								// 기타
								else {
									
									if("2".equals(comType) || "3".equals(comType) || "5".equals(comType)){
										
										String oid                        = (String)deviceMap.get("OID");
										List<Map<String, Object>> tagList = (List<Map<String, Object>>)deviceMap.get("TAG");
										
										Class clz = Class.forName(devicePackageName + objectName);
										//Class[] constructParameterTypes = {String.class, int.class, int.class, String.class, String.class, int.class, int.class, java.util.List.class};
										Class[] constructParameterTypes = {String.class, int.class, int.class, String.class, java.util.List.class};
										Constructor constructor = clz.getDeclaredConstructor(constructParameterTypes);
										Object obj = constructor.newInstance(deviceName, protocol, deviceType, oid, tagList);
										
										if(obj instanceof DeviceBase){
										
											DeviceBase device = (DeviceBase)obj;
											device.setLogger(logger2);
											
											// TCP
											if("2".equals(comType)){
												device.setTransObject(tcp);
											}
											// Serial
											else if("3".equals(comType)){
												device.setTransObject(serial);
											}
											
											DeviceMIB deviceMIB = device.getMIB();
											
											if(deviceMIB != null){
												xeyeSnmpAgent.registerMIB(deviceMIB);
												
												// 장비가 사용으로 설정되어 있으면
												if("Y".equals(useYN)){
													deviceMIB.addSetListener(device);
													
													// TCP
													if("2".equals(comType)){
														tcp.setDevice(device);
													}
													// Serial
													else if("3".equals(comType)){
														serial.setDevice(device);
													}
												}
											}
										}
									}
								}
								
							}else{
								
								String oid      = (String)deviceMap.get("OID");
								int unitID      = Integer.parseInt((String)deviceMap.get("UNIT_ID"));
								String channel  = (String)deviceMap.get("CHANNEL");
								String baudRate = (String)deviceMap.get("BAUDRATE");
								List<Map<String, Object>> tagList = (List<Map<String, Object>>)deviceMap.get("TAG");
								
								Class clz = Class.forName(devicePackageName + objectName);
								Class[] constructParameterTypes = {String.class, int.class, int.class, String.class, int.class, String.class, String.class, java.util.List.class};
								Constructor constructor = clz.getDeclaredConstructor(constructParameterTypes);
								Object obj = constructor.newInstance(deviceName, protocol, deviceType, oid, unitID, channel, baudRate, tagList);
								
								if(obj instanceof DeviceBase){
								
									DeviceBase device = (DeviceBase)obj;
									device.setLogger(logger2);
									
									// Serial(GPIO) 일 경우
									if("1".equals(comType)){
										device.setTransObject(gpioSerial);
									}
									// TCP 일 경우
									else if("2".equals(comType)){
										device.setTransObject(tcp);
									}
									// Serial(RS232, RS485) 일 경우
									else if("3".equals(comType)){
										device.setTransObject(serial);
									}
									
									DeviceMIB deviceMIB = device.getMIB();
									
									if(deviceMIB != null){
										xeyeSnmpAgent.registerMIB(deviceMIB);
										
										// 장비가 사용으로 설정되어 있으면
										if("Y".equals(useYN)){
											
											deviceMIB.addSetListener(device);
											
											// Serial(GPIO) 일 경우
											if("1".equals(comType)){
												gpioSerial.setDevice(device);
											}
											// TCP 일 경우
											else if("2".equals(comType)){
												tcp.setDevice(device);
											}
											// Serial(RS232, RS485) 일 경우
											else if("3".equals(comType)){
												serial.setDevice(device);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
	
	private void initScheduler() throws Exception {
		
		scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();
	}
	
	private void initTrigger() throws Exception {
		
		// 냉난방정책
	    DynamicConfUtils.getInstance().setHaconControl(false);
	    // 피크발생여부
	    DynamicConfUtils.getInstance().setPowerPeak(false);
	    // 전력피크 경과시간
	    DynamicConfUtils.getInstance().setPowerPeakTime(0L);
	    
	    // Ping
	    if(!"".equals(ResourceBundleHandler.getInstance().getString("cron.pingcheck.expression").trim())){
	    
		    JobDetail pingCheckPolicyJob = JobBuilder
		    		.newJob(PingCheckJob.class)
		    		.withIdentity("pingCheckPolicyJob", "group1")
		    		.build();
		    Trigger pingCheckTrigger = TriggerBuilder
		    		.newTrigger()
		    		.withIdentity("pingCheckTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.pingcheck.expression"))).build();
	    
		    scheduler.scheduleJob(pingCheckPolicyJob, pingCheckTrigger);
	    }
	    
	    // 전력피크
	    if(!"".equals(ResourceBundleHandler.getInstance().getString("cron.powerpeak.expression").trim())){
	    	
		    JobDetail powerPeakPolicyJob = JobBuilder
		    		.newJob(PowerPeakPolicyJob.class)
		    		.withIdentity("powerPeakPolicyJob", "group1")
		    		.build();
		    Trigger powerPeakPolicyTrigger = TriggerBuilder
		    		.newTrigger()
		    		.withIdentity("powerPeakPolicyTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.powerpeak.expression"))).build();
		    
		    scheduler.scheduleJob(powerPeakPolicyJob, powerPeakPolicyTrigger);
	    }
	    
		// 간판
	    if(!"".equals(ResourceBundleHandler.getInstance().getString("cron.signboard.expression").trim())){
	    	
			JobDetail signBoardJob = JobBuilder
		    		.newJob(SignBoardJob.class)
		    		.withIdentity("signBoardJob", "group1")
		    		.build();
		    Trigger signBoardTrigger = TriggerBuilder
		    		.newTrigger()
		    		.withIdentity("signBoardTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.signboard.expression"))).build();
		    
		    scheduler.scheduleJob(signBoardJob, signBoardTrigger);
	    }
	    
	    // 냉난방정책
	    if(!"".equals(ResourceBundleHandler.getInstance().getString("cron.hacpolicy.expression"))){
	    	
		    JobDetail hacPolicyJob = JobBuilder
		    		.newJob(HACPolicyJob.class)
		    		.withIdentity("hacPolicyJob", "group1")
		    		.build();
		    Trigger hacPolicyTrigger = TriggerBuilder
		    		.newTrigger()
		    		.withIdentity("hacPolicyTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.hacpolicy.expression"))).build();
	    
		    scheduler.scheduleJob(hacPolicyJob, hacPolicyTrigger);
	    }
	    
	    // 일출물시간, 날씨, 냉난방, 권장온도
	    if(!"".equals(ResourceBundleHandler.getInstance().getString("cron.informationreq.expression").trim())){
	    	
		    JobDetail informationReqJob = JobBuilder
		    		.newJob(InformationReqJob.class)
		    		.withIdentity("informationReqJob", "group1")
		    		.build();
		    Trigger informationReqTrigger = TriggerBuilder
		    		.newTrigger()
		    		.withIdentity("informationReqTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.informationreq.expression"))).build();
	    
		    scheduler.scheduleJob(informationReqJob, informationReqTrigger);
	    }
	    
	    // 시간동기화
	    if(!"".equals(ResourceBundleHandler.getInstance().getString("cron.timesync.expression").trim())){
	    	
		    JobDetail timeSyncJob = JobBuilder
		    		.newJob(TimeSyncJob.class)
		    		.withIdentity("timeSyncJob", "group1")
		    		.build();
		    Trigger timeSyncTrigger = TriggerBuilder
		    		.newTrigger()
		    		.withIdentity("timeSyncTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.timesync.expression"))).build();
	    
		    scheduler.scheduleJob(timeSyncJob, timeSyncTrigger);
	    }
	    
	    // 데이터전송
	    if(!"".equals(ResourceBundleHandler.getInstance().getString("cron.datasend.expression").trim())){
	    	
		    JobDetail dataSendJob = JobBuilder
		    		.newJob(DataSendJob.class)
		    		.withIdentity("dataSendJob", "group1")
		    		.build();
		    Trigger dataSendTrigger = TriggerBuilder
		    		.newTrigger()
		    		.withIdentity("dataSendTrigger", "group1")
		    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.datasend.expression"))).build();
	    
		    scheduler.scheduleJob(dataSendJob, dataSendTrigger);
	    }
	}
	
	public void initNodeJS(){
		
		File nodeDir = new File("node");
		
		if(nodeDir.exists()){
			
			// Node js 구동여부 확인
			Process process   = null;
			BufferedReader br = null;
			BufferedWriter bw = null;
			
			String[] cmd = {"/bin/sh", "-c", "ps ex | grep index.js | grep -v grep | wc -l"};
			
			String line = "";
			String psCnt = "0";
			
			try{
				
				process = Runtime.getRuntime ().exec(cmd);
				process.waitFor();
				
				br = new BufferedReader (new InputStreamReader (process.getInputStream ()));
				
				StringBuffer strbuf = new StringBuffer();
				while ((line = br.readLine ()) != null)
					strbuf.append (line);
				
				psCnt = strbuf.toString();
				
				logger.info("Node.js process cnt="+psCnt);
				
				br.close();
				process.destroy();
				
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}finally{
				try{
					if(br != null) br.close();
					if(bw != null) bw.close();
					if(process != null) process.destroy();
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			// 프로세스가 존재하면
			if(!"0".equals(psCnt)){
				
				String[] cmd2 = {"/bin/sh", "-c", "ps ex | grep index.js | grep -v grep | awk '{print $1}'"};
				
				line = "";
				String ps = "";
				
				try{
					
					process = Runtime.getRuntime ().exec(cmd2);
					process.waitFor();
					
					br = new BufferedReader (new InputStreamReader (process.getInputStream ()));
					
					short cnt = 0;
					while ((line = br.readLine ()) != null){
						if(cnt == 0){
							ps = line;
						}
					}
					
					logger.info("node index.js process id="+ps);
					
					br.close();
					process.destroy();
					
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}finally{
					try{
						if(br != null) br.close();
						if(bw != null) bw.close();
						if(process != null) process.destroy();
					}catch(Exception e){
						logger.error(e.getMessage(), e);
					}
				}
				
				if(!"".equals(ps)){
					
					logger.info("kill node index.js");
					
					String[] cmdKill = {"kill", "-9", ps};
					
					try{
						
						process = Runtime.getRuntime ().exec(cmdKill);
						process.waitFor();
						
						br = new BufferedReader (new InputStreamReader (process.getInputStream ()));
						
						StringBuffer strbuf = new StringBuffer();
						while ((line = br.readLine ()) != null)
							strbuf.append (line);
						
						br.close();
						process.destroy();
						
					}catch(Exception e){
						logger.error(e.getMessage(), e);
					}finally{
						try{
							if(br != null) br.close();
							if(bw != null) bw.close();
							if(process != null) process.destroy();
						}catch(Exception e){
							logger.error(e.getMessage(), e);
						}
					}
				}
				
				logger.info("hciconfig hci0 down...");
				
				String[] cmd3 = {"hciconfig", "hci0", "down"};
				
				try{
					
					process = Runtime.getRuntime ().exec(cmd3);
					process.waitFor();
					
					br = new BufferedReader (new InputStreamReader (process.getInputStream ()));
					
					StringBuffer strbuf = new StringBuffer();
					while ((line = br.readLine ()) != null)
						strbuf.append (line);
					
					br.close();
					process.destroy();
					
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}finally{
					try{
						if(br != null) br.close();
						if(bw != null) bw.close();
						if(process != null) process.destroy();
					}catch(Exception e){
						logger.error(e.getMessage(), e);
					}
				}
			}
			
			logger.info("hciconfig hci0 up...");
			
			String[] cmd2 = {"hciconfig", "hci0", "up"};
			
			try{
				
				process = Runtime.getRuntime ().exec(cmd2);
				process.waitFor();
				
				br = new BufferedReader (new InputStreamReader (process.getInputStream ()));
				
				StringBuffer strbuf = new StringBuffer();
				while ((line = br.readLine ()) != null)
					strbuf.append (line);
				
				br.close();
				process.destroy();
				
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}finally{
				try{
					if(br != null) br.close();
					if(bw != null) bw.close();
					if(process != null) process.destroy();
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			logger.info("node index.js...");
			
			String[] cmd3 = {"node", "/home/xeye/node/index.js", "&"};
			
			try{
				
				process = Runtime.getRuntime ().exec(cmd3);
				/*process.waitFor();
				
				br = new BufferedReader (new InputStreamReader (process.getInputStream ()));
				
				StringBuffer strbuf = new StringBuffer();
				while ((line = br.readLine ()) != null)
					strbuf.append (line);
				
				br.close();
				process.destroy();*/
				
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}finally{
				/*try{
					if(br != null) br.close();
					if(bw != null) bw.close();
					if(process != null) process.destroy();
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}*/
			}
		}
	}
	
	public void pauseScheduler(){
		try{
			scheduler.pauseAll();
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	public void resumeScheduler(){
		try{
			scheduler.resumeAll();
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	// 수집중지
	public boolean doStopCommDevice(){
		
		logger.info("Data communication is stoping...");
		
		try{
			
			if(threadList.size() > 0){
				for(int i = 0; i < threadList.size(); i++){
					if(threadList.get(i) instanceof Serial){
						Serial serial = (Serial)threadList.get(i);
						serial.interrupt();
					}
		        }
			}
			
			threadList.clear();
			
			return true;
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			
			return false;
		}
	}
	
	/*
	// 게이트웨이 재시작
	private void doStartGateway(){
		
		try{
			
			logger.info("Gateway is restarting...");
			
			// 게이트웨이 재시작
			XMLLoadManager.getInstance().init();
			
			initDevice();
			
			doStart();
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	// 게이트웨이 중지
	private boolean doStopGateway(){
		
		logger.info("Gateway is stoping...");
		
		isWork = false;
		
		try{
			
			if(threadList.size() > 0){
				for(int i = 0; i < threadList.size(); i++){
					if(threadList.get(i) instanceof Serial){
						Serial serial = (Serial)threadList.get(i);
						serial.interrupt();
					}
		        }
			}
			
			Thread.sleep(1000);
			
			threadList.clear();
			
			// SNMP Agent를 초기화한다.
			xeyeSnmpAgent.stop();
			
			Thread.sleep(1000);
			
			xeyeSnmpAgent.unregisterDeviceMIB();
			
			Thread.sleep(1000);
			
			xeyeSnmpAgent.run();
			
			logger.info("Gateway is stopped...");
			
			return true;
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			
			return false;
		}
	}*/
	
	public void doStart() throws Exception {
		
		try{
			
			if(gpioSerial != null){
				serialThread = new Thread(gpioSerial);
				serialThread.start();
			}
			
			logger.info("threadList.size()="+threadList.size());
			
			if(threadList.size() > 0){
			
				// Thread start
				ExecutorService executor = Executors.newCachedThreadPool();
		    	
		    	for(Runnable runnable : threadList){
		        	executor.execute(runnable);
		        }
			}else{
				threadList = null;
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw e;
		}
		
		// Gateway 구동여부
		//isWork = true;
	}
	
	// IP 변경
	public boolean notifyIPChange(IPChangeEvent evt){
		
		logger.info("IPChange start...");
		
		logger.info("Address="+evt.getAddress());
		logger.info("Netmask="+evt.getNetmask());
		logger.info("Gateway="+evt.getGateway());
		
		pauseScheduler();
		
		// /etc/network/interface 설정
		boolean result = doSetNetworkInterface(evt.getAddress(), evt.getNetmask(), evt.getGateway());
		
		logger.info("/etc/network/interface 설정 결과="+result);
		
		if(result){
			
			// /etc/dhcpcd.conf 설정
			result = doSetDhcpcdConf(evt.getAddress(), evt.getNetmask(), evt.getGateway());
			
			logger.info("/etc/dhcpcd.conf="+result);
			
			if(result){
				
				IPUtils.getInstance().setEtherIP(evt.getAddress());
				IPUtils.getInstance().setSubnetMask(evt.getNetmask());
				IPUtils.getInstance().setGatewayIP(evt.getGateway());
				
				// IP
				etherIPOID.setValue(new OctetString(IPUtils.getInstance().getEtherIP()));
				// Subnet Mask
				etherSubNetMaskOID.setValue(new OctetString(IPUtils.getInstance().getSubnetMask()));
				// Gateway
				etherGatewayOID.setValue(new OctetString(IPUtils.getInstance().getGatewayIP()));
				
				logger.info("IP Setting finished");
				
				// 연결종료 후 재접속함
				IFClientProxyManager.getInstance().getIFClient().stop();
				
				resumeScheduler();
				
				return true;
			}else{
				return false;
			}
			
		}else{
			return false;
		}
	}
	
	// /etc/network/interface 설정
	private boolean doSetNetworkInterface(String address, String netmask, String gateway){
		
		boolean result = true;
		
		Process process   = null;
		BufferedReader br = null;
		BufferedWriter bw = null;
		
		String[] cmd = {"/etc/init.d/networking", "restart"};
		
		try{
			
			File file = new File("/etc/network/interfaces");
			
			br = new BufferedReader(new FileReader(file));
			
			StringBuffer sb = new StringBuffer();
			
			String line = "";
			
			while( (line = br.readLine()) != null ){
				if(line.contains("iface") && line.contains("eth0")){
					
					sb.append("auto eth0").append(System.lineSeparator());
					//sb.append("allow-hotplug eth0").append(System.lineSeparator());
					sb.append("iface eth0 inet static").append(System.lineSeparator());
					
					if(!"".equals(address))
						sb.append("address ").append(address).append(System.lineSeparator());
					
					if(!"".equals(netmask))
						sb.append("netmask ").append(netmask).append(System.lineSeparator());
					
					if(!"".equals(gateway))
						sb.append("gateway ").append(gateway).append(System.lineSeparator());
					
				}else{
					
					if("auto lo".equals(line)){
						sb.append("");
					}else if("iface lo inet loopback".equals(line)){
						sb.append("");
					}else if("auto eth0".equals(line)){
						sb.append("");
					}else if(line.startsWith("address")){
						sb.append("");
					}else if(line.startsWith("netmask")){
						sb.append("");
					}else if(line.startsWith("gateway")){
						sb.append("");
					}else{
						sb.append(line);
						sb.append(System.lineSeparator());
					}
				}
			}
			
			br.close();
			
			FileWriter fw = new FileWriter(file, false);
			bw = new BufferedWriter(fw);
			
			bw.write(sb.toString());
			bw.close();
			
			// 프로세스 실행
			process = Runtime.getRuntime ().exec(cmd);
			process.waitFor();
			
			br = new BufferedReader (new InputStreamReader (process.getInputStream ()));
			
			StringBuffer strbuf = new StringBuffer();
			while ((line = br.readLine ()) != null)
				strbuf.append (line);
			
			logger.info(strbuf.toString());
			
			br.close();
			process.destroy();
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			result = false;
		}finally{
			try{
				if(br != null) br.close();
				if(bw != null) bw.close();
				if(process != null) process.destroy();
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
		
		return result;
	}
	
	// /etc/dhcpcd.conf 설정
	private boolean doSetDhcpcdConf(String address, String netmask, String gateway){
		
		boolean result = true;
		
		Process process   = null;
		BufferedReader br = null;
		BufferedWriter bw = null;
		
		String[] cmd = {"dhcpcd", "-n", "eth0"};
		
		try{
			
			File file = new File("/etc/dhcpcd.conf");
			
			br = new BufferedReader(new FileReader(file));
			
			StringBuffer sb = new StringBuffer();
			
			String line = "";
			
			boolean flag = false;
			
			while( (line = br.readLine()) != null ){
				
				if(line.contains("interface") && line.contains("eth0")){
					sb.append("");
					flag = true;
				}
				
				if(flag){
					if(line.contains("static") && line.contains("ip_address")){
						sb.append("");
					}
					
					if(line.contains("static") && line.contains("routers")){
						sb.append("");
					}
				}else{
					sb.append(line);
					sb.append(System.lineSeparator());
				}
			}
			
			br.close();
			
			String temp = sb.toString().trim();
			
			sb = new StringBuffer();
			sb.append(temp);
			sb.append(System.lineSeparator());
			sb.append(System.lineSeparator());
			sb.append("interface eth0");
			sb.append(System.lineSeparator());
			sb.append("static ip_address=").append(address);
			sb.append(System.lineSeparator());
			sb.append("static routers=").append(gateway);
			
			FileWriter fw = new FileWriter(file, false);
			bw = new BufferedWriter(fw);
			
			bw.write(sb.toString());
			bw.close();
			
			// 프로세스 실행
			process = Runtime.getRuntime ().exec(cmd);
			process.waitFor();
			
			br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			StringBuffer strbuf = new StringBuffer();
			while (( line = br.readLine ()) != null)
				strbuf.append (line);
			
			logger.info(strbuf.toString());
			
			br.close();
			process.destroy();
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			result = false;
		}finally{
			try{
				if(br != null) br.close();
				if(bw != null) bw.close();
				if(process != null) process.destroy();
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
		
		return result;
	}
	
	public static void main(String[] args){
		
		try{
			
			Main m = new Main();
			m.doStart();
			//m.initSPPServer();
			m.initIFClient();
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
