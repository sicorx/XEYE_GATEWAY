package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UnsignedInteger32;

public class UnsignedInteger32OID extends EnumeratedScalar<UnsignedInteger32>{
	
	private DeviceMIB deviceMIB;
	
	public UnsignedInteger32OID(DeviceMIB deviceMIB, OID oid, MOAccess access) {
		
		super(oid, access, new UnsignedInteger32());
		
		this.deviceMIB = deviceMIB;
		
		setVolatile(true);
	}
	
	public int setValueByObserver(UnsignedInteger32 newValue) {
		
		int result = super.setValue(newValue);
		
		return result;
	}
	
	@Override
	public int setValue(UnsignedInteger32 newValue) {
		
		int result = 0;
		
		if(deviceMIB.notifySet(oid.toString(), newValue.toString())){
			result = super.setValue(newValue);
		}
		
		return result;
	}
}
