package com.hoonit.xeye.net.snmp;

import org.apache.log4j.Logger;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class OctetStringOID extends EnumeratedScalar<OctetString>{
	
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private DeviceMIB deviceMIB;
	
	public OctetStringOID(DeviceMIB deviceMIB, OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		this.deviceMIB = deviceMIB;
		
		setVolatile(true);
	}
	
	@Override
	public OctetString getValue(){
		
		logger.info("REQ " + super.getOid() + "=" + super.getValue());
		
		return super.getValue();
	}
	
	public int setValueByObserver(OctetString newValue) {

		int result = super.setValue(newValue);
		
		return result;
	}
	
	@Override
	public int setValue(OctetString newValue) {
		
		int result = 0;
		
		if(deviceMIB.notifySet(oid.toString(), newValue.toString())){
			result = super.setValue(newValue);
		}else{
			result = -1;
		}
		
		return result;
	}
}
