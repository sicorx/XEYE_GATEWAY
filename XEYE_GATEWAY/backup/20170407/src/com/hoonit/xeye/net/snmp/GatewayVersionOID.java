package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

/**
 * G/W 버전 정보를 담당하는 OID
 * @author Administrator
 *
 */
public class GatewayVersionOID extends EnumeratedScalar<OctetString>{
	
	public GatewayVersionOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
	}
	
	@Override
	public int setValue(OctetString newValue) {

		return super.setValue(newValue);
	}
}
