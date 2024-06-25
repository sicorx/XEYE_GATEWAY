package com.hoonit.xeye.net.socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.hoonit.xeye.manager.EnterpriseOIDManager;
import com.hoonit.xeye.manager.IFClientProxyManager;
import com.hoonit.xeye.manager.SNMPAgentProxyManager;
import com.hoonit.xeye.util.ByteUtils;
import com.hoonit.xeye.util.CRC16;
import com.hoonit.xeye.util.IOBoardUtils;
import com.hoonit.xeye.util.Utils;  
  
/** 
 * 패치파일 통신처리를 담당하는 객체
 */  
public class FileClient{
    
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
    private ClientThread clientThread;
    
    private IFClient ifClient;
    
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
    	
    	this.ifClient = IFClientProxyManager.getInstance().getIFClient();
    	this.host     = host;
    	this.port     = port;
    	this.storeCD  = storeCD;
    	this.gwID     = gwID;
    	
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
        private File doReceiveFile() throws IOException, Exception{
        	
        	logger.info("파일정보 수신");
        	
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
        	logger.info("File Size="+fileSize);
        	
        	byte[] fileNameBytes = Utils.getBytes(dataBuffer, idx, dataBuffer.length - 15); // STX+LEN+CMD+File Size+CRC+ETX = 15 
			String fileName = new String(fileNameBytes, "UTF-8");
			fileName = fileName.trim();
			
			String fileNm = fileName.substring(0, fileName.lastIndexOf("."));
			String ext = fileName.substring(fileName.lastIndexOf(".")+1);
			
			Calendar cal = Calendar.getInstance();
			int year   = cal.get(Calendar.YEAR);
			int month  = cal.get(Calendar.MONTH) + 1;
			int date   = cal.get(Calendar.DATE);
			int hour   = cal.get(Calendar.HOUR_OF_DAY);
			int minute = cal.get(Calendar.MINUTE);
			int second = cal.get(Calendar.SECOND);
			
			fileName = new StringBuilder()
					.append(fileNm)
					.append("_")
					.append(year)
					.append("_")
					.append(month)
					.append("_")
					.append(date)
					.append("_")
					.append(hour)
					.append("_")
					.append(minute)
					.append("_")
					.append(second)
					.append(".")
					.append(ext)
					.toString();
			
			logger.info("File Name="+fileName);
			
			////////////// 파일수신 부분 /////////////
			
			logger.info("파일 수신");
			
			File file = new File("resource/upload/"+fileName);
			
			BufferedOutputStream bos = null;
			
			try{
				
				read = new byte[1024];
	
				bos = new BufferedOutputStream(new FileOutputStream(file));
	
				int fileReadBytes = 0;
				long totalFileReadBytes = 0;
	
				while ((fileReadBytes = in.read(read)) != -1) {
	
					bos.write(read, 0, fileReadBytes);
					bos.flush();
	
					totalFileReadBytes += fileReadBytes;
	
					if(totalFileReadBytes >= fileSize){
						logger.info("File download is completed!");
						break;
					}
				}
	
				bos.close();
				
			}catch(IOException e){
				throw e;
			}finally{
				if(bos != null) bos.close();
			}
			
			return file;
        }
        
