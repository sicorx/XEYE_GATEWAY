package com.hoonit.xeye.net.snmp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.io.ImportModes;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.TransportDomains;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.ThreadPool;

import com.hoonit.xeye.util.ResourceBundleHandler;

/**
 * The <code>XEYESNMPAgent</code> is a sample SNMP agent implementation of all
 * features (MIB implementations) provided by the SNMP4J-Agent framework.
 * The <code>XEYESNMPAgent</code> extends the <code>BaseAgent</code> which provides
 * a framework for custom agent implementations through hook methods. Those
 * abstract hook methods need to be implemented by extending the
 * <code>BaseAgent</code>.
 * <p>
 * This IF-MIB implementation part of this test agent, is instrumentation as
 * a simulation MIB. Thus, by changing the agentppSimMode
 * (1.3.6.1.4.1.4976.2.1.1.0) from 'oper(1)' to 'config(2)' any object of the
 * IF-MIB is writable and even creatable (columnar objects) via SNMP. Check it
 * out!
 *
 * @author SungChan HA
 * @version 1.0
 */
public class XEYESNMPAgent extends BaseAgent {

	// initialize Log4J logging
	static {
		LogFactory.setLogFactory(new Log4jLogFactory());
	}
	
	protected final Logger logger = Logger.getLogger(getClass().getName());

	private String address;
	
	private int requestPoolSize = 1;
  
	private List<Object> deviceMIBList;
	
	private List<Object> staticMIBList;

	/**
	 * Creates the test agent with a file to read and store the boot counter and
	 * a file to read and store its configuration.
	 *
	 * @param bootCounterFile
	 *    a file containing the boot counter in serialized form (as expected by
	 *    BaseAgent).
	 * @param configFile
	 *    a configuration file with serialized management information.
	 * @throws IOException
	 *    if the boot counter or config file cannot be read properly.
	 */
	public XEYESNMPAgent(File bootCounterFile, File configFile) throws IOException {
	
		super(bootCounterFile, configFile, new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));

		this.address = ResourceBundleHandler.getInstance().getString("snmp.agent.address");
		
		this.requestPoolSize = Integer.parseInt(ResourceBundleHandler.getInstance().getString("snmp.request.pool.size"));
		
		agent.setWorkerPool(ThreadPool.create("RequestPool", this.requestPoolSize));
		
		deviceMIBList = new ArrayList<Object>();
		
