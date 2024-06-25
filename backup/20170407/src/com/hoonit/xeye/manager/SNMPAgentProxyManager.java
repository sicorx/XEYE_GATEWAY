package com.hoonit.xeye.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.OctetString;

import com.hoonit.xeye.net.snmp.DeviceMIB;
import com.hoonit.xeye.net.snmp.OctetStringOID;
import com.hoonit.xeye.net.snmp.StaticMIB;
import com.hoonit.xeye.net.snmp.XEYESNMPAgent;

public class SNMPAgentProxyManager {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static SNMPAgentProxyManager instance = new SNMPAgentProxyManager();
	
	private XEYESNMPAgent xeyeSnmpAgent;
	
	private SNMPAgentProxyManager(){
	}
	
	public static SNMPAgentProxyManager getInstance(){
		
		if(instance == null){
			instance = new SNMPAgentProxyManager();
		}
		
		return instance;
	}

	public XEYESNMPAgent getXeyeSnmpAgent() {
		return xeyeSnmpAgent;
	}

	public void setXeyeSnmpAgent(XEYESNMPAgent xeyeSnmpAgent) {
		this.xeyeSnmpAgent = xeyeSnmpAgent;
	}
	
	/**
	 * Device MIB 데이터 반환
	 * @param oid
	 * @return
	 * @throws Exception
	 */
	public String getOIDValue(String oid) throws Exception {
		
		String result = "";
		
		List<Object> deviceMIBList = xeyeSnmpAgent.getDeviceMIBList();
		
		for(Object objectMIB : deviceMIBList){
			
			List<MOScalar> oidList = ((DeviceMIB)objectMIB).getOIDList();
			
			for(MOScalar scalar :  oidList){
				
				if(oid.equals("."+scalar.getOid().toString())){
					
					result = ((OctetStringOID)scalar).getValue().toString();
					break;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Device MIB 데이터 반환
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getOIDValues(List<String> list) throws Exception {
		
		Map<String, String> resultMap = new HashMap<String, String>();
		
		List<Object> deviceMIBList = xeyeSnmpAgent.getDeviceMIBList();
		
		boolean flag = false;
		
		for(Object objectMIB : deviceMIBList){
			
			List<MOScalar> oidList = ((DeviceMIB)objectMIB).getOIDList();
			
			for(MOScalar scalar :  oidList){
				
				for(String oid : list){
					
					if(oid.equals("."+scalar.getOid().toString())){
						
						resultMap.put(oid, ((OctetStringOID)scalar).getValue().toString());
						
						// 넘어온 갯수와 Map의 갯수가 같으면 더이상 진행하지 않아도 됨.
						if(list.size() == resultMap.size()){
							
							flag = true;
							
							break;
						}
					}
				}
				
				if(flag){
					break;
				}
			}
			
			if(flag){
				break;
			}
		}
		
		return resultMap;
	}
	
	/**
	 * Device MIB 데이터 변경
	 * @param oid
	 * @param val
	 * @throws Exception
	 */
	public void setOIDValue(String oid, String val) throws Exception {
		
		List<Object> deviceMIBList = xeyeSnmpAgent.getDeviceMIBList();
		
		boolean flag = false;
		
		for(Object objectMIB : deviceMIBList){
			
			List<MOScalar> oidList = ((DeviceMIB)objectMIB).getOIDList();
			
			for(MOScalar scalar :  oidList){
				
				if(oid.equals("."+scalar.getOid().toString())){
					
					((OctetStringOID)scalar).setValue(new OctetString(val));
					
					flag = true;
					
					break;
				}
			}
			
			if(flag){
				break;
			}
		}
	}
	
	/**
	 * Static MIB 데이터 반환
	 * @param oid
	 * @return
	 * @throws Exception
	 */
	public String getStaticOIDValue(String oid) throws Exception {
		
		String result = "";
		
		List<Object> staticMIBList = xeyeSnmpAgent.getStaticMIBList();
		
		for(Object objectMIB : staticMIBList){
			
			List<MOScalar> oidList = ((StaticMIB)objectMIB).getOIDList();
			
			for(MOScalar scalar :  oidList){
				
				if(oid.equals("."+scalar.getOid().toString())){
					
					//result = ((OctetStringOID)scalar).getValue().toString();
					result = scalar.getValue().toString();
					break;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Static MIB 데이터 변경
	 * @param oid
	 * @param val
	 * @throws Exception
	 */
	public void setStaticOIDValue(String oid, String val) throws Exception {
		
		List<Object> staticMIBList = xeyeSnmpAgent.getStaticMIBList();
		
		boolean flag = false;
		
		for(Object objectMIB : staticMIBList){
			
			List<MOScalar> oidList = ((StaticMIB)objectMIB).getOIDList();
			
			for(MOScalar scalar :  oidList){
				
				if(oid.equals("."+scalar.getOid().toString())){
					
					//((OctetStringOID)scalar).setValue(new OctetString(val));
					scalar.setValue(new OctetString(val));
					flag = true;
					
					break;
				}
			}
			
			if(flag){
				break;
			}
		}
	}
}
