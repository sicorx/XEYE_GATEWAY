package com.hoonit.xeye.manager;

import org.apache.log4j.Logger;

import com.hoonit.xeye.net.socket.IFClient;

public class IFClientProxyManager {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static IFClientProxyManager instance = new IFClientProxyManager();
	
	private IFClient ifClient;
	
	private IFClientProxyManager(){
	}
	
	public static IFClientProxyManager getInstance(){
		
		if(instance == null){
			instance = new IFClientProxyManager();
		}
		
		return instance;
	}

	public IFClient getIFClient() {
		return ifClient;
	}

	public void setIFClient(IFClient ifClient) {
		this.ifClient = ifClient;
	}
}