        /**
         * 배포
         * @param file
         */
        private void doDeploy(File zipFile) throws Throwable{
        	
        	logger.info("Deploy start...");
        	
        	if(zipFile != null){
        		
        		if(zipFile.exists()){
        			
        			deCompress(zipFile);
        			
        			logger.info("UnZip completed...");
        			
        			String fileNm = zipFile.getName().substring(0, zipFile.getName().lastIndexOf("."));
        			File file = new File(zipFile.getParent(), fileNm);
        			
        			if(file.exists()){
        				
        				// 처음일 경우 데이터 전송시간을 피하고 시간을 확보하기 위헤
						// 분이 1,6,11,16,21,26,31,36,41,46,51,56 일때만 실행한다.
						/*while(true){
							
							Calendar cal = Calendar.getInstance();
							int minute = cal.get(Calendar.MINUTE);
							
							logger.info("Current minute="+minute);
							
							if(minute == 1 || 
									minute == 6 ||
									minute == 11 ||
									minute == 16 ||
									minute == 21 ||
									minute == 26 ||
									minute == 31 ||
									minute == 36 ||
									minute == 41 ||
									minute == 46 ||
									minute == 51 ||
									minute == 56){
								
								break;
							}
							
							try{
								logger.info("Waiting the deploy minute!");
								Thread.sleep(5000);
							}catch(Exception e){
								logger.error(e.getMessage(), e);
							}
						}*/
						
        				File deployXMLFile = new File(file, "deploy.xml");
        				
        				if(deployXMLFile.exists()){
        					
        					SAXBuilder builder    = new SAXBuilder();
        					FileInputStream fis   = new FileInputStream(deployXMLFile);
        					InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        					Document document     = builder.build(isr);
        					
        					Element rootElement = document.getRootElement();
        					
        					List<Element> fileList = rootElement.getChildren("file");
        					
        					// G/W 파일만 적용
        					for(Element fileElement : fileList){
        						
        						String type       = fileElement.getChild("type").getTextTrim();
        						String fileName   = fileElement.getChild("name").getTextTrim();
        						String targetPath = fileElement.getChild("target_path").getTextTrim();
        						
        						// G/W 적용파일일 경우
        						if("G".equals(type)){
        							
        							logger.info("type="+type);
            						logger.info("name="+fileName);
            						logger.info("path="+targetPath);
        							
        							File[] files = file.listFiles();
        							
        							for(File deployFile : files){
        							
        								String ext = deployFile.getName().substring(deployFile.getName().lastIndexOf(".")+1);
        								
        								if(!"xml".equals(ext)){
        									
        									if(fileName.equals(deployFile.getName())){
        									
        										BufferedInputStream bis  = null;
        										BufferedOutputStream bos = null;
        									 
        										try{
		        							
        											bis = new BufferedInputStream(new FileInputStream(deployFile));
        										 
        											File target = new File(targetPath, fileName);
			        							
        											bos = new BufferedOutputStream(new FileOutputStream(target));
        											
        											int i =0;
        											
        											while( (i=bis.read()) != -1){
        								                bos.write(i);
        								            }
			        							
        										}catch(IOException e){
        											logger.error(e.getMessage(), e);
        										}finally{
        											if(bis != null) try{bis.close();}catch(IOException e){}
        											if(bos != null) try{bos.close();}catch(IOException e){}
        											
        											logger.info(fileName + " is patched!");
        										}
        										
        										break;
        									}
        								}
        							}
        						}
        					} // end for
        					
        					// I/O 보드 파일만 적용
        					for(Element fileElement : fileList){
        						
        						String type       = fileElement.getChild("type").getTextTrim();
        						String fileName   = fileElement.getChild("name").getTextTrim();
        						String targetPath = fileElement.getChild("target_path").getTextTrim();
        						
        						// I/O 적용파일일 경우
        						if("I".equals(type)){
        							
        							logger.info("type="+type);
            						logger.info("name="+fileName);
            						
        							File[] files = file.listFiles();
        							
        							for(File deployFile : files){
        							
        								String ext = deployFile.getName().substring(deployFile.getName().lastIndexOf(".")+1);
        								
        								if(!"xml".equals(ext)){
        									
        									if(fileName.equals(deployFile.getName())){
        										
        										logger.info("I/O deploy file path="+deployFile.getAbsolutePath());
        										        										
        										// /dev/ttyS0 포트로 파일을 업로드해야하기 때문에 통신 및 Serial Port를 중단한다.
        										ifClient.getMain().doStopCommDevice();
        										
        										try{
        											
        											// 18번 high
        											IOBoardUtils.getInstance().getPin1().high();
        											
        											Thread.sleep(500);
        											
        											// 17번 high
        											IOBoardUtils.getInstance().getPin0().high();
        											
        											Thread.sleep(500);
        											
        											// 17번 low
        											IOBoardUtils.getInstance().getPin0().low();
        											
        											Thread.sleep(500);
        											
        											// 파일전송
        											/*Process process   = null;
        											BufferedReader br = null;
        											
        											// -b : baud rate
        											// -m : data bit+flow control+stop bit
        											String[] cmd = {"stm32flash", "-b", "115200", "-m", "8n1" , "-w", deployFile.getAbsolutePath(), "-v", "/dev/ttyS0"};
        											
        											try{
        												
        												logger.info("upload file...");
        												
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
        												
        												logger.info("upload completed...");
        												
        											}catch(Exception e){
        												logger.error(e.getMessage(), e);
        											}finally{
        												try{
        													if(br != null) br.close();
        													if(process != null) process.destroy();
        												}catch(Exception e){
        													logger.error(e.getMessage(), e);
        												}
        											}*/
        											
        											Thread.sleep(500);
        											
        											// 18번 low
        											IOBoardUtils.getInstance().getPin1().low();
        											
        											Thread.sleep(500);
        											
        											// 17번 high
        											IOBoardUtils.getInstance().getPin0().high();
        											
        											Thread.sleep(500);
        											
        											// 17번 low
        											IOBoardUtils.getInstance().getPin0().low();
        											
        										}catch(Exception e){
        											logger.error(e.getMessage(), e);
        										}finally{
        											
        											if(IOBoardUtils.getInstance().getPin1().isHigh())
        												IOBoardUtils.getInstance().getPin1().low();
        											
        											if(IOBoardUtils.getInstance().getPin0().isHigh())
        												IOBoardUtils.getInstance().getPin0().low();
        										}
        										
        										break;
        									}
        								}
        							}
        						}
        					} // end for
        					
        					// Gateway restart
        					SNMPAgentProxyManager.getInstance().setStaticOIDValue(EnterpriseOIDManager.getEnterpriseOID() + ".1.12.0", "1");
        				}
        			}
        		}
        	}
        }
        
