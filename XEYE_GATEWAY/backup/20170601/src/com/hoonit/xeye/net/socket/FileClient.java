package com.hoonit.xeye.net.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.Utils;  
  
/** 
 * 패치파일 통신처리를 담당하는 객체
 */  
public class FileClient{
    
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
    private ClientThread clientThread;
    
    private String host;
    
    private int port;
    
    private String storeCD;
    
    private String gwID;
    
	private final byte STX              = 0x02;
	private final byte ETX              = 0x03;
	
	/*private final byte NORMAL           = 0x01; // 정상
	private final byte ERR_STX_ETX      = 0x02; // STX, ETX 오류
	private final byte ERR_CRC          = 0x03; // CRC 오류
	private final byte ERR_INVALID_DATA = 0x04; // 유효하지 않은 데이터 오류
	private final byte ERR_FILE_TRANS   = 0x05; // 파일전송 오류
	private final byte ERR_CTRL         = 0x06; // 제어오류
	private final byte ERR_EXCEPTION    = 0x07; // Exception 발생 오류
	private final byte ERR_STR_NOEXIST  = 0x08; // 매장정보 미존재 오류*/
    
    public FileClient(String host, int port, String storeCD, String gwID){
    	
    	this.host    = host;
    	this.port    = port;
    	this.storeCD = storeCD;
    	this.gwID    = gwID;
    	
    	logger.info("host    : " + host);
    	logger.info("port    : " + port);
    	logger.info("storeCD : " + storeCD);
    	logger.info("gwID    : " + gwID);
    }
    
    /** 
     * start client
     *  
     * @param host
     * @param port
     */  
    public void start() {
        
    	logger.info("File Client is starting...");
    	
        if (clientThread == null || !clientThread.isAlive()) {
            clientThread = new ClientThread(host, port);
            clientThread.start();
        }
    }
    
    /** 
     * Client Thread Object
     */  
    public class ClientThread extends Thread {
        
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        
        /** 
         *  Constructor
         * @param abortable 
         * @param host 
         * @param port 
         */  
        public ClientThread(String host, int port) {
        	
        	try{
        		
        		socket = new Socket(host, port);
        		in = new DataInputStream(socket.getInputStream());
        		out = new DataOutputStream(socket.getOutputStream());
        		
        	}catch(Exception e){
        		logger.error(e.getMessage(), e);
        	}
        }
        
        /**
         * File Server 접속
         * @throws Exception
         */
        private void doConnectFileServer() throws IOException, Exception{
        	
        	logger.info("File Server 접속");
            
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
    		
    		out.write(writeBuffer);
    		out.flush();
        }
        
        /**
         * 파일 수신
         * @throws IOException
         * @throws Exception
         */
        private void doReceiveFile() throws IOException, Exception{
        	
        	logger.info("파일 수신");
        	
        	byte[] read = new byte[200];

			int readBytes = in.read(read, 0, read.length);
			
			logger.info("readBytes="+readBytes);
        	
			byte[] dataBuffer = Utils.getBytes(read, 0, readBytes);
			
			short idx = 0;
			
			byte stx = dataBuffer[idx++];
			logger.info("STX="+stx);
			
			int len = ByteUtils.toUnsignedShort(Utils.getBytes(dataBuffer, idx, 2));
			idx += 2;
			logger.info("LEN="+len);
			
			byte cmd = dataBuffer[idx++];
			logger.info("CMD="+cmd);
			
			byte[] fileSizeBytes = Utils.getBytes(dataBuffer, idx, 8);
        	idx += 8;
        	long fileSize = ByteUtils.toLong(fileSizeBytes);
        	logger.info("파일사이즈="+fileSize);
        	
        	byte[] fileNameBytes = Utils.getBytes(dataBuffer, idx, dataBuffer.length-3);
			String fileName = new String(fileNameBytes, "UTF-8");
			fileName = fileName.trim();
			logger.info("파일명="+fileName);
        }
        
        @Override  
        public void run() {
        	
            super.run();
            
            try{
            	
            	doConnectFileServer();
            	
            	doReceiveFile();
            	
            }catch(IOException e){
            	logger.error(e.getMessage(), e);
            }catch(Exception e){
            	logger.error(e.getMessage(), e);
            }finally{
            	try{
            		if (in != null) in.close();
            		if (out != null) out.close();
            		if (socket != null) socket.close();
            	}catch(Exception e){
            		logger.error(e.getMessage(), e);
            	}
            }
        }  
    }
}