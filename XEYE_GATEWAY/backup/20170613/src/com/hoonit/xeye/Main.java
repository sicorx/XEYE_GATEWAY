package com.hoonit.xeye;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.hoonit.xeye.net.snmp.HeartBeatOID;
import com.hoonit.xeye.net.snmp.IOBoardResetOID;
import com.hoonit.xeye.net.snmp.IOBoardUploadOID;
import com.hoonit.xeye.net.snmp.LastUpdateOID;
import com.hoonit.xeye.net.snmp.ModuleVersionOID;
import com.hoonit.xeye.net.snmp.ProcessIDOID;
import com.hoonit.xeye.net.snmp.RestartOID;
import com.hoonit.xeye.net.snmp.StaticMIB;
import com.hoonit.xeye.net.snmp.SystemRebootOID;
import com.hoonit.xeye.net.snmp.SystemTimeOID;
import com.hoonit.xeye.net.snmp.UpdateIPOID;
import com.hoonit.xeye.net.snmp.UpdateOID;
import com.hoonit.xeye.net.snmp.UpdatePortOID;
import com.hoonit.xeye.net.snmp.ViewStoreCDOID;
import com.hoonit.xeye.net.snmp.XEYESNMPAgent;
import com.hoonit.xeye.net.socket.IFClient;
import com.hoonit.xeye.net.tcp.TCP;
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

public class Main{

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
	private ProcessIDOID processIDOID;
	private HeartBeatOID heartBeatOID;
	private RestartOID restartOID;
	private UpdateOID updateOID;
	private UpdateIPOID updateIPOID;
	private UpdatePortOID updatePortOID;
	private GatewayRestartOID gatewayRestartOID;
	private SystemRebootOID systemRebootOID;
	private DataLoadCompleteCheckOID dataLoadCompleteCheckOID;
	private LastUpdateOID lastUpdateOID;
	private ViewStoreCDOID viewStoreCDOID;
	private IOBoardResetOID ioBoardResetOID;
	private EtherSubNetMaskOID etherSubNetMaskOID;
	private EtherGatewayOID etherGatewayOID;
	private EtherIPChangeOID etherIPChangeOID;
	
