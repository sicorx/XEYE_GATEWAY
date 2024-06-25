package com.hoonit.xeye.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class SystemTime {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	public SystemTime(){
	}
	
	public String getCurrentTime(){
		
		Process process   = null;
		BufferedReader br = null;
		
		String[] cmd = {"date", "+%F %R:%S"};
		
		String currentTime = "";
		
		try{
			
			// 프로세스 실행
			process = Runtime.getRuntime ().exec(cmd);
			process.waitFor();
			
			br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			String line;
			while (( line = br.readLine ()) != null){
				logger.info(line);
				
				if(line != null){
					currentTime = line;
				}
			}
			
			br.close();
			process.destroy();
			
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
		
		return currentTime;
	}
	
	public boolean setCurrentTime(String currentTime){
		
		boolean result = false;
		
		Process process   = null;
		BufferedReader br = null;
		
		String[] cmd = {"date", "-s", currentTime};
		
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
			
			result = true;
			
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
		
		return result;
	}
}