        /**
         * 압축해제
         * @param zipFile
         * @throws Throwable
         */
        private void deCompress(File zipFile) throws Throwable {
        	
        	String fileNm = zipFile.getName().substring(0, zipFile.getName().lastIndexOf("."));
        	
        	FileInputStream fis = null;
	        ZipInputStream zis = null;
	        ZipEntry zipentry = null;
	        try {
	            //파일 스트림
	            fis = new FileInputStream(zipFile);
	            //Zip 파일 스트림
	            zis = new ZipInputStream(fis);
	            //entry가 없을때까지 뽑기
	            while ((zipentry = zis.getNextEntry()) != null) {
	                String filename = zipentry.getName();
	                File file = new File(zipFile.getParent()+"/"+fileNm, filename);
	                //entiry가 폴더면 폴더 생성
	                if (zipentry.isDirectory()) {
	                    file.mkdirs();
	                } else {
	                    //파일이면 파일 만들기
	                    createFile(file, zis);
	                }
	            }
	        } catch (Throwable e) {
	            throw e;
	        } finally {
	            if (zis != null)
	                zis.close();
	            if (fis != null)
	                fis.close();
	        }
        }
        
        private void createFile(File file, ZipInputStream zis) throws Throwable {
            //디렉토리 확인
            File parentDir = new File(file.getParent());
            //디렉토리가 없으면 생성하자
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            //파일 스트림 선언
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[256];
                int size = 0;
                //Zip스트림으로부터 byte뽑아내기
                while ((size = zis.read(buffer)) > 0) {
                    //byte로 파일 만들기
                    fos.write(buffer, 0, size);
                }
            } catch (Throwable e) {
                throw e;
            }
        }
        
        @Override  
        public void run() {
        	
            super.run();
            
            File file = null;
            
            try{
            	
            	doConnectFileServer();
            	
            	file = doReceiveFile();
            	
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
            	
            	try{
            		doDeploy(file);
                }catch(Throwable e){
                	logger.error(e.getMessage(), e);
                }
            }
        }  
    }
}