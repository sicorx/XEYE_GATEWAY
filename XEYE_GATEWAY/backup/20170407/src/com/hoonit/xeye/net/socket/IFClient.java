package com.hoonit.xeye.net.socket;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.hoonit.xeye.manager.InformationManager;
import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.DynamicConfUtils;
import com.hoonit.xeye.util.IPUtils;
import com.hoonit.xeye.util.ResourceBundleHandler;
import com.hoonit.xeye.util.StringZipper;
import com.hoonit.xeye.util.Utils;

import net.sf.json.JSONObject;  
  
/** 
 * IF Server 통신처리를 담당하는 객체
 */  
public class IFClient {
    
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private Timer timer;
	
	private static final int PORT_NUMBER = 12100;
	
	private Lock writeLock = new ReentrantLock();
	
    private Abortable abortable = new Abortable();
    
    private ClientThread clientThread;
    
    private int bufferSize = 1024;
    
    private String host;
    
    private int port;
    
    private int delay;
    
    private static final byte STX              = 0x02;
	private static final byte ETX              = 0x03;
	private static final byte NORMAL           = 1;
	private static final byte ERR_STX_ETX      = 2;
	private static final byte ERR_CRC          = 3;
	private static final byte ERR_INVALID_DATA = 4;
	private static final byte ERR_FILE_TRANS   = 5;
	private static final byte ERR_CTRL         = 6;
	private static final byte ERR_EXCEPTION    = 7;
    
    public IFClient(){
    	
    	this.bufferSize = DynamicConfUtils.getInstance().getClientBufferSize();
    	this.host       = DynamicConfUtils.getInstance().getIfServerHost();
    	this.port       = DynamicConfUtils.getInstance().getIfServerPort();
    	this.delay      = DynamicConfUtils.getInstance().getIfServerSendDelayTime();
    	
    	logger.info("host  : " + host);
    	logger.info("port  : " + port);
    	logger.info("delay : " + delay);	
    }
    
    public void pause(){
    	
    	if(timer != null){
    		timer.cancel();
    		timer = null;
    	}
    }
    
    public void resume(){
    	
    	if(timer != null){
    		timer.cancel();
    		timer = null;
    	}
    	
    	timer = new Timer();
    	
    	IFClientTask task = new IFClientTask(this);
    	
    	Calendar cal = Calendar.getInstance();
    	int min = cal.get(Calendar.MINUTE);
		
		if(min > 0 && min < 5){
			cal.add(Calendar.MINUTE, (5-min));
		}else if(min > 5 && min < 10){
			cal.add(Calendar.MINUTE, (10-min));
		}else if(min > 10 && min < 15){
			cal.add(Calendar.MINUTE, (15-min));
		}else if(min > 15 && min < 20){
			cal.add(Calendar.MINUTE, (20-min));
		}else if(min > 20 && min < 25){
			cal.add(Calendar.MINUTE, (25-min));
		}else if(min > 25 && min < 30){
			cal.add(Calendar.MINUTE, (30-min));
		}else if(min > 30 && min < 35){
			cal.add(Calendar.MINUTE, (35-min));
		}else if(min > 35 && min < 40){
			cal.add(Calendar.MINUTE, (40-min));
		}else if(min > 40 && min < 45){
			cal.add(Calendar.MINUTE, (45-min));
		}else if(min > 45 && min < 50){
			cal.add(Calendar.MINUTE, (50-min));
		}else if(min > 50 && min < 55){
			cal.add(Calendar.MINUTE, (55-min));
		}else if(min > 55 && min < 60){
			cal.add(Calendar.MINUTE, (60-min));
		}else{
			cal.add(Calendar.MINUTE, (min+5));
		}
    	
    	Calendar cal2 = Calendar.getInstance();
    	cal2.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
    	cal2.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
    	cal2.set(Calendar.SECOND, 0);
    	cal2.set(Calendar.MILLISECOND, 0);
    	
    	// 1분후 5분마다 전송
    	timer.scheduleAtFixedRate(task, cal2.getTime(), delay);
    }
      
    /** 
     * start client
     *  
     * @param host
     * @param port
     */  
    public void start() {
        
    	logger.info("Client is starting...");
    	
        abortable.init();
        
        if (clientThread == null || !clientThread.isAlive()) {
            clientThread = new ClientThread(abortable, host, port);
            clientThread.start();
        }
    }
    
    public void restart(){
    	
    	logger.info("Client is restarting...");
    	
    	this.host = DynamicConfUtils.getInstance().getIfServerHost();
    	this.port = DynamicConfUtils.getInstance().getIfServerPort();
    	
    	abortable.init();
    	 
    	clientThread = new ClientThread(abortable, host, port);
        clientThread.start();
    }
    
