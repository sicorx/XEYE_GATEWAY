package com.hoonit.xeye.net;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.hoonit.xeye.util.ResourceBundleHandler;

public class NetworkBase {

	protected Logger logger;
	
	private Lock writeLock = new ReentrantLock();
	
	//private Runtime instance = Runtime.getRuntime();
	
	protected long reconnectInterval;
	
	public NetworkBase(){
		this.reconnectInterval = Long.parseLong(ResourceBundleHandler.getInstance().getString("network.error.reconnect.interval"));
	}
	
	public void initLog(String logName, String logLevel, String maxFileSize, String maxBackupIndex){
		
		logger = Logger.getLogger(logName);
		
		Properties prop = new Properties();
		
		prop.setProperty("log4j.logger."+logName, logLevel+", "+logName+", stdout");
		
		prop.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
		prop.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
		prop.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%d{yy-MM-dd HH:mm:ss,SSS} %-5p %m%n");
		
		prop.setProperty("log4j.appender."+logName, "org.apache.log4j.RollingFileAppender");
		prop.setProperty("log4j.appender."+logName+".File", "./logs/"+logName+"/log4j.log");
		prop.setProperty("log4j.appender."+logName+".MaxFileSize", maxFileSize);
		prop.setProperty("log4j.appender."+logName+".MaxBackupIndex", maxBackupIndex);
		prop.setProperty("log4j.appender."+logName+".Append", "true");
		prop.setProperty("log4j.appender."+logName+".layout", "org.apache.log4j.PatternLayout");
		prop.setProperty("log4j.appender."+logName+".layout.ConversionPattern", "%d{yy-MM-dd HH:mm:ss,SSS} %C{1} %-5p %m%n");
		prop.setProperty("log4j.appender."+logName+".Threshold", logLevel);

		PropertyConfigurator.configure(prop);
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	public boolean tryWriteLock(){
    	
    	try{
    		return writeLock.tryLock(5000, TimeUnit.SECONDS);
    	}catch(InterruptedException e){
    		return false;
    	}
	}
	
	public void unWritelock(){
		writeLock.unlock();
	}
	
	/*protected void doPrintMemory(){
    	
    	logger.debug("==========Memory Info [MB]==========");
		// available memory
		logger.debug("Total Memory:" + instance.totalMemory() / (1024 * 1024));
		// Maximum available memory
		logger.debug("Max Memory:" + instance.maxMemory() / (1024 * 1024));
		// used memory
		logger.debug("Used Memory:" + (instance.totalMemory() - instance.freeMemory()) / (1024 * 1024));
		// free memory
		logger.debug("Free Memory:" + instance.freeMemory() / (1024 * 1024));
    }*/
}
