package com.hoonit.xeye.net.snmp;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

/**
 * 시스템 재시작 정보를 담당하는 OID
 * @author Administrator
 *
 */
public class SystemRebootOID extends EnumeratedScalar<OctetString>{

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	public SystemRebootOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
	}
	
	private void doReboot(){
		
		new Thread(){
			public void run(){
				
				try{
					Thread.sleep(5000);
				}catch(Exception e){}
				
				if("1".equals(getValue().toString())){
				
					Process process   = null;
					BufferedReader br = null;
					
					String[] cmd = {"reboot"};
					
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
				}
			}
		}.start();
		
	}
	
	@Override
	public int setValue(OctetString newValue) {

		int result = 0;
		
		result = super.setValue(newValue);
		
		doReboot();
		
		return result;
	}
}
