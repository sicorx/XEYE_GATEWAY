package com.hoonit.xeye.net.snmp;

import java.util.List;

import org.apache.log4j.Logger;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.OctetString;

/**
 * 고정 OID를 관리하는 MIB Object
 * @author Administrator
 *
 */
public class StaticMIB implements MOGroup {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private MOServer moServer;
	
	private OctetString myContext;
	
	private List<MOScalar> oidList;
	
	public StaticMIB() {
	}
	
	public void setOIDList(List<MOScalar> oidList){
		this.oidList = oidList;
	}
	
	public List<MOScalar> getOIDList(){
		return this.oidList;
	}
	
	public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException
	{	
		if(oidList != null){
			for(int i = 0; i < oidList.size(); i++){
				server.register(oidList.get(i), context);
			}
		}
		
		moServer = server;
		myContext = context;
	}
	
	public void unregisterMOs(MOServer server, OctetString context) {
		
		if(oidList != null){
			for(int i = 0; i < oidList.size(); i++){
				server.unregister(oidList.get(i), context);
			}
		}
		
		moServer = null;
		myContext = null;
	}
}
