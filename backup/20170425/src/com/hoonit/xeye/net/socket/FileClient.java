package com.hoonit.xeye.net.socket;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.DynamicConfUtils;
import com.hoonit.xeye.util.Utils;  
  
/** 
 * 패치파일 통신처리를 담당하는 객체
 */  
public class FileClient {
    
	protected final Logger logger = Logger.getLogger(getClass().getName());
    
	private boolean done = false;
	
    private ClientThread clientThread;
    
    private int bufferSize = 1024;
    
    private String host;
    
    private int port;
    
    private String storeCD;
    
    private String gwID;
    
    private String fileGubun;
    
    private short requestFileStep = 0;
    
	private final byte STX              = 0x02;
	private final byte ETX              = 0x03;
	
	private final byte NORMAL           = 0x01; // 정상
	private final byte ERR_STX_ETX      = 0x02; // STX, ETX 오류
	private final byte ERR_CRC          = 0x03; // CRC 오류
	private final byte ERR_INVALID_DATA = 0x04; // 유효하지 않은 데이터 오류
	private final byte ERR_FILE_TRANS   = 0x05; // 파일전송 오류
	private final byte ERR_CTRL         = 0x06; // 제어오류
	private final byte ERR_EXCEPTION    = 0x07; // Exception 발생 오류
	private final byte ERR_STR_NOEXIST  = 0x08; // 매장정보 미존재 오류
    
    public FileClient(String host, int port, String storeCD, String gwID, String fileGubun){
    	
    	this.bufferSize = DynamicConfUtils.getInstance().getClientBufferSize();
    	this.host       = host;
    	this.port       = port;
    	this.storeCD    = storeCD;
    	this.gwID       = gwID;
    	this.fileGubun  = fileGubun;
    	
    	logger.info("host      : " + host);
    	logger.info("port      : " + port);
    	logger.info("storeCD   : " + storeCD);
    	logger.info("gwID      : " + gwID);
    	logger.info("fileGubun : " + fileGubun);
    }
    
    /** 
     * start client
     *  
     * @param host
     * @param port
     */  
    public void start() {
        
    	logger.info("Client is starting...");
    	
        if (clientThread == null || !clientThread.isAlive()) {
            clientThread = new ClientThread(host, port);
            clientThread.start();
        }
    }
    
