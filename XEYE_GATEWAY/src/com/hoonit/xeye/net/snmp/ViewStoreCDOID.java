package com.hoonit.xeye.net.snmp;

import org.apache.commons.lang.StringUtils;
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

		int result = 0;
		
		if(!"".equals(newValue.toString())){
			//DynamicConfUtils.getInstance().doSetViewStoreCDInfo(newValue.toString());
			if(!StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getViewStoreCD(), "").equals(newValue.toString())){
				DynamicConfUtils.getInstance().doSetViewStoreCDInfo(newValue.toString());
				
				result = super.setValue(newValue);
			}else{
				result = super.setValue(newValue);
			}
		}
		
		return result;
	}
}
