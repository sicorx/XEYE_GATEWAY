package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import com.hoonit.xeye.util.IOBoardUtils;
import com.hoonit.xeye.util.SystemTime;

/**
 * I/O보드 리셋을 담당하는 OID
 * @author Administrator
 *
 */
public class IOBoardResetOID extends EnumeratedScalar<OctetString>{
	
	public IOBoardResetOID(OID oid, MOAccess access) {
		
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
		
		if("1".equals(newValue.toString())){
			
			boolean isFlag = IOBoardUtils.getInstance().doReset();
			
			if(isFlag){
				result = super.setValue(newValue);
			}
		}
		
		return result;
	}
}
