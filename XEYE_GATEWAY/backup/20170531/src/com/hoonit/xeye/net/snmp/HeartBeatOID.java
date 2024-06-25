package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

/**
 * Heart Beat를 정보를 담당하는 OID
 * @author Administrator
 *
 */
public class HeartBeatOID extends EnumeratedScalar<OctetString>{
	
	public HeartBeatOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
	}
	
	@Override
	public int setValue(OctetString newValue) {

		return super.setValue(newValue);
	}
}
