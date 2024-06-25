package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

/**
 * 마지막 업데이트 결과 정보를 담당하는 OID
 * @author Administrator
 *
 */
public class LastUpdateOID extends EnumeratedScalar<OctetString>{
	
	public LastUpdateOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
	}
	
	@Override
	public int setValue(OctetString newValue) {

		return super.setValue(newValue);
	}
}