		staticMIBList = new ArrayList<Object>();
	}
	
	public List<Object> getDeviceMIBList(){
		return this.deviceMIBList;
	}
	
	public List<Object> getStaticMIBList(){
		return this.staticMIBList;
	}
	
	protected void registerManagedObjects() {
		
	}
	
	protected void unregisterManagedObjects() {
		// here we should unregister those objects previously registered...
	}
	
	public void registerMIB(Object objectMIB){
		
		try {
			
			if(objectMIB instanceof DeviceMIB){
				
				((DeviceMIB)objectMIB).registerMOs(server, null);
					
				deviceMIBList.add(objectMIB);
				
			}else{
				
				((StaticMIB)objectMIB).registerMOs(server, null);
				
				staticMIBList.add(objectMIB);
			}
			
		}catch (DuplicateRegistrationException ex) {
			logger.error(ex);
		}
	}
	
	public void unregisterDeviceMIB(){
			
		for(Object objectMIB : this.deviceMIBList){
			((DeviceMIB)objectMIB).unregisterMOs(server, null);
		}
		
		this.deviceMIBList.clear();
	}

	protected void addNotificationTargets(SnmpTargetMIB targetMIB, SnmpNotificationMIB notificationMIB) {
		
		targetMIB.addDefaultTDomains();

		targetMIB.addTargetAddress(new OctetString("notificationV2c"),
                               	   TransportDomains.transportDomainUdpIpv4,
                               	   new OctetString(new UdpAddress("127.0.0.1/162").getValue()),
                               	   200, 1,
                               	   new OctetString("notify"),
                               	   new OctetString("v2c"),
                               	   StorageType.permanent);
		targetMIB.addTargetAddress(new OctetString("notificationV3"),
                               	   TransportDomains.transportDomainUdpIpv4,
                               	   new OctetString(new UdpAddress("127.0.0.1/1162").getValue()),
                               	   200, 1,
                               	   new OctetString("notify"),
                               	   new OctetString("v3notify"),
                               	   StorageType.permanent);
		
		targetMIB.addTargetParams(new OctetString("v2c"),
                              	  MessageProcessingModel.MPv2c,
                              	  SecurityModel.SECURITY_MODEL_SNMPv2c,
	                              new OctetString("cpublic"),
	                              SecurityLevel.AUTH_PRIV,
	                              StorageType.permanent);
		targetMIB.addTargetParams(new OctetString("v3notify"),
                              	  MessageProcessingModel.MPv3,
	                              SecurityModel.SECURITY_MODEL_USM,
	                              new OctetString("v3notify"),
	                              SecurityLevel.NOAUTH_NOPRIV,
	                              StorageType.permanent);
	}

	protected void addViews(VacmMIB vacm) {
		
	    vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1,
	                  new OctetString("cpublic"),
	                  new OctetString("v1v2group"),
	                  StorageType.nonVolatile);
	    vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c,
	                  new OctetString("cpublic"),
	                  new OctetString("v1v2group"),
	                  StorageType.nonVolatile);
	    vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
	                  new OctetString("SHADES"),
	                  new OctetString("v3group"),
	                  StorageType.nonVolatile);
	
	    vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
	                   SecurityModel.SECURITY_MODEL_ANY,
	                   SecurityLevel.NOAUTH_NOPRIV,
	                   MutableVACM.VACM_MATCH_EXACT,
	                   new OctetString("fullReadView"),
	                   new OctetString("fullWriteView"),
	                   new OctetString("fullNotifyView"),
	                   StorageType.nonVolatile);
	    vacm.addAccess(new OctetString("v3group"), new OctetString(),
	                   SecurityModel.SECURITY_MODEL_USM,
	                   SecurityLevel.AUTH_PRIV,
	                   MutableVACM.VACM_MATCH_EXACT,
	                   new OctetString("fullReadView"),
	                   new OctetString("fullWriteView"),
	                   new OctetString("fullNotifyView"),
	                   StorageType.nonVolatile);
	    vacm.addAccess(new OctetString("v3restricted"), new OctetString(),
	                   SecurityModel.SECURITY_MODEL_USM,
	                   SecurityLevel.NOAUTH_NOPRIV,
	                   MutableVACM.VACM_MATCH_EXACT,
	                   new OctetString("restrictedReadView"),
	                   new OctetString("restrictedWriteView"),
	                   new OctetString("restrictedNotifyView"),
	                   StorageType.nonVolatile);
	
	    vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
	                           new OctetString(), VacmMIB.vacmViewIncluded,
	                           StorageType.nonVolatile);
	    vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"),
	                           new OctetString(), VacmMIB.vacmViewIncluded,
	                           StorageType.nonVolatile);
	    vacm.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1.3"),
	                           new OctetString(), VacmMIB.vacmViewIncluded,
	                           StorageType.nonVolatile);
	
	    vacm.addViewTreeFamily(new OctetString("restrictedReadView"),
	                           new OID("1.3.6.1.2"),
	                           new OctetString(), VacmMIB.vacmViewIncluded,
	                           StorageType.nonVolatile);
	    vacm.addViewTreeFamily(new OctetString("restrictedWriteView"),
	                           new OID("1.3.6.1.2.1"),
	                           new OctetString(),
	                           VacmMIB.vacmViewIncluded,
	                           StorageType.nonVolatile);
	    vacm.addViewTreeFamily(new OctetString("restrictedNotifyView"),
	                           new OID("1.3.6.1.2"),
	                           new OctetString(), VacmMIB.vacmViewIncluded,
	                           StorageType.nonVolatile);
	    vacm.addViewTreeFamily(new OctetString("restrictedNotifyView"),
	                           new OID("1.3.6.1.6.3.1"),
	                           new OctetString(), VacmMIB.vacmViewIncluded,
	                           StorageType.nonVolatile);
	}

	protected void addUsmUser(USM usm) {

		String securityName       = ResourceBundleHandler.getInstance().getString("snmp.agent.security.name");
		String shadesAuthPassword = ResourceBundleHandler.getInstance().getString("snmp.agent.SHADES.Auth.Password");
		String shadesPrivPassword = ResourceBundleHandler.getInstance().getString("snmp.agent.SHADES.Priv.Password");
		
		if("SHADES".equals(securityName)){
			
		    UsmUser user = new UsmUser(new OctetString("SHADES"),
		                               AuthSHA.ID,
		                               new OctetString(shadesAuthPassword),
		                               PrivDES.ID,
		                               new OctetString(shadesPrivPassword));
		    
		    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
		}
	}
  
	protected void initTransportMappings() throws IOException {
		
		transportMappings = new TransportMapping<?>[1];
		Address addr = GenericAddress.parse(address);
		TransportMapping<? extends Address> tm = TransportMappings.getInstance().createTransportMapping(addr);
		transportMappings[0] = tm;
	}

	protected void addCommunities(SnmpCommunityMIB communityMIB) {
		
		String securityName = ResourceBundleHandler.getInstance().getString("snmp.agent.security.name");
		
		Variable[] com2sec = new Variable[] {
										     new OctetString("public"),              // community name
										     new OctetString(securityName),          // security name
										     getAgent().getContextEngineID(),        // local engine ID
										     new OctetString("public"),              // default context name
										     new OctetString(),                      // transport tag
										     new Integer32(StorageType.nonVolatile), // storage type
										     new Integer32(RowStatus.active)         // row status
											};
		
		SnmpCommunityMIB.SnmpCommunityEntryRow row = 
				communityMIB.getSnmpCommunityEntry().createRow(new OctetString("public2public").toSubIndex(true), com2sec);
		
		communityMIB.getSnmpCommunityEntry().addRow(row);
	}

	protected void registerSnmpMIBs() {
		super.registerSnmpMIBs();
	}
	
	public void doStart(){
		
		try{
			
			BasicConfigurator.configure();
			
			logger.info("SNMP Init...");
			
			init();
			
			logger.info("SNMP Init Completed...");
			
			logger.info("SNMP Load Config...");
			
			loadConfig(ImportModes.REPLACE_CREATE);
			
			logger.info("SNMP Load Config Completed...");
			
			logger.info("SNMP Add Shutdown Hook...");
			
			addShutdownHook();
			
			logger.info("SNMP Add Shutdown Hook Completed...");
			
			getServer().addContext(new OctetString("public"));
			
			logger.info("SNMP Finish Init...");
			
			finishInit();
			
			logger.info("SNMP Finish Init Completed...");
			
			logger.info("SNMP Run...");
			
			run();
			
			logger.info("SNMP Run Completed...");
			
			logger.info("SNMP Send Cold Start Notification...");
			
			sendColdStartNotification();
			
			logger.info("SNMP Send Cold Start Notification Completed...");
			
		}catch(Exception e){
			logger.error(e);
		}
	}
}
