package com.hoonit.xeye.net.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import com.hoonit.xeye.net.socket.FileClient;

public class IOBoardUploadOID extends EnumeratedScalar<OctetString>{
	
	public IOBoardUploadOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
	}
	
	@Override
	public int setValue(OctetString newValue) {

		int result = 0;
		
		/*boolean isFlag = IOBoardUtils.getInstance().doUpload();
		
		if(isFlag){
			result = super.setValue(newValue);
		}*/
		FileClient fc = new FileClient("27.122.202.54", 10001, "RGR000005", "5");
		fc.start();
		
		result = super.setValue(newValue);
		
		return result;
	}
}