    /** 
     * stop client
     */  
    public void stop() {
        
        abortable.done = true;
          
        if (clientThread != null && clientThread.isAlive()) {
            clientThread.interrupt();
        }
    }
    
    public boolean isSocketConnected(){
    	
    	if(clientThread != null){
    		
    		return clientThread.getSocketChannel().isConnected();
    		
    	}else{
    		return false;
    	}
    }
    
    /** 
     *  Socket write
     * @param text
     * @throws IOException
     */
    public void write(byte[] b) throws IOException {
    	clientThread.write(b);
    }
    
    /** 
     * Client Thread Object
     */  
    public class ClientThread extends Thread {
          
        private Abortable abortable;
        private String host;
        private int port;
        private SocketChannel client;
        
        /** 
         *  Constructor
         * @param abortable 
         * @param host 
         * @param port 
         */  
        public ClientThread(Abortable abortable, String host, int port) {
            this.abortable = abortable;
            this.host = host;
            this.port = port;
        }
        
        public SocketChannel getSocketChannel(){
        	return client;
        }
  
        /** 
         *  Socket write
         * @param text 
         * @throws IOException  
         */  
        public void write(byte[] b) throws IOException {
        	
        	//synchronized(writeLock){
        	try{
	        	if(writeLock.tryLock(5000, TimeUnit.SECONDS)){
	        		
	        		try{
	        			
	        			StringBuffer sb = new StringBuffer();
	            		for (int i = 0; i < b.length; i++) {
	            			sb.append(ByteUtils.toHexString((byte) b[i])).append(" ");
	            		}
	            		logger.info("Write data : " + sb.toString());
	            		
			            int len = client.write(ByteBuffer.wrap(b));
			            
			            logger.info("Writed data length : " + len);
			            
	        		}catch(IOException e){
	        			throw e;
	        		}catch(Exception e){
	        			logger.error(e.getMessage(), e);
	        		}finally{
	        			writeLock.unlock();
	        		}
	        	}
        	}catch(InterruptedException e){
        		logger.error(e.getMessage(), e);
        	}
        }
  
