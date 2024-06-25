package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import com.hoonit.xeye.util.DynamicConfUtils;

/**
 * VIEW 매장코드 정보를 담당하는 OID
 * @author Administrator
 *
 */
public class ViewStoreCDOID extends EnumeratedScalar<OctetString>{
	
	public ViewStoreCDOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
	}
	
	@Override
	public int setValue(OctetString newValue) {

		DynamicConfUtils.getInstance().doSetViewStoreCDInfo(newValue.toString());

		int result = super.setValue(newValue);
		
		return result;
	}
}
