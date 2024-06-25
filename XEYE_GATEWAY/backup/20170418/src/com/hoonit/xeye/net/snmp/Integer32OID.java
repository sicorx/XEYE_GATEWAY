package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;

public class Integer32OID extends EnumeratedScalar<Integer32>{
	
	private DeviceMIB deviceMIB;
	
	public Integer32OID(DeviceMIB deviceMIB, OID oid, MOAccess access) {
		
		super(oid, access, new Integer32());
		
		this.deviceMIB = deviceMIB;
		
		setVolatile(true);
	}
	
	public int setValueByObserver(Integer32 newValue) {
		
		int result = super.setValue(newValue);
		
		return result;
	}
	
	@Override
	public int setValue(Integer32 newValue) {
		
		int result = 0;
		
		if(deviceMIB.notifySet(oid.toString(), newValue.toString())){
			result = super.setValue(newValue);
		}
		
		return result;
	}
}