    /** 
     * stop client
     */  
    public void stopClient() {
    	
    	done = true;
    	
        if (clientThread != null && clientThread.isAlive()) {
            clientThread.interrupt();
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
        
        private String host;
        private int port;
        private SocketChannel client;
        
        /** 
         *  Constructor
         * @param abortable 
         * @param host 
         * @param port 
         */  
        public ClientThread(String host, int port) {
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
        	
    		try{
    			
    			StringBuffer sb = new StringBuffer();
        		for (int i = 0; i < b.length; i++) {
        			sb.append(ByteUtils.toHexString((byte) b[i])).append(" ");
        		}
        		logger.info("Write data : " + sb.toString());
        		
	            int len = client.write(ByteBuffer.wrap(b));
	            
	            logger.info("Writed data length : " + len);
	            
    		}catch(Exception e){
    			logger.error(e.getMessage(), e);
    		}
        }
  
        @Override  
        public void run() {
        	
            super.run();
            
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
                
                while (!Thread.interrupted() && !client.finishConnect()) {
                    Thread.sleep(10);
                }
                
                logger.info("File Client is connected");
                
                // File Server 접속요청
                doConnectFileServer();
    			
                short wIdx = 0;
                
                ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                
                byte cmd = 0;
                
                while (!Thread.interrupted() && !done) {
                    
                    selector.select(3000);
                    
                    Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                    
                    while (!Thread.interrupted() && !done && iter.hasNext()) {
                    	
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
	                    		if(cmd == 0x02){
	                    			
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
	        							
	        							resultBuffer[wIdx++] = 0x03;
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
	        							
	        							cmd = 0x00;
	        							
	        							if("3".equals(fileGubun)){
	        								
	        								if(requestFileStep == 2){
	        									
	        									stopClient();
	        									
	        									// IO 펌웨어 업로드 및 재시작
	        									// Gateway 재시작
	        								}else{
	        									// IO 파일 요청
	        									doRequestFile((byte)0x02);
	        								}
	        							}else{
	        								
	        								stopClient();
	        								
	        								// GW이면
	        								if("1".equals(fileGubun)){
	        									// Gateway 재시작
	        								}else{
	        									// IO 펌웨어 업로드 및 재시작
	        								}
	        							}
	                    			}
	                    			
	                    		}else{
                    			
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
		        							
		        							// File Server 접속정보 수신
		        							if(cmd == 0x01){
		        								doProcessCmd1(dataBuffer);
		        							}
		        							// 패치파일 수신
		        							else if(cmd == 0x02){
		        								
		        								logger.info("패치파일 수신");
		        								
		        								byte fileGubun       = dataBuffer[idx++]; // 1:GW, 2:IO
		        								byte[] fileSizeBytes = Utils.getBytes(dataBuffer, 2, 8);
		        								byte[] fileNameBytes = Utils.getBytes(dataBuffer, 10, dataBuffer.length-10); // 파일사이즈와 CMD 합친값을 빼면 파일명
		        								
		        								fileSize = ByteUtils.toLong(fileSizeBytes);
		        								String fileName = new String(fileNameBytes, "UTF-8");
		        								
		        								logger.info("파일구분="+fileGubun);
		        								logger.info("파일사이즈="+fileSize);
		        								logger.info("파일명="+fileName);
		        								
		        								if(fileSize > 0){
		        									
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
			    	                            	
		        								}else{
		        									// 파일 사이즈가 0이면 종료
		        									stopClient();
		        								}
		        							}
		        							
		        						}else{
			        						logger.info("CRC 오류");
		        						}
		        					}else{
			        					logger.info("STX, ETX 오류");
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
                
                logger.info("File Client is closed...");
            }
        }  
    }  
    
    /**
     * File Server 접속
     * @throws Exception
     */
    private void doConnectFileServer() throws Exception{
    	
        byte[] writeBuffer = new byte[29];
        
        short wIdx = 0;
        
        writeBuffer[wIdx++] = STX;  // STX
        
        // LEN
        byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x19);
        writeBuffer[wIdx++] = lenBytes[0];
        writeBuffer[wIdx++] = lenBytes[1];
        
        writeBuffer[wIdx++] = 0x01;    // CMD
        
        int storeCDLen = 20;
        
        // 매장코드
        try{
        	
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
        byte[] gwIDs = ByteUtils.toUnsignedShortBytes(Integer.parseInt(StringUtils.defaultIfEmpty(gwID, "0")));
        writeBuffer[wIdx++] = gwIDs[0];
        writeBuffer[wIdx++] = gwIDs[1];
		
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
		
		logger.info("File Server 접속 시도");
		
		write(writeBuffer);
    }
    
    /**
     * File Server 접속 응답 처리
     */
    private void doProcessCmd1(byte[] dataBuffer){
    	
    	logger.info("File Sever 접속결과 처리");
    	
    	int idx = 1;
    	
    	byte result = dataBuffer[idx++];
		
		logger.info("RESULT="+result);
		
		if(result == NORMAL){
			
			logger.info("SUCCESS");
			
			byte fg = 0x01;
			
			if("3".equals(fileGubun)){
				// GW 요청
				doRequestFile(fg);
			}else{
				doRequestFile(Byte.parseByte(fileGubun));
			}
			
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
     * 파일요청
     * @param fileGubun
     */
    private void doRequestFile(byte fileGubun){
    	
    	try{
    		
	    	byte[] writeBuffer = new byte[8];
	        
	        short wIdx = 0;
	        
	        writeBuffer[wIdx++] = STX;  // STX
	        
	        // LEN
	        byte[] lenBytes = ByteUtils.toUnsignedShortBytes(0x4);
	        writeBuffer[wIdx++] = lenBytes[0];
	        writeBuffer[wIdx++] = lenBytes[1];
	        
	        writeBuffer[wIdx++] = 0x02;    // CMD
	        
	        writeBuffer[wIdx++] = fileGubun;
			
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
			
			logger.info("File 요청");
			
			write(writeBuffer);
			
			requestFileStep++;
			
    	}catch(Exception e){
    		logger.error(e.getMessage(), e);
    	}
    }
}