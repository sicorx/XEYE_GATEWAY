package com.hoonit.xeye.net.snmp;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

/**
 * Gateway 재시작 정보를 담당하는 OID
 * @author Administrator
 *
 */
public class GatewayRestartOID extends EnumeratedScalar<OctetString>{

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	public GatewayRestartOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
	}
	
	@Override
	public int setValue(OctetString newValue) {

		int result = 0;
		
		if("1".equals(newValue.toString())){
			
			Process process   = null;
			BufferedReader br = null;
			
			String[] cmd = {"/home/xeye/xeye_boot_start.sh", "restart"};
			
			boolean isFlag = false;
			
			try{
				
				// 프로세스 실행
				process = Runtime.getRuntime ().exec(cmd);
				process.waitFor();
				
				br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				
				String line;
				while (( line = br.readLine ()) != null){
					logger.info(line);
				}
				
				br.close();
				process.destroy();
				
				isFlag = true;
				
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}finally{
				try{
					if(br != null) br.close();
					if(process != null) process.destroy();
				}catch(Exception e){
					logger.error(e.getMessage(), e);
				}
			}
			
			if(isFlag){
				result = super.setValue(newValue);
			}
		}
		
		return result;
	}
}
