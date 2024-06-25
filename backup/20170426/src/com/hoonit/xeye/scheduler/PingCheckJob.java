package com.hoonit.xeye.scheduler;

import java.nio.ByteBuffer;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.IFClientProxyManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;
import com.hoonit.xeye.net.socket.IFClient;
import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.DynamicConfUtils;
import com.hoonit.xeye.util.Utils;

public class PingCheckJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private final byte STX = 0x02;
	private final byte ETX = 0x03;
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		logger.info("Ping Policy job is executing...");
		
		try{
			
			IFClient client = IFClientProxyManager.getInstance().getIFClient();
			
			// 데이터 수신 경과시간이 30초가 넘으면 Ping Packet을 보낸다.
			if(client.getElapsedTime() > 30.0){
			
				logger.info("Ping check elapsed time is over...");
				
				Calendar cal = Calendar.getInstance();
				int minute = cal.get(Calendar.MINUTE);
				
				if(minute % 5 != 0){
					doProcess(client);
				}else{
					
					String dataLoadComplete = SNMPAgentProxyManager.getInstance().getStaticOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".1.99.0");
					
					// 데이터가 로딩 안되었으면 Ping을 전송한다.
					if("0".equals(dataLoadComplete)){
						doProcess(client);
					}
					// 데이터는 로딩 되었는데 전송을 한번도 하지 않았다면
					else{
						if(!DynamicConfUtils.getInstance().isDataSendAtFirst()){
							doProcess(client);
						}
					}
				}
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		logger.info("Ping Policy job is executed...");
	}
	
	private void doProcess(IFClient client) throws Exception {
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[7];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x03);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = 0x00;
		
		// CRC
		byte[] crc = Utils.getBytes(writeBuffer, 3, writeBuffer.length-6);
		
		short sCRC = CRC16.getInstance().getCRC(crc);
		
		ByteBuffer wBuffer = ByteBuffer.allocate(2);
		wBuffer.putShort(sCRC);
		wBuffer.flip();
		
		byte crc1 = wBuffer.get();
		byte crc2 = wBuffer.get();
		
		writeBuffer[wIdx++] = crc1;
		writeBuffer[wIdx++] = crc2;
		writeBuffer[wIdx++] = ETX;
		
		if(client != null){
			client.write(writeBuffer);
		}
	}
}