	// 임시
	private IOBoardUploadOID ioBoardUploadOID;
	
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
		}
	}
	
	// Bluetooth 통신 서버 초기화
	/*public void initSPPServer() {
					
		logger.info("Init SPP Server...");
		
		try{
			
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			logger.info("Bluetooth Address: " + localDevice.getBluetoothAddress());
			logger.info("Bluetooth Name: " + localDevice.getFriendlyName());
			
			SPPServer server = new SPPServer();
			server.addBluetoothPacketListener(this);
	        
	        while (true) {  
	            Session session = server.accept();
	            new Thread(session).start();
	        }
	        
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}*/
	
	public void initIFClient(){
		
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
			String processIDOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.6.0";
			processIDOID = new ProcessIDOID(new OID(processIDOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(processIDOID);
			
			// HeartBeat 일시
			String heartBeatOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.7.0";
			heartBeatOID = new HeartBeatOID(new OID(heartBeatOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(heartBeatOID);
			
			// 재실행 요청
			String restartOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.8.0";
			restartOID = new RestartOID(new OID(restartOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(restartOID);
			
			// 업데이트 요청
			String updateOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.9.0";
			updateOID = new UpdateOID(new OID(updateOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(updateOID);
			
			// 업데이트서버 IP
			String updateIPOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.10.0";
			updateIPOID = new UpdateIPOID(new OID(updateIPOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(updateIPOID);
			
			// 업데이트서버 Port
			String updatePortOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.11.0";
			updatePortOID = new UpdatePortOID(new OID(updatePortOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(updatePortOID);
			
			// Gateway 재시작
			String gatewayRestartOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.12.0";
			gatewayRestartOID = new GatewayRestartOID(new OID(gatewayRestartOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(gatewayRestartOID);
			
			// System 재시작
			String systemRebootOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.13.0";
			systemRebootOID = new SystemRebootOID(new OID(systemRebootOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(systemRebootOID);
			
			// 마지막 업데이트 결과
			String lastUpdateOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.14.0";
			lastUpdateOID = new LastUpdateOID(new OID(lastUpdateOIDStr), MOAccessImpl.ACCESS_READ_ONLY);
			oidList.add(lastUpdateOID);
			
			// View 매장코드
			String viewStoreCDOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.15.0";
			viewStoreCDOID = new ViewStoreCDOID(new OID(viewStoreCDOIDStr), MOAccessImpl.ACCESS_READ_WRITE);
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
			oidList.add(etherIPChangeOID);
			
			// 임시(파일업데이트)
			String ioBoardUploadOIDStr = EnterpriseOIDManager.getEnterpriseOID() + ".1.98.0";
			ioBoardUploadOID = new IOBoardUploadOID(new OID(ioBoardUploadOIDStr), MOAccessImpl.ACCESS_WRITE_ONLY);
			oidList.add(ioBoardUploadOID);
			
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
	    JobDetail pingCheckPolicyJob = JobBuilder
	    		.newJob(PingCheckJob.class)
	    		.withIdentity("pingCheckPolicyJob", "group1")
	    		.build();
	    Trigger pingCheckTrigger = TriggerBuilder
	    		.newTrigger()
	    		.withIdentity("pingCheckTrigger", "group1")
	    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.pingcheck.expression"))).build();
	    
	    // 전력피크
	    JobDetail powerPeakPolicyJob = JobBuilder
	    		.newJob(PowerPeakPolicyJob.class)
	    		.withIdentity("powerPeakPolicyJob", "group1")
	    		.build();
	    Trigger powerPeakPolicyTrigger = TriggerBuilder
	    		.newTrigger()
	    		.withIdentity("powerPeakPolicyTrigger", "group1")
	    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.powerpeak.expression"))).build();
	    
		// 간판
		JobDetail signBoardJob = JobBuilder
	    		.newJob(SignBoardJob.class)
	    		.withIdentity("signBoardJob", "group1")
	    		.build();
	    Trigger signBoardTrigger = TriggerBuilder
	    		.newTrigger()
	    		.withIdentity("signBoardTrigger", "group1")
	    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.signboard.expression"))).build();
	    
	    // 냉난방정책
	    JobDetail hacPolicyJob = JobBuilder
	    		.newJob(HACPolicyJob.class)
	    		.withIdentity("hacPolicyJob", "group1")
	    		.build();
	    Trigger hacPolicyTrigger = TriggerBuilder
	    		.newTrigger()
	    		.withIdentity("hacPolicyTrigger", "group1")
	    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.hacpolicy.expression"))).build();
	    
	    // 일출물시간, 날씨, 냉난방, 권장온도
	    JobDetail informationReqJob = JobBuilder
	    		.newJob(InformationReqJob.class)
	    		.withIdentity("informationReqJob", "group1")
	    		.build();
	    Trigger informationReqTrigger = TriggerBuilder
	    		.newTrigger()
	    		.withIdentity("informationReqTrigger", "group1")
	    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.informationreq.expression"))).build();
	    
	    // 시간동기화
	    JobDetail timeSyncJob = JobBuilder
	    		.newJob(TimeSyncJob.class)
	    		.withIdentity("timeSyncJob", "group1")
	    		.build();
	    Trigger timeSyncTrigger = TriggerBuilder
	    		.newTrigger()
	    		.withIdentity("timeSyncTrigger", "group1")
	    		.withSchedule(CronScheduleBuilder.cronSchedule(ResourceBundleHandler.getInstance().getString("cron.timesync.expression"))).build();
	    
	    
	    scheduler.scheduleJob(pingCheckPolicyJob, pingCheckTrigger);
	    scheduler.scheduleJob(powerPeakPolicyJob, powerPeakPolicyTrigger);
	    //scheduler.scheduleJob(signBoardJob, signBoardTrigger);
	    scheduler.scheduleJob(hacPolicyJob, hacPolicyTrigger);
	    scheduler.scheduleJob(informationReqJob, informationReqTrigger);
	    scheduler.scheduleJob(timeSyncJob, timeSyncTrigger);
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
	
	/*private String getOIDValue(String oid){
		
		List<Object> deviceMIBList = xeyeSnmpAgent.getDeviceMIBList();
		
		for(Object objectMIB : deviceMIBList){
			
			List<MOScalar> oidList = ((DeviceMIB)objectMIB).getOIDList();
			
			for(MOScalar scalar :  oidList){
				
				if(oid.equals("."+scalar.getOid().toString())){
					
					return ((OctetStringOID)scalar).getValue().toString();
				}
			}
		}
		
		return "";
	}*/
	
	/*
	// SPP Server로부터 받은 이벤트
	public String notifyBluetoothPacket(BluetoothPacketEvent evt){
		
		String result = "";
		
		try{
			
			JSONObject obj = JSONObject.fromObject(evt.getPacket());
			
			String command = obj.getString("command");
			
			// 게이트웨이 정보
			if("1".equals(command)){
				result = getGatewayInfo();
			}
			// 데이터 조회
			else if("2".equals(command)){
				result = getDataList();
			}
			// 지점설정
			else if("3".equals(command)){
				
				String data = obj.getString("data");
				
				result = doSetStoreName(data);
			}
			// 장비설정
			else if("4".equals(command)){
				
				String data = obj.getString("data");
				
				result = doSetDevice(data);
			}
			// 간판제어(ON)
			else if("5".equals(command)){
				
				String data = obj.getString("data");
				
				result = doControl(EnterpriseOIDManager.getEnterpriseOID() + ".20.1.0", data);
			}
			// 네트워크 정보
			else if("6".equals(command)){
				
				String ip      = IPUtils.getInstance().getEtherIP();
				String netMask = IPUtils.getInstance().getSubnetMask();
				String gateway = IPUtils.getInstance().getGatewayIP();
				String mac     = IPUtils.getInstance().getEtherMacAddress();
				
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("ip", ip);
				jsonObj.put("netmask", netMask);
				jsonObj.put("gateway", gateway);
				jsonObj.put("mac", mac);
				
				result = jsonObj.toString();
			}
			// 네트워크 설정
			else if("7".equals(command)){
				
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("result", "0");
				
				// 게이트웨이 중단
				if(doStopGateway()){
				
					// 네트워크 설정
					String data = obj.getString("data");
					
					JSONObject dataObj = JSONObject.fromObject(data);
					
					String ip      = dataObj.getString("ip");
					String netmask = dataObj.getString("netmask");
					String gateway = dataObj.getString("gateway");
					
					// /etc/network/interface 설정
					boolean r = doSetNetworkInterface(ip, netmask, gateway);
					
					if(r){
						// /etc/dhcpcd.conf 설정
						r = doSetDhcpcdConf(ip, netmask, gateway);
						
						if(r){
							
							// Ethernet IP 재설정
							IPUtils.getInstance().init();
							
							DynamicConfUtils.getInstance().doSetNetworkConfig(ip, netmask, gateway);
							
							jsonObj.put("result", "1");
						}
					}
					
					// 게이트웨이 재시작
					doStartGateway();
					
					etherIPOID.setValue(new OctetString(IPUtils.getInstance().getEtherIP()));
				}
				
				result = jsonObj.toString();
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		return result;
	}*/
	
	/*
	// Gateway 정보 JSON 리턴
	private String getGatewayInfo() throws Exception{
		
		JSONObject obj = new JSONObject();
		
		// 지점명
		obj.put("location1", DynamicConfUtils.getInstance().getLocationName1());
		obj.put("location2", DynamicConfUtils.getInstance().getLocationName2());
		obj.put("location3", DynamicConfUtils.getInstance().getLocationName3());
		obj.put("store", DynamicConfUtils.getInstance().getStoreName());
		
		// 게이트웨이 구동 여부
		if(isWork)
			obj.put("iswork", "1");
		else
			obj.put("iswork", "0");
		
		// 장비설정 여부
		int deviceCnt = 0;
		
		if(isWork){
			
			List<Map<String, Object>> inventoryList = XMLLoadManager.getInstance().getInventoryList();
			
			for(Map<String, Object> inventoryMap : inventoryList){
				
				List<Map<String, Object>> deviceList = (List<Map<String, Object>>)inventoryMap.get("DEVICE");
				
				if(deviceList.size() > 0){
					
					for(Map<String, Object> deviceMap : deviceList){
						
						String useYN = (String)deviceMap.get("USE_YN");
						
						if("Y".equals(useYN)){
							deviceCnt++;
							break;
						}
					}
				}
			}
		}
		
		if(deviceCnt == 0)
			obj.put("device", "0");
		else
			obj.put("device", "1");
		
		// Mac Address
		obj.put("mac", IPUtils.getInstance().getEtherMacAddress());
		
		return obj.toString();
	}
	*/
	
	/*
	// 데이터 조회
	private String getDataList() throws Exception{
		
		JSONArray deviceArr = new JSONArray();
		
		List<Map<String, Object>> inventoryList = XMLLoadManager.getInstance().getInventoryList();
		
		for(Map<String, Object> inventoryMap : inventoryList){
			
			List<Map<String, Object>> deviceList = (List<Map<String, Object>>)inventoryMap.get("DEVICE");
			
			if(deviceList.size() > 0){
				
				for(Map<String, Object> deviceMap : deviceList){
					
					if("Y".equals((String)deviceMap.get("USE_YN"))){
						
						JSONObject deviceObj = new JSONObject();
						
						String deviceOID  = (String)deviceMap.get("OID");
						String deviceName = (String)deviceMap.get("NAME");
						
						deviceObj.put("device_oid", deviceOID);
						deviceObj.put("device_nm", deviceName);
						
						List<Map<String, Object>> tagList = (List<Map<String, Object>>)deviceMap.get("TAG");
						
						if(tagList.size() > 0){
							
							JSONArray tagArr = new JSONArray();
							
							for(Map<String, Object> tagMap : tagList){
								
								// 서브 태그가 존재하면
								if(tagMap.get("BIT_TAG") != null){
									
									List<Map<String, Object>> bitTagList = (List<Map<String, Object>>)tagMap.get("BIT_TAG");
									
									for(Map<String, Object> bitTagMap : bitTagList){
										
										if( "R".equals((String)bitTagMap.get("ACCESS")) || "R/W".equals((String)bitTagMap.get("ACCESS")) ){
											
											JSONObject tagObj = new JSONObject();
											
											String tagOID    = (String)bitTagMap.get("OID");
											String tagName   = (String)bitTagMap.get("NAME");
											String monitorYN = (String)bitTagMap.get("MONITOR_YN");
											String tagValue  = getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "." + deviceOID + "." + tagOID + ".0");
											
											tagObj.put("tag_oid", tagOID);
											tagObj.put("tag_nm", tagName);
											tagObj.put("monitor_yn", monitorYN);
											tagObj.put("tag_val", tagValue);
											
											tagArr.add(tagObj);
										}
									}
									
								}else{
								
									if( "R".equals((String)tagMap.get("ACCESS")) || "R/W".equals((String)tagMap.get("ACCESS")) ){
									
										JSONObject tagObj = new JSONObject();
										
										String tagOID    = (String)tagMap.get("OID");
										String tagName   = (String)tagMap.get("NAME");
										String monitorYN = (String)tagMap.get("MONITOR_YN");
										String tagValue  = getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "." + deviceOID + "." + tagOID + ".0");
										
										tagObj.put("tag_oid", tagOID);
										tagObj.put("tag_nm", tagName);
										tagObj.put("monitor_yn", monitorYN);
										tagObj.put("tag_val", tagValue);
										
										tagArr.add(tagObj);
									}
								}
							}
							
							deviceObj.put("tags", tagArr);
						}
						
						deviceArr.add(deviceObj);
					}
				}
			}
		}
		
		return deviceArr.toString();
	}
	*/
	
	/*
	// 지점 설정
	private String doSetStoreName(String storeInfo) throws Exception{
		
		JSONObject jsonObj = new JSONObject();
		
		JSONObject dataObj = JSONObject.fromObject(storeInfo);
		
		String locationName1 = dataObj.getString("location_nm1");
		String locationName2 = dataObj.getString("location_nm2");
		String locationName3 = dataObj.getString("location_nm3");
		String storeName     = dataObj.getString("store_nm");
		
		DynamicConfUtils.getInstance().doSetLocation(locationName1, locationName2, locationName3, storeName);
		
		if(DynamicConfUtils.getInstance().getLocationName1().equals(locationName1) &&
				DynamicConfUtils.getInstance().getLocationName2().equals(locationName2) &&
				DynamicConfUtils.getInstance().getLocationName3().equals(locationName3) &&
				DynamicConfUtils.getInstance().getStoreName().equals(storeName)){
			jsonObj.put("result", "1");
		}else{
			jsonObj.put("result", "0");
		}
		
		return jsonObj.toString();
	}
	*/
	
	/*
	// 장비 설정
	private String doSetDevice(String deviceInfo) throws Exception{
		
		JSONObject jsonObj = new JSONObject();
		
		try{
			
			// 게이트웨이 중단
			doStopGateway();
			
			DeviceSettingUtils.getInstance().doSetDevice(deviceInfo);
			
			// 게이트웨이 재시작
			doStartGateway();
			
			jsonObj.put("result", "1");
			
		}catch(Exception e){
			jsonObj.put("result", "0");
		}
		
		return jsonObj.toString();
	}
	*/
	
	/*
	// 간판 제어
	private String doControl(String oid, String ctrlData) throws Exception{
		
		JSONObject jsonObj = new JSONObject();
		
		int result = 0;
		
		boolean flag = false;
		
		List<Object> deviceMIBList = xeyeSnmpAgent.getDeviceMIBList();
		
		for(Object objectMIB : deviceMIBList){
			
			List<MOScalar> oidList = ((DeviceMIB)objectMIB).getOIDList();
			
			for(MOScalar scalar :  oidList){
				
				if(oid.equals("."+scalar.getOid().toString())){
					
					MOAccess access = scalar.getAccess();
					
					if(access.isAccessibleForWrite()){
						result = ((OctetStringOID)scalar).setValue(new OctetString(ctrlData));
					}
					
					flag = true;
					
					break;
				}
			}
			
			if(flag){
				break;
			}
		}
		
		if(result == 0){
			jsonObj.put("result", "1");
		}else{
			jsonObj.put("result", "0");
		}
		
		return jsonObj.toString();
	}
	*/
	
	/*
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
	*/
	
	/*
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
	*/
	
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
