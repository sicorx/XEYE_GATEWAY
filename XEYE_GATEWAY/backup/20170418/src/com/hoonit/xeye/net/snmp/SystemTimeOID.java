package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import com.hoonit.xeye.util.SystemTime;

/**
 * 시스템 시간 정보를 담당하는 OID
 * @author Administrator
 *
 */
public class SystemTimeOID extends EnumeratedScalar<OctetString>{
	
	public SystemTimeOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
	}
	
	@Override
	public OctetString getValue(){
		
		String systemTime = new SystemTime().getCurrentTime();
		
		return new OctetString(systemTime);
	}
	
	@Override
	public int setValue(OctetString newValue) {

		int result = 0;
		
		boolean flag = new SystemTime().setCurrentTime(newValue.toString());
		
		if(flag){
			result = super.setValue(newValue);
		}
		
		return result;
	}
}
