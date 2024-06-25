package com.hoonit.xeye.net.socket;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.hoonit.xeye.Main;
import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;
import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.DynamicConfUtils;
import com.hoonit.xeye.util.IPUtils;
import com.hoonit.xeye.util.Utils;  
  
/** 
 * IF Server 통신처리를 담당하는 객체
 */  
public class IFClient {
    
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private Main main;
	
	private Timer timer;
	
	private Lock writeLock = new ReentrantLock();
	
    private Abortable abortable = new Abortable();
    
    private ClientThread clientThread;
    
    private int bufferSize = 1024;
    
    private String host;
    
    private int port;
    
    private int delay;
    
    private long reConDelay = 10000; // IF Server 접속 재시도(10초)
    
    private static final byte STX              = 0x02;
	private static final byte ETX              = 0x03;
	
	private static final byte NORMAL           = 0x01; // 정상
	private static final byte ERR_STX_ETX      = 0x02; // STX, ETX 오류
	private static final byte ERR_CRC          = 0x03; // CRC 오류
	private static final byte ERR_INVALID_DATA = 0x04; // 유효하지 않은 데이터 오류
	private static final byte ERR_FILE_TRANS   = 0x05; // 파일전송 오류
	private static final byte ERR_CTRL         = 0x06; // 제어오류
	private static final byte ERR_EXCEPTION    = 0x07; // Exception 발생 오류
	private static final byte ERR_STR_NOEXIST  = 0x08; // 매장정보 미존재 오류
	
	// 최근 데이터 수신일자
	private long recentRecvTime = 0L;
	// 데이터 수신일자로부터 경과된시간
	private float elapsedTime = 0F;
	
    public IFClient(){
    	
    	this.bufferSize = DynamicConfUtils.getInstance().getClientBufferSize();
    	this.host       = DynamicConfUtils.getInstance().getIfServerHost();
    	this.port       = DynamicConfUtils.getInstance().getIfServerPort();
    	this.delay      = DynamicConfUtils.getInstance().getIfServerSendDelayTime();
    	
    	logger.info("host  : " + host);
    	logger.info("port  : " + port);
    	logger.info("delay : " + delay);
    }
    
