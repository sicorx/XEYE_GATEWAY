package com.hoonit.xeye.scheduler;

import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hoonit.xeye.manager.IFClientProxyManager;
import com.hoonit.xeye.net.socket.IFClient;
import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.DynamicConfUtils;
import com.hoonit.xeye.util.Utils;

public class InformationReqJob implements Job {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private final byte STX              = 0x02;
	private final byte ETX              = 0x03;
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		
		logger.info("Information job is executing...");
		
		try{
			
			if(!"".equals(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getStoreCD(), ""))){
				doProcess();
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
		logger.info("Information job is executing...");
	}
	
	private void doProcess() throws Exception {
		
		// IF Server에 동시에 많이 접속하는 걸 방지하기 위해
        // 난수(0~59초)만큼 Delay한 후 접속 시도
        try{
        	
        	Random random = new Random();
        	
        	Thread.sleep(random.nextInt(60) * 1000);
        	
        }catch(Exception e){
        	logger.error(e.getMessage(), e);
        }
		
		// 요청 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[29];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x19);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = 0x11;
		
		// 매장코드
		short storeCDLen = 20;
		String storeCD = DynamicConfUtils.getInstance().getStoreCD();
		String gwID    = DynamicConfUtils.getInstance().getGwID();
		
        try{
        	
    		String strCD = StringUtils.defaultIfEmpty(storeCD, "");
    		byte[] storeBytes = Utils.getFillNullByte(strCD.getBytes(), storeCDLen);
    		
    		for(int i = 0; i < storeBytes.length; i++){
    			writeBuffer[wIdx++] = storeBytes[i];
    		}
        }catch(ArrayIndexOutOfBoundsException e){
        	logger.error(e.getMessage(), e);
        	
        	for(int i = 0; i < storeCDLen; i++){
    			writeBuffer[wIdx++] = 0x20;
    		}
        }
        // GW ID
        byte[] gwIDs = ByteUtils.toUnsignedShortBytes(Integer.parseInt(StringUtils.defaultIfEmpty(gwID, "0")));
        writeBuffer[wIdx++] = gwIDs[0];
        writeBuffer[wIdx++] = gwIDs[1];
		
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
		
		IFClient client = IFClientProxyManager.getInstance().getIFClient();
		
		if(client != null){
			client.write(writeBuffer);
		}
	}
}