        @Override  
        public void run() {
        	
            super.run();
              
            boolean done = false;
            
            Selector selector = null;
            
            BufferedOutputStream bos = null;
            long fileSize = 0L;
            long totalFileReadBytes = 0L;
              
            try {
            	
                client = SocketChannel.open();
                client.configureBlocking(false);
                client.connect(new InetSocketAddress(host, port));
                
                selector = Selector.open();
                client.register(selector, SelectionKey.OP_READ);
                
                while (!Thread.interrupted() && !abortable.isDone() && !client.finishConnect()) {
                    Thread.sleep(10);
                }
                
                logger.info("Client is connected");
                
                // IF Server 접속
                doConnectIFServer();
    			
                short wIdx = 0;
                
                ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                
                byte cmd = 0;
                
                while (!Thread.interrupted() && !abortable.isDone() && !done) {
                    
                    selector.select(3000);
                    
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    
                    while (!Thread.interrupted() && !abortable.isDone() && !done && iter.hasNext()) {
                    	
                        SelectionKey key = iter.next();
                        
                        if (key.isReadable()) {
                        	
                        	buffer.clear();
                        	
                            int readBytes = client.read(buffer);
                            
                            if (readBytes < 0) {
                                done = true;
                                break;
                            } else if (readBytes == 0) {
                                continue;
                            }
                            
                            logger.info("read length : " + readBytes);
                            
                            buffer.flip();
                            
                            byte[] data = new byte[readBytes];
                            
                            int idx = 0;
                    		while (buffer.hasRemaining()) {
                    			data[idx++] = buffer.get();
                    		}
                    		
                    		try{
                    			
	                    		// 패치파일 수신중이면
	                    		if(cmd == 4){
	                    			
	                    			if(bos != null){
	                    				bos.write(data);
	                    			}
	                    			
	                    			totalFileReadBytes += readBytes;
	                    			
	                    			logger.debug("file size="+fileSize+", read size="+totalFileReadBytes);
	                    			
	                    			if(totalFileReadBytes >= fileSize){
	                    				
	                    				logger.info("패치파일 수신 완료");
	                    				
	                    				bos.flush();
	                    				bos.close();
	                    				
	                    				logger.info("패치파일 수신 결과 전송");
	                    				
	                    				// 결과 전송
	        							wIdx = 0;
	        							byte[] resultBuffer = new byte[8];
	        							resultBuffer[wIdx++] = STX;
	        							
	        							byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x04);
	        							resultBuffer[wIdx++] = lenBytes[0];
	        							resultBuffer[wIdx++] = lenBytes[1];
	        							
	        							resultBuffer[wIdx++] = cmd;
	        							resultBuffer[wIdx++] = NORMAL;
	        							
	        							// CRC
	        							byte[] crc = Utils.getBytes(resultBuffer, 3, resultBuffer.length-6);
	        							
	        							short sCRC = CRC16.getInstance().getCRC(crc);
	        							
	        							ByteBuffer crcResultBuffer = ByteBuffer.allocate(2);
	        							crcResultBuffer.putShort(sCRC);
	        							crcResultBuffer.flip();
	        							
	        							byte crc1 = crcResultBuffer.get();
	        							byte crc2 = crcResultBuffer.get();
	        							
	        							resultBuffer[wIdx++] = crc1;
	        							resultBuffer[wIdx++] = crc2;
	        							resultBuffer[wIdx++] = ETX;
	        							
	        							write(resultBuffer);
	        							
	        							cmd = 0;
	        							
	        							writeLock.unlock();
	                    			}
	                    			
	                    		}else{
	                    		
		                    		byte stx = data[0];
		        					//byte len = data[1];
		                    		int len = ByteUtils.toUnsignedShort(Utils.getBytes(data, 1, 2));
		        					byte etx = data[readBytes-1];
		        					
		        					logger.debug("STX=" + ByteUtils.toHexString(stx));
		        					logger.debug("ETX=" + ByteUtils.toHexString(etx));
		        					
		        					if(stx == STX && etx == ETX){
		        					
		        						logger.debug("LEN=" + Integer.toHexString(len & 0xFFFF));
		        						
		        						byte[] dataBuffer = Utils.getBytes(data, 3, len-2);
		        						byte[] crcBuffer  = Utils.getBytes(data, len+1, 2);
		        						
		        						short dataCRC = CRC16.getInstance().getCRC(dataBuffer);
		        						
		        						ByteBuffer crcBuffer2 = ByteBuffer.allocate(2);
		        						crcBuffer2.put(crcBuffer);
		        						crcBuffer2.flip();
		        						
		        						short chekCRC = crcBuffer2.getShort();
		        						
		        						logger.debug("DATA CRC=" + dataCRC);
		        						logger.debug("CHEC CRC=" + chekCRC);
		        						
		        						if(dataCRC == chekCRC){
		        							
		        							idx = 0;
		        							
		        							cmd = dataBuffer[idx++];
		        							
		        							logger.debug("CMD=" + ByteUtils.toHexString(cmd));
		        							
		        							// IF Sever 접속 결과 수신
		        							if(cmd == 0x01){
		        								doProcessCmd1(dataBuffer);
		        							}
		        							// Data전송 결과 수신
		        							else if(cmd == 0x02){
		        								doProcessCmd2(cmd, dataBuffer);
		        							}
		        							// 매장정보 수신
		        							else if(cmd == 0x03){
		        								doProcessCmd3(cmd, dataBuffer);
		        							}
		        							// 패치파일 정보 수신
		        							else if(cmd == 0x04){
		        								
		        								if(writeLock.tryLock(5000, TimeUnit.SECONDS)){
		        								
			        								logger.info("패치파일 정보 수신");
			        								
			        								byte fileGubun       = dataBuffer[idx++]; // 1:GW, 2:IO
			        								byte[] fileSizeBytes = Utils.getBytes(dataBuffer, 2, 8);
			        								byte[] fileNameBytes = Utils.getBytes(dataBuffer, 10, dataBuffer.length-10); // 파일사이즈와 CMD 합친값을 빼면 파일명
			        								
			        								fileSize = ByteUtils.toLong(fileSizeBytes);
			        								String fileName = new String(fileNameBytes, "UTF-8");
			        								
			        								logger.info("파일구분="+fileGubun);
			        								logger.info("파일사이즈="+fileSize);
			        								logger.info("파일명="+fileName);
			        								
			        								File resourceDir = new File("resource");
			        								
			        								if(!resourceDir.exists())
			        									resourceDir.mkdirs();
			        								
			        								File uploadDir = new File(resourceDir, "upload");
			        								
			        								if(!uploadDir.exists())
			        									uploadDir.mkdirs();
			        								
			        								File file = null;
			        								
			        								if(fileGubun == 1){
			        									
			        									File gwDir = new File(uploadDir, "gw");
			        									
			        									if(!gwDir.exists())
			        										gwDir.mkdirs();
			        									
			        									file = new File(gwDir, fileName);
			        								}else{
			        									
			        									File ioDir = new File(uploadDir, "io");
			        									
			        									if(!ioDir.exists())
			        										ioDir.mkdirs();
			        									
			        									file = new File(ioDir, fileName);
			        								}
			        								
			        								totalFileReadBytes = 0;
			    	                            	
			    	                            	bos = new BufferedOutputStream(new FileOutputStream(file));
		        								}
		        							}
		        							// 일출일몰시간 수신
		        							else if(cmd == 0x05){
		        								doProcessCmd5(cmd, dataBuffer);
		        							}
		        							// 냉난방정책 수신
		        							else if(cmd == 0x06){
		        								doProcessCmd6(cmd, dataBuffer);
		        							}
		        							// 냉난방 권장온도 수신
		        							else if(cmd == 0x07){
		        								doProcessCmd7(cmd, dataBuffer);
		        							}
		        							
		        						}else{
		        							
		        							// 응답이 아닐 경우에만 전송한다.
		        							// 0x01 : IF Server 접속정보 수신
		        							if(data[3] != 0x01){
		        								
		        								// 패치파일일 경우 OutputStream을 닫는다
		        								if(data[3] == 0x04){
		        									if(bos != null) bos.close(); 
		        								}
		        								
			        							logger.info("CRC 오류 전송");
			        							
			        							// 결과 전송
			        							wIdx = 0;
			        							byte[] resultBuffer = new byte[8];
			        							resultBuffer[wIdx++] = STX;
			        							
			        							byte[] tempBytes = ByteUtils.toBytes((short)0x04);
			        							resultBuffer[wIdx++] = tempBytes[0];
			        							resultBuffer[wIdx++] = tempBytes[1];
			        							
			        							resultBuffer[wIdx++] = data[3];
			        							resultBuffer[wIdx++] = ERR_CRC;
			        							
			        							// CRC
			        							byte[] crc = Utils.getBytes(resultBuffer, 3, resultBuffer.length-6);
			        							
			        							short sCRC = CRC16.getInstance().getCRC(crc);
			        							
			        							ByteBuffer crcResultBuffer = ByteBuffer.allocate(2);
			        							crcResultBuffer.putShort(sCRC);
			        							crcResultBuffer.flip();
			        							
			        							byte crc1 = crcResultBuffer.get();
			        							byte crc2 = crcResultBuffer.get();
			        							
			        							resultBuffer[wIdx++] = crc1;
			        							resultBuffer[wIdx++] = crc2;
			        							resultBuffer[wIdx++] = ETX;
			        							
			        							write(resultBuffer);
		        							}
		        						}
		        					}else{
		        						
		        						// 응답이 아닐 경우에만 전송한다.
	        							// 0x01 : IF Server 접속정보 수신
		        						if(data[3] != 0x01){
		        							
		        							// 패치파일일 경우 OutputStream을 닫는다
	        								if(data[3] == 0x04){
	        									if(bos != null) bos.close();
	        								}
	        								
			        						logger.info("STX, ETX 오류 전송");
			        						
			        						// 결과 전송
			        						wIdx = 0;
			        						byte[] resultBuffer = new byte[8];
			        						resultBuffer[wIdx++] = STX;
			        						
			        						byte[] tempBytes = ByteUtils.toBytes((short)0x04);
		        							resultBuffer[wIdx++] = tempBytes[0];
		        							resultBuffer[wIdx++] = tempBytes[1];
		        							
			        						resultBuffer[wIdx++] = data[3];
			        						resultBuffer[wIdx++] = ERR_STX_ETX;
			        						
			        						// CRC
			        						byte[] crc = Utils.getBytes(resultBuffer, 3, resultBuffer.length-6);
			        						
			        						short sCRC = CRC16.getInstance().getCRC(crc);
			        						
			        						ByteBuffer crcBuffer = ByteBuffer.allocate(2);
			        						crcBuffer.putShort(sCRC);
			        						crcBuffer.flip();
			        						
			        						byte crc1 = crcBuffer.get();
			        						byte crc2 = crcBuffer.get();
			        						
			        						resultBuffer[wIdx++] = crc1;
			        						resultBuffer[wIdx++] = crc2;
			        						resultBuffer[wIdx++] = ETX;
			        						
			        						write(resultBuffer);
		        						}
		        					}
	                    		}
                    		}catch(Exception e){
                    			logger.error(e.getMessage(), e);
                    		}
                        }
                    }
                }
                  
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                
                if (client != null) {
                    try {
                        client.socket().close();
                        client.close();
                    } catch (IOException e) {
                    	logger.error(e.getMessage(), e);
                    }
                }
                
                logger.info("Client is closed...");
                
                // Data 전송 Timer 중지
                pause();
                
                // IF Server 재접속 시도
                restart();
            }
        }  
    }  
    
    /**
     * IF Server 접속
     * @throws Exception
     */
    private void doConnectIFServer() throws Exception{
    	
    	// 접속 후 IP, MAC Address 전송
        byte[] writeBuffer = new byte[50];
        
        String ip  = IPUtils.getInstance().getEtherIP();
        String mac = IPUtils.getInstance().getEtherMacAddress();
        
        String[] ips = ip.split("\\.");
        String[] macs = mac.split("-");
        
        short wIdx = 0;
        
        writeBuffer[wIdx++] = STX;  // STX
        
        // LEN
        byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x2E);
        writeBuffer[wIdx++] = lenBytes[0];
        writeBuffer[wIdx++] = lenBytes[1];
        
        writeBuffer[wIdx++] = 0x01;    // CMD
        
        // Version
        String version = DynamicConfUtils.getInstance().getVersion();
        
        BigDecimal bd1 = new BigDecimal(version);
		BigDecimal bd2 = new BigDecimal("10");
		
		int cv = bd1.multiply(bd2).intValue();
		
		writeBuffer[wIdx++] = (byte)cv;
        
        // IP
        byte[] ip1 = ByteUtils.toUnsignedShortBytes(Integer.parseInt(ips[0]));
        byte[] ip2 = ByteUtils.toUnsignedShortBytes(Integer.parseInt(ips[1]));
        byte[] ip3 = ByteUtils.toUnsignedShortBytes(Integer.parseInt(ips[2]));
        byte[] ip4 = ByteUtils.toUnsignedShortBytes(Integer.parseInt(ips[3]));
        
        writeBuffer[wIdx++] = ip1[0];
        writeBuffer[wIdx++] = ip1[1];
        writeBuffer[wIdx++] = ip2[0];
        writeBuffer[wIdx++] = ip2[1];
        writeBuffer[wIdx++] = ip3[0];
        writeBuffer[wIdx++] = ip3[1];
        writeBuffer[wIdx++] = ip4[0];
        writeBuffer[wIdx++] = ip4[1];
        
        // MAC
        writeBuffer[wIdx++] = macs[0].getBytes()[0];
        writeBuffer[wIdx++] = macs[0].getBytes()[1];
        writeBuffer[wIdx++] = macs[1].getBytes()[0];
        writeBuffer[wIdx++] = macs[1].getBytes()[1];
        writeBuffer[wIdx++] = macs[2].getBytes()[0];
        writeBuffer[wIdx++] = macs[2].getBytes()[1];
        writeBuffer[wIdx++] = macs[3].getBytes()[0];
        writeBuffer[wIdx++] = macs[3].getBytes()[1];
        writeBuffer[wIdx++] = macs[4].getBytes()[0];
        writeBuffer[wIdx++] = macs[4].getBytes()[1];
        writeBuffer[wIdx++] = macs[5].getBytes()[0];
        writeBuffer[wIdx++] = macs[5].getBytes()[1];
        
        int storeCDLen = 20;
        
        // 매장코드
        try{
        	
    		String storeCD = DynamicConfUtils.getInstance().getStoreCD();
    		byte[] storeBytes = Utils.getFillNullByte(storeCD.getBytes(), storeCDLen);
    		
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
        byte[] gwID = ByteUtils.toUnsignedShortBytes(Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getGwID(), "0")));
        writeBuffer[wIdx++] = gwID[0];
        writeBuffer[wIdx++] = gwID[1];
		
        // CRC
		byte[] crc = Utils.getBytes(writeBuffer, 3, writeBuffer.length-6);
		
		short chkCRC = CRC16.getInstance().getCRC(crc);
		
		ByteBuffer wBuffer = ByteBuffer.allocate(2);
		wBuffer.putShort(chkCRC);
		wBuffer.flip();
		
		byte crc1 = wBuffer.get();
		byte crc2 = wBuffer.get();
		
		writeBuffer[wIdx++] = crc1;
		writeBuffer[wIdx++] = crc2;
		
		// ETX
		writeBuffer[wIdx++] = ETX;
		
		logger.info("IF Server 접속 시도");
		
		write(writeBuffer);
    }
    
    /**
     * IF Sever 접속 응답 처리
     */
    private void doProcessCmd1(byte[] dataBuffer){
    	
    	logger.info("IF Sever 접속결과 처리");
    	
    	int idx = 1;
    	
    	byte result = dataBuffer[idx++];
		
		logger.info("RESULT="+result);
		
		if(result == NORMAL){
			logger.info("SUCCESS");
			
			// 매장코드
    		String storeCD = new String(Utils.getBytes(dataBuffer, idx, 20)).trim();
    		logger.info("STR CD=" + storeCD);
    		idx += 20;
    		// GW ID
    		int gwID = ByteUtils.toUnsignedShort(Utils.getBytes(dataBuffer, idx, 2));
    		logger.info("GW ID=" + gwID);
    		idx += 2;
    		// 일출시간
    		int sunRiseTime = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("SunRise Time=" + sunRiseTime);
    		// 일출분
    		int sunRiseMinute = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("SunRise Minute=" + sunRiseMinute);
    		// 일몰시간
    		int sunSetTime = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("SunSet Time=" + sunSetTime);
    		// 일몰분
    		int sunSetMinute = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("SunSet Minute=" + sunSetMinute);
    		//  계약전력
    		long contractPower = ByteUtils.toUnsignedInt(Utils.getBytes(dataBuffer, idx, 4));
    		logger.info("Contract Power=" + contractPower);
    		idx += 4;
    		// 냉난방기 제조사
    		int hacManufacture = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("HAC Manufacture=" + hacManufacture);
    		// 1월 냉난방정책
    		int hac1MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("1월 냉난방정책=" + hac1MPolicy);
    		// 2월 냉난방정책
    		int hac2MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("2월 냉난방정책=" + hac2MPolicy);
    		// 3월 냉난방정책
    		int hac3MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("3월 냉난방정책=" + hac3MPolicy);
    		// 4월 냉난방정책
    		int hac4MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("4월 냉난방정책=" + hac4MPolicy);
    		// 5월 냉난방정책
    		int hac5MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("5월 냉난방정책=" + hac5MPolicy);
    		// 6월 냉난방정책
    		int hac6MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("6월 냉난방정책=" + hac6MPolicy);
    		// 7월 냉난방정책
    		int hac7MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("7월 냉난방정책=" + hac7MPolicy);
    		// 8월 냉난방정책
    		int hac8MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("8월 냉난방정책=" + hac8MPolicy);
    		// 9월 냉난방정책
    		int hac9MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("9월 냉난방정책=" + hac9MPolicy);
    		// 10월 냉난방정책
    		int hac10MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("10월 냉난방정책=" + hac10MPolicy);
    		// 11월 냉난방정책
    		int hac11MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("11월 냉난방정책=" + hac11MPolicy);
    		// 12월 냉난방정책
    		int hac12MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("12월 냉난방정책=" + hac12MPolicy);
    		// 1월 냉난방권장온도
    		int hac1MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("1월 냉난방권장온도=" + hac1MTemp);
    		// 2월 냉난방권장온도
    		int hac2MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("2월 냉난방권장온도=" + hac2MTemp);
    		// 3월 냉난방권장온도
    		int hac3MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("3월 냉난방권장온도=" + hac3MTemp);
    		// 4월 냉난방권장온도
    		int hac4MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("4월 냉난방권장온도=" + hac4MTemp);
    		// 5월 냉난방권장온도
    		int hac5MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("5월 냉난방권장온도=" + hac5MTemp);
    		// 6월 냉난방권장온도
    		int hac6MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("6월 냉난방권장온도=" + hac6MTemp);
    		// 7월 냉난방권장온도
    		int hac7MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("7월 냉난방권장온도=" + hac7MTemp);
    		// 8월 냉난방권장온도
    		int hac8MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("8월 냉난방권장온도=" + hac8MTemp);
    		// 9월 냉난방권장온도
    		int hac9MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("9월 냉난방권장온도=" + hac9MTemp);
    		// 10월 냉난방권장온도
    		int hac10MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("10월 냉난방권장온도=" + hac10MTemp);
    		// 11월 냉난방권장온도
    		int hac11MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("11월 냉난방권장온도=" + hac11MTemp);
    		// 12월 냉난방권장온도
    		int hac12MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
    		logger.info("12월 냉난방권장온도=" + hac12MTemp);
    		// IF Server IP
    		String serverIP = new String(Utils.getBytes(dataBuffer, idx, 15)).trim();
    		logger.info("IF Server IP=" + serverIP);
    		idx += 15;
    		// IF Server Port
    		int serverPort = ByteUtils.toUnsignedShort(Utils.getBytes(dataBuffer, idx, 2));
    		logger.info("IF Server Port=" + serverPort);
    		
    		// 지점정보 세팅
    		DynamicConfUtils.getInstance().doSetStoreInfo(storeCD, String.valueOf(gwID));
    		
    		// 일출/일몰시간 세팅
    		DynamicConfUtils.getInstance().doSetSunRisetInfo(String.valueOf(sunRiseTime), 
    				                                         String.valueOf(sunRiseMinute), 
    				                                         String.valueOf(sunSetTime), 
    				                                         String.valueOf(sunSetMinute));
    		
    		// 계약전력 세팅
    		DynamicConfUtils.getInstance().doSetContractPowerInfo(String.valueOf(contractPower));
    		
    		// 냉난방정책 세팅
    		DynamicConfUtils.getInstance().doSetHACMonthPolicyInfo(String.valueOf(hac1MPolicy), 
    				                                               String.valueOf(hac2MPolicy), 
    				                                               String.valueOf(hac3MPolicy), 
    				                                               String.valueOf(hac4MPolicy), 
    				                                               String.valueOf(hac5MPolicy), 
    				                                               String.valueOf(hac6MPolicy), 
    				                                               String.valueOf(hac7MPolicy), 
    															   String.valueOf(hac8MPolicy), 
    															   String.valueOf(hac9MPolicy), 
    															   String.valueOf(hac10MPolicy), 
    															   String.valueOf(hac11MPolicy), 
    															   String.valueOf(hac12MPolicy));
    		
    		// 냉난방권장온도 세팅
    		DynamicConfUtils.getInstance().doSetHACMonthTempInfo(String.valueOf(hac1MTemp), 
    				                                             String.valueOf(hac2MTemp), 
    				                                             String.valueOf(hac3MTemp), 
    				                                             String.valueOf(hac4MTemp), 
    				                                             String.valueOf(hac5MTemp), 
    				                                             String.valueOf(hac6MTemp), 
    				                                             String.valueOf(hac7MTemp), 
    															 String.valueOf(hac8MTemp), 
    															 String.valueOf(hac9MTemp), 
    															 String.valueOf(hac10MTemp), 
    															 String.valueOf(hac11MTemp), 
    															 String.valueOf(hac12MTemp));
    		
    		// IF Server 세팅
    		if(!"".equals(serverIP) && serverPort > 0){
    				
    			DynamicConfUtils.getInstance().doSetIFServer(serverIP, String.valueOf(serverPort));
	    		
	    		// Thread 중지 후 재접속 시도
	    		stop();
    		}
    		
    		// Data 전송 Timer 구동
    		resume();
			
		}else if(result == ERR_STX_ETX){
			logger.info("STX, EXT Error...");
		}else if(result == ERR_CRC){
			logger.info("CRC Error...");
		}else if(result == ERR_INVALID_DATA){
			logger.info("Invalid Data Error...");
		}else if(result == ERR_FILE_TRANS){
			logger.info("File Transfer Error...");
		}else if(result == ERR_CTRL){
			logger.info("Control Error...");
		}else if(result == ERR_EXCEPTION){
			logger.info("Exception Error...");
		}else{
			logger.info("Unknown Error...");
		}
    }
    
    /**
     * Data전송 결과 처리
     */
    private void doProcessCmd2(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	int idx = 1;
    	
    	byte result = dataBuffer[idx++];
		
		logger.info("RESULT="+result);
		
		if(result == NORMAL){
			logger.info("SUCCESS");
		}else if(result == ERR_STX_ETX){
			logger.info("STX, EXT Error...");
		}else if(result == ERR_CRC){
			logger.info("CRC Error...");
		}else if(result == ERR_INVALID_DATA){
			logger.info("Invalid Data Error...");
		}else if(result == ERR_FILE_TRANS){
			logger.info("File Transfer Error...");
		}else if(result == ERR_CTRL){
			logger.info("Control Error...");
		}else if(result == ERR_EXCEPTION){
			logger.info("Exception Error...");
		}else{
			logger.info("Unknown Error...");
		}
    }
    
    /**
     * 매장정보 처리
     */
    private void doProcessCmd3(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("매장정보 수신 처리");
    	
    	int idx = 1;
		
		// 매장코드
		String storeCD = new String(Utils.getBytes(dataBuffer, idx, 20)).trim();
		logger.info("STR CD=" + storeCD);
		idx += 20;
		// GW ID
		int gwID = ByteUtils.toUnsignedShort(Utils.getBytes(dataBuffer, idx, 2));
		logger.info("GW ID=" + gwID);
		idx += 2;
		//  계약전력
		long contractPower = ByteUtils.toUnsignedInt(Utils.getBytes(dataBuffer, idx, 4));
		logger.info("Contract Power=" + contractPower);
		
		// Validation
		byte result = NORMAL;
		
		// 지점정보 세팅
		DynamicConfUtils.getInstance().doSetStoreInfo(storeCD, String.valueOf(gwID));
		
		// 계약전력 세팅
		DynamicConfUtils.getInstance().doSetContractPowerInfo(String.valueOf(contractPower));
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[8];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x04);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = cmd;
		writeBuffer[wIdx++] = result;
		
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
		
		write(writeBuffer);
    }
    
    /**
     * 일출일몰정보 처리
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd5(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("일출일몰정보 수신 처리");
    	
    	int idx = 1;
		
    	// 일출시간
		int sunRiseTime = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("SunRise Time=" + sunRiseTime);
		// 일출분
		int sunRiseMinute = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("SunRise Minute=" + sunRiseMinute);
		// 일몰시간
		int sunSetTime = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("SunSet Time=" + sunSetTime);
		// 일몰분
		int sunSetMinute = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("SunSet Minute=" + sunSetMinute);
		
		// Validation
		byte result = NORMAL;
		
		// 일출/일몰시간 세팅
		DynamicConfUtils.getInstance().doSetSunRisetInfo(String.valueOf(sunRiseTime), 
				                                         String.valueOf(sunRiseMinute), 
				                                         String.valueOf(sunSetTime), 
				                                         String.valueOf(sunSetMinute));
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[8];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x04);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = cmd;
		writeBuffer[wIdx++] = result;
		
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
		
		write(writeBuffer);
    }
    
    /**
     * 냉난방정책 처리
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd6(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("냉난방정책 수신 처리");
    	
    	int idx = 1;
		
    	// 1월 냉난방정책
		int hac1MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("1월 냉난방정책=" + hac1MPolicy);
		// 2월 냉난방정책
		int hac2MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("2월 냉난방정책=" + hac2MPolicy);
		// 3월 냉난방정책
		int hac3MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("3월 냉난방정책=" + hac3MPolicy);
		// 4월 냉난방정책
		int hac4MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("4월 냉난방정책=" + hac4MPolicy);
		// 5월 냉난방정책
		int hac5MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("5월 냉난방정책=" + hac5MPolicy);
		// 6월 냉난방정책
		int hac6MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("6월 냉난방정책=" + hac6MPolicy);
		// 7월 냉난방정책
		int hac7MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("7월 냉난방정책=" + hac7MPolicy);
		// 8월 냉난방정책
		int hac8MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("8월 냉난방정책=" + hac8MPolicy);
		// 9월 냉난방정책
		int hac9MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("9월 냉난방정책=" + hac9MPolicy);
		// 10월 냉난방정책
		int hac10MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("10월 냉난방정책=" + hac10MPolicy);
		// 11월 냉난방정책
		int hac11MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("11월 냉난방정책=" + hac11MPolicy);
		// 12월 냉난방정책
		int hac12MPolicy = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("12월 냉난방정책=" + hac12MPolicy);
		
		// Validation
		byte result = NORMAL;
		
		// 냉난방정책 세팅
		DynamicConfUtils.getInstance().doSetHACMonthPolicyInfo(String.valueOf(hac1MPolicy), 
				                                               String.valueOf(hac2MPolicy), 
				                                               String.valueOf(hac3MPolicy), 
				                                               String.valueOf(hac4MPolicy), 
				                                               String.valueOf(hac5MPolicy), 
				                                               String.valueOf(hac6MPolicy), 
				                                               String.valueOf(hac7MPolicy), 
															   String.valueOf(hac8MPolicy), 
															   String.valueOf(hac9MPolicy), 
															   String.valueOf(hac10MPolicy), 
															   String.valueOf(hac11MPolicy), 
															   String.valueOf(hac12MPolicy));
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[8];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x04);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = cmd;
		writeBuffer[wIdx++] = result;
		
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
		
		write(writeBuffer);
    }
    
    /**
     * 냉난방 권장온도 처리
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd7(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("냉난방 권장온도 수신 처리");
    	
    	int idx = 1;
		
    	// 1월 냉난방권장온도
		int hac1MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("1월 냉난방권장온도=" + hac1MTemp);
		// 2월 냉난방권장온도
		int hac2MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("2월 냉난방권장온도=" + hac2MTemp);
		// 3월 냉난방권장온도
		int hac3MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("3월 냉난방권장온도=" + hac3MTemp);
		// 4월 냉난방권장온도
		int hac4MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("4월 냉난방권장온도=" + hac4MTemp);
		// 5월 냉난방권장온도
		int hac5MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("5월 냉난방권장온도=" + hac5MTemp);
		// 6월 냉난방권장온도
		int hac6MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("6월 냉난방권장온도=" + hac6MTemp);
		// 7월 냉난방권장온도
		int hac7MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("7월 냉난방권장온도=" + hac7MTemp);
		// 8월 냉난방권장온도
		int hac8MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("8월 냉난방권장온도=" + hac8MTemp);
		// 9월 냉난방권장온도
		int hac9MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("9월 냉난방권장온도=" + hac9MTemp);
		// 10월 냉난방권장온도
		int hac10MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("10월 냉난방권장온도=" + hac10MTemp);
		// 11월 냉난방권장온도
		int hac11MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("11월 냉난방권장온도=" + hac11MTemp);
		// 12월 냉난방권장온도
		int hac12MTemp = ByteUtils.unsignedByte(dataBuffer[idx++]);
		logger.info("12월 냉난방권장온도=" + hac12MTemp);
		
		// Validation
		byte result = NORMAL;
		
		// 냉난방권장온도 세팅
		DynamicConfUtils.getInstance().doSetHACMonthTempInfo(String.valueOf(hac1MTemp), 
				                                             String.valueOf(hac2MTemp), 
				                                             String.valueOf(hac3MTemp), 
				                                             String.valueOf(hac4MTemp), 
				                                             String.valueOf(hac5MTemp), 
				                                             String.valueOf(hac6MTemp), 
				                                             String.valueOf(hac7MTemp), 
															 String.valueOf(hac8MTemp), 
															 String.valueOf(hac9MTemp), 
															 String.valueOf(hac10MTemp), 
															 String.valueOf(hac11MTemp), 
															 String.valueOf(hac12MTemp));
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[8];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x04);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = cmd;
		writeBuffer[wIdx++] = result;
		
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
		
		write(writeBuffer);
    }
}