    public void setMain(Main main){
    	this.main = main;
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
    	
    	// 매시간 5분마다 전송
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
    		
    		if(clientThread.getSocketChannel() != null)
    			return clientThread.getSocketChannel().isConnected();
    		else
    			return false;
    		
    	}else{
    		return false;
    	}
    }
    
    public float getElapsedTime(){
    	return elapsedTime;
    }
    
    /** 
     *  Socket write
     * @param text
     * @throws IOException
     */
    public void write(byte[] b) throws IOException {
    	
    	if(isSocketConnected())
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
            
            // IF Server에 동시에 많이 접속하는 걸 방지하기 위해
            // 난수(0~59초)만큼 Delay한 후 접속 시도
            try{
            	
            	Random random = new Random();
            	
            	Thread.sleep(random.nextInt(60) * 1000);
            	
            }catch(Exception e){
            	logger.error(e.getMessage(), e);
            }
              
            boolean done = false;
            
            Selector selector = null;
            
            // 데이터 수신 시간
        	recentRecvTime = 0L;
        	// 데이터 수신 경과시간
        	elapsedTime = 0F;
            
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
                        
                        // 데이터 수신 시간이 존재하면
                        if(recentRecvTime > 0){
                        	
                        	// 현재시간에서 최근에 받은 시간을 뺀 후 60초(1분)가 넘으면 종료한다.
                        	elapsedTime = (float)((System.currentTimeMillis() - recentRecvTime) / 1000.0);
                        	
                        	if( elapsedTime > (60 * 1) ){
                        		throw new IOException("Receive time is over 1 minute");
                        	}
                        }
                        
                        if (key.isReadable()) {
                        	
                        	buffer.clear();
                        	
                            int readBytes = client.read(buffer);
                            
                            if (readBytes < 0) {
                                done = true;
                                break;
                            } else if (readBytes == 0) {
                                continue;
                            }
                            
                            // 데이터 수신 시간
                        	recentRecvTime = System.currentTimeMillis();
                        	// 데이터 수신 경과시간
                        	elapsedTime = 0F;
                            
                            logger.info("read length : " + readBytes);
                            
                            buffer.flip();
                            
                            byte[] data = new byte[readBytes];
                            
                            short idx = 0;
                    		while (buffer.hasRemaining()) {
                    			data[idx++] = buffer.get();
                    		}
                    		
                    		try{
                    			
	                    		byte stx = data[0];
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
	        							
	        							// Echo 결과 수신
	        							if(cmd == 0x00){
	        								logger.info("Echo 전송 응답 수신");
	        							}
	        							// IF Sever 접속 결과 수신
	        							else if(cmd == 0x01){
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
	        							// 패치파일 업데이트 수신
	        							else if(cmd == 0x04){
	        								doProcessCmd4(cmd, dataBuffer);
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
	        							// 간판제어 수신
	        							else if(cmd == 0x08){
	        								doProcessCmd8(cmd, dataBuffer);
	        							}
	        							// 간판제어결과 전송 응답 수신
	        							else if(cmd == 0x09){
	        								doProcessCmd9(cmd, dataBuffer);
	        							}
	        							// 냉난방 온도제어결과 전송 응답 수신
	        							else if(cmd == 0x0A){
	        								doProcessCmd10(cmd, dataBuffer);
	        							}
	        							// 냉난방제어 수신
	        							else if(cmd == 0x0B){
	        								doProcessCmd11(cmd, dataBuffer);
	        							}
	        							// 전력피크알람 수신
	        							else if(cmd == 0x0C){
	        								doProcessCmd12(cmd, dataBuffer);
	        							}
	        							
	        						}else{
	        							
	        							// 응답이 아닐 경우에만 전송한다.
	        							// 0x01 : IF Server 접속정보 수신
	        							if(data[3] != 0x01 && data[3] != 0x09){
	        								
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
	        						if(data[3] != 0x01 && data[3] != 0x09){
	        							
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
    	
    	String viewStoreCD = StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getViewStoreCD(), "");
    	String storeCD = StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getStoreCD(), "");
    	
    	if("".equals(storeCD) && "".equals(viewStoreCD)){
    		
    		Thread.sleep(reConDelay);
    		
    		// IF Server 접속 재시도
    		doConnectIFServer();
    		
    	}else{
    		
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
	        	
	        	byte[] storeBytes = Utils.getFillNullByte(viewStoreCD.getBytes(), storeCDLen);
	        	
	        	if(!"".equals(storeCD)){
	        		storeBytes = Utils.getFillNullByte(storeCD.getBytes(), storeCDLen);
	        	}
	    		
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
    }
    
    /**
     * IF Sever 접속 응답 처리
     */
    private void doProcessCmd1(byte[] dataBuffer){
    	
    	logger.info("IF Sever 접속결과 처리");
    	
    	short idx = 1;
    	
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
    		DynamicConfUtils.getInstance().doSetHACMonthPolicyInfo(String.valueOf(hacManufacture),
    				                                               String.valueOf(hac1MPolicy), 
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
		}else if(result == ERR_STR_NOEXIST){
			logger.info("Store Info is not exist...");
			
			try{
				
				Thread.sleep(reConDelay);
			
				// IF Server 접속 재시도
	            doConnectIFServer();
	            
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
			
		}else{
			logger.info("Unknown Error...");
		}
    }
    
    /**
     * Data전송 결과 처리
     */
    private void doProcessCmd2(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	short idx = 1;
    	
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
    	
    	short idx = 1;
		
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
     * 패치파일 업데이트 처리
     */
    private void doProcessCmd4(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("패치파일 업데이트 처리");
    	
    	short idx = 1;
		
		// 파일구분
    	byte fileGubun = dataBuffer[idx++];
    	
    	// File Server IP
		String serverIP = new String(Utils.getBytes(dataBuffer, idx, 15)).trim();
		logger.info("File Server IP=" + serverIP);
		idx += 15;
		// File Server Port
		int serverPort = ByteUtils.toUnsignedShort(Utils.getBytes(dataBuffer, idx, 2));
		logger.info("File Server Port=" + serverPort);
		
		// Validation
		byte result = NORMAL;
		
		if(fileGubun != 0x01 && fileGubun != 0x02 && fileGubun != 0x03){
			result = ERR_INVALID_DATA;
		}
		
		if("".equals(serverIP)){
			result = ERR_INVALID_DATA;
		}
		
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
		
		if(result == NORMAL){
			
			String storeCD = DynamicConfUtils.getInstance().getStoreCD();
			String gwID = DynamicConfUtils.getInstance().getGwID();
			
			FileClient fc = new FileClient(serverIP, serverPort, storeCD, gwID, String.valueOf(fileGubun));
			fc.start();
		}
    }
    
    /**
     * 일출일몰정보 처리
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd5(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("일출일몰정보 수신 처리");
    	
    	short idx = 1;
		
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
    	
    	short idx = 1;
		
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
		DynamicConfUtils.getInstance().doSetHACMonthPolicyInfo("",
				                                               String.valueOf(hac1MPolicy), 
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
    	
    	short idx = 1;
		
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
    
    /**
     * 간판제어 처리
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd8(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("간판제어 수신 처리");
    	
    	Calendar cal = Calendar.getInstance();
		int year   = cal.get(Calendar.YEAR);
		int month  = cal.get(Calendar.MONTH) + 1;
		int date   = cal.get(Calendar.DATE);
		int hour   = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
    	
    	short idx = 1;
		
    	// 매장코드
		String storeCD = new String(Utils.getBytes(dataBuffer, idx, 20)).trim();
		logger.info("STR CD=" + storeCD);
		idx += 20;
		// GW ID
		int gwID = ByteUtils.toUnsignedShort(Utils.getBytes(dataBuffer, idx, 2));
		logger.info("GW ID=" + gwID);
		idx += 2;
		// 제어구분
		byte ctlGubun = dataBuffer[idx++];
		logger.info("Control Gubun=" + ctlGubun);
		
		// Validation
		byte result = NORMAL;
		
		// 기존 간판상태
		byte statusOld = 0x00;
		// 현재 간판상태
		byte status = 0x00;
		
		if(storeCD.equals(DynamicConfUtils.getInstance().getStoreCD()) && String.valueOf(gwID).equals(DynamicConfUtils.getInstance().getGwID())){
			
			List<String> list = new ArrayList<String>();
			list.add(EnterpriseOIDManager.getEnterpriseOID()+".10.0.0");
			list.add(EnterpriseOIDManager.getEnterpriseOID()+".20.2.0");
			list.add(EnterpriseOIDManager.getEnterpriseOID()+".20.3.0");
			
			Map<String, String> map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
			
			String commStatus = map.get(EnterpriseOIDManager.getEnterpriseOID()+".10.0.0"); // 통신상태
			
			// 통신이 정상이면
			if("0".equals(commStatus)){
				
				String status1    = map.get(EnterpriseOIDManager.getEnterpriseOID()+".20.2.0");
				String status2    = map.get(EnterpriseOIDManager.getEnterpriseOID()+".20.3.0");
				
				logger.info("기존간판1 상태="+status1);
				logger.info("기존간판2 상태="+status2);
				
				// 기존 간판상태
				if("0".equals(StringUtils.defaultIfEmpty(status1, "0")) && "0".equals(StringUtils.defaultIfEmpty(status2, "0"))){
					statusOld = 0x00;
				}else{
					statusOld = 0x01;
				}
				
				// 제어시작
				int res = SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID()+".20.1.0", String.valueOf(ctlGubun));
				
				logger.info("제어결과="+res);
				
				if(res == -1){
					result = ERR_CTRL;
				}
				
				list.remove(EnterpriseOIDManager.getEnterpriseOID()+".10.0.0");
				
				map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
				
				status1 = map.get(EnterpriseOIDManager.getEnterpriseOID()+".20.2.0");
				status2 = map.get(EnterpriseOIDManager.getEnterpriseOID()+".20.3.0");
				
				logger.info("현재간판1 상태="+status1);
				logger.info("현재간판2 상태="+status2);
				
				// 현재 간판상태
				if("0".equals(StringUtils.defaultIfEmpty(status1, "0")) && "0".equals(StringUtils.defaultIfEmpty(status2, "0"))){
					status = 0x00;
				}else{
					status = 0x01;
				}
			}else{
				logger.info("통신불량...");
				result = ERR_CTRL;
			}
		}else{
			result = ERR_INVALID_DATA;
		}
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[40];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x24);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = cmd;
		writeBuffer[wIdx++] = result;
		
		short storeCDLen = 20;
		storeCD = DynamicConfUtils.getInstance().getStoreCD();
		
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
        byte[] gwIDs = ByteUtils.toUnsignedShortBytes(Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getGwID(), "0")));
        writeBuffer[wIdx++] = gwIDs[0];
        writeBuffer[wIdx++] = gwIDs[1];
		
		// 년
        byte[] yearBytes = ByteUtils.toUnsignedShortBytes(year);
        writeBuffer[wIdx++] = yearBytes[0];
        writeBuffer[wIdx++] = yearBytes[1];
        // 월
        writeBuffer[wIdx++] = (byte)month;
        // 일
        writeBuffer[wIdx++] = (byte)date;
        // 시
        writeBuffer[wIdx++] = (byte)hour;
        // 분
        writeBuffer[wIdx++] = (byte)minute;
        // 초
        writeBuffer[wIdx++] = (byte)second;
		writeBuffer[wIdx++] = statusOld; // 기존 간판상태
		writeBuffer[wIdx++] = status;    // 현재 간판상태
		writeBuffer[wIdx++] = 0x01;      // 제어주체(0x00:SEMS, 0x01:Man)
		
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
     * 간판제어결과 전송 응답 수신
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd9(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("간판제어결과 전송 응답 수신 처리");
    	
    	short idx = 1;
		
    	byte result = dataBuffer[idx];
    	
    	logger.info("RESULT="+result);
    }
    
    /**
     * 냉난방 온도제어결과 전송 응답 수신
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd10(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("냉난방 온도제어결과 전송 응답 수신 처리");
    	
    	short idx = 1;
		
    	byte result = dataBuffer[idx];
    	
    	logger.info("RESULT="+result);
    }
    
    /**
     * 냉난방제어 처리
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd11(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("냉난방제어 수신 처리");
    	
    	Calendar cal = Calendar.getInstance();
		int year   = cal.get(Calendar.YEAR);
		int month  = cal.get(Calendar.MONTH) + 1;
		int date   = cal.get(Calendar.DATE);
		int hour   = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);
    	
    	short idx = 1;
		
    	// 매장코드
		String storeCD = new String(Utils.getBytes(dataBuffer, idx, 20)).trim();
		logger.info("STR CD=" + storeCD);
		idx += 20;
		// GW ID
		int gwID = ByteUtils.toUnsignedShort(Utils.getBytes(dataBuffer, idx, 2));
		logger.info("GW ID=" + gwID);
		idx += 2;
		// 하콘ID(0x00:전체, 0x01:하콘1…0x05:하콘5)
		byte haconID = dataBuffer[idx++];
		// 제조사(0x00:LG, 0x01:Samsung)
		byte manufacture = dataBuffer[idx++];
		logger.info("Manufacture=" + manufacture);
		// 냉난방(0x00:난방, 0x01:냉방)
		byte coolNheat = dataBuffer[idx++];
		logger.info("CoolNHeat=" + coolNheat);
		// 설정온도
		byte temp = dataBuffer[idx++];
		logger.info("Temperature=" + temp);
		// ON/FF(0x00:OFF, 0x01:ON)
		byte onOFF = dataBuffer[idx++];
		logger.info("ON/OFF=" + onOFF);
		
		// Validation
		byte result = NORMAL;
		
		if(storeCD.equals(DynamicConfUtils.getInstance().getStoreCD()) && String.valueOf(gwID).equals(DynamicConfUtils.getInstance().getGwID())){
			
			String commStatus = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID()+".10.0.0"); // 통신상태
			
			// 통신이 정상이면
			if("0".equals(commStatus)){
				
				// 전체이면
				if(haconID == 0x00){
					
					for(short i = 0; i < 5; i++){
						
						// 제조사
						SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".4.0", String.valueOf(manufacture));
						// 냉난방
						SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".5.0", String.valueOf(coolNheat));
						// 설정온도
						SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".6.0", String.valueOf(temp));
						// ON/OFF
						SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".7.0", String.valueOf(onOFF));
						// 제어
						int res = SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".8.0", "1");
						
						if(res == 0){
							result = NORMAL;
						}else{
							result = ERR_CTRL;
							break;
						}
					}
					
				}else{
					
					int i = Integer.parseInt(String.valueOf(haconID)) - 1;
					
					// 제조사
					SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".4.0", String.valueOf(manufacture));
					// 냉난방
					SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".5.0", String.valueOf(coolNheat));
					// 설정온도
					SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".6.0", String.valueOf(temp));
					// ON/OFF
					SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".7.0", String.valueOf(onOFF));
					// 제어
					int res = SNMPAgentProxyManager.getInstance().setOIDValue(EnterpriseOIDManager.getEnterpriseOID() + "."+(i+60)+".8.0", "1");
					
					if(res == 0){
						result = NORMAL;
					}else{
						result = ERR_CTRL;
					}
				}
				
			}else{
				logger.info("통신불량...");
				result = ERR_CTRL;
			}
		}else{
			result = ERR_INVALID_DATA;
		}
		
		
		// 결과 전송
		short wIdx = 0;
		byte[] writeBuffer = new byte[46];
		writeBuffer[wIdx++] = STX;
		byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x2A);
		writeBuffer[wIdx++] = lenBytes[0];
		writeBuffer[wIdx++] = lenBytes[1];
		writeBuffer[wIdx++] = 0x0B;
		writeBuffer[wIdx++] = result;
		
		// 매장코드
		short storeCDLen = 20;
		storeCD = DynamicConfUtils.getInstance().getStoreCD();
		
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
        byte[] gwIDs = ByteUtils.toUnsignedShortBytes(Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getGwID(), "0")));
        writeBuffer[wIdx++] = gwIDs[0];
        writeBuffer[wIdx++] = gwIDs[1];
		
		// 년
        byte[] yearBytes = ByteUtils.toUnsignedShortBytes(year);
        writeBuffer[wIdx++] = yearBytes[0];
        writeBuffer[wIdx++] = yearBytes[1];
        // 월
        writeBuffer[wIdx++] = (byte)month;
        // 일
        writeBuffer[wIdx++] = (byte)date;
        // 시
        writeBuffer[wIdx++] = (byte)hour;
        // 분
        writeBuffer[wIdx++] = (byte)minute;
        // 초
        writeBuffer[wIdx++] = (byte)second;
        // 하콘ID(0x00:전체, 0x01:하콘1…0x05:하콘5)
     	writeBuffer[wIdx++] = haconID; // 전체
        // 센싱온도
		writeBuffer[wIdx++] = (byte)getAverageTemp();
		// 제어온도
		if(result == NORMAL)
			writeBuffer[wIdx++] = temp;
		else
			writeBuffer[wIdx++] = 0x00;
		// 권장온도
		writeBuffer[wIdx++] = (byte)getHACTemp(month);
		// 주체(0x00:SEMS, 0x01:Man, 0x02:Peak)
		writeBuffer[wIdx++] = 0x01;
		// 제어종류(0:사람제어, 1:REMS ON, 2:REMS OFF,  3:REMS 온도 제어)
		writeBuffer[wIdx++] = 0x00;
		
		// ON/OFF(0x00:ON, 0x01:OFF)
		if(onOFF == 0x00)
			writeBuffer[wIdx++] = 0x01;
		else
			writeBuffer[wIdx++] = 0x00;
		
		// 냉난방(0x00:냉방, 0x01:난방)
		writeBuffer[wIdx++] = coolNheat;
		// 제어주체(0x00:SEMS, 0x01:Mobile)
		writeBuffer[wIdx++] = 0x01;
		
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
    
    // 티센서 평균온도
 	private int getAverageTemp(){
 		
 		int temp = 0, deviceCnt = 0;
 		
 		try{
 			
 			String bleConOIDVal = SNMPAgentProxyManager.getInstance().getOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".69.1.0"); // BLE연결 개수
 			short bleConCnt = Short.parseShort(StringUtils.defaultIfEmpty(bleConOIDVal, "0"));
 			
 			List<String> list = new ArrayList<String>();
 			
 			short x = 0;
 			
 			// 무선티센서가 존재하면
 			if(bleConCnt > 0){
 				
 				for(x = 0; x < 10; x++){
 					list.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".1.0"); // 구분
 					list.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".2.0"); // 온도
 				}
 				
 				Map<String, String> map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
 				
 				for(x = 0; x < 10; x++){
 					String gubun = map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".1.0");
 					
 					// Major가 1 이면 상온에 있는 센서
 					if(gubun.startsWith("1")){
 						temp += Integer.parseInt(StringUtils.defaultIfEmpty(map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+70)+".2.0"), "0"));
 						deviceCnt++;
 					}
 				}
 				
 				if(deviceCnt > 0 && temp > 0)
 					temp = (temp / deviceCnt);
 			}
 			// 무선티센서가 존재하지 않으면 유선티센서
 			else{
 				
 				for(x = 0; x < 5; x++){
 					list.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".1.0"); // 연결상태
 					list.add(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".3.0"); // 온도
 				}
 				
 				Map<String, String> map = SNMPAgentProxyManager.getInstance().getOIDValues(list);
 				
 				for(x = 0; x < 5; x++){
 					String gubun = map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".1.0");
 					
 					// 연결이 정상이면
 					if("0".equals(gubun)){
 						temp += Integer.parseInt(StringUtils.defaultIfEmpty(map.get(EnterpriseOIDManager.getEnterpriseOID() + "."+(x+50)+".3.0"), "0"));
 						deviceCnt++;
 					}
 				}
 				
 				if(deviceCnt > 0 && temp > 0)
 					temp = (temp / deviceCnt);
 			}
 			
 		}catch(Exception e){
 			logger.error(e.getMessage(), e);
 		}
 		
 		return temp;
 	}
 	
 	// 권장온도
 	private int getHACTemp(int month){
 		
 		int result;
 		
 		switch(month){
 			case 1 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac1MTemp(), "255"));
 				break;
 			case 2 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac2MTemp(), "255"));
 				break;
 			case 3 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac3MTemp(), "255"));
 				break;
 			case 4 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac4MTemp(), "255"));
 				break;
 			case 5 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac5MTemp(), "255"));
 				break;
 			case 6 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac6MTemp(), "255"));
 				break;
 			case 7 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac7MTemp(), "255"));
 				break;
 			case 8 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac8MTemp(), "255"));
 				break;
 			case 9 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac9MTemp(), "255"));
 				break;
 			case 10 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac10MTemp(), "255"));
 				break;
 			case 11 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac11MTemp(), "255"));
 				break;
 			case 12 : 
 				result = Integer.parseInt(StringUtils.defaultIfEmpty(DynamicConfUtils.getInstance().getHac12MTemp(), "255"));
 				break;
 			default : 
 				result = 255;
 				break;
 		}
 		
 		return result;
 	}
 	
 	/**
     * 전력피크알람 전송 응답 수신
     * @param cmd
     * @param dataBuffer
     * @throws Exception
     */
    private void doProcessCmd12(byte cmd, byte[] dataBuffer) throws Exception{
    	
    	logger.info("전력피크알람 전송 응답 수신 처리");
    	
    	short idx = 1;
		
    	byte result = dataBuffer[idx];
    	
    	logger.info("RESULT="+result);
    }
}