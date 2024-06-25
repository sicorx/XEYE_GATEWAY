package com.hoonit.xeye.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.log4j.Logger;

public class IPUtils {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static IPUtils instance = new IPUtils();
	
	private String etherIP = "";
	
	private String wlanIP = "";
	
	private String etherMacAddress = "";
	
	private String wlanMacAddress = "";
	
	private String subnetMask = "";
	
	private String gatewayIP = "";
	
	private IPUtils(){
		
		init();
	}
	
	public static IPUtils getInstance() {
    	
    	if(instance == null){
			instance = new IPUtils();
		}
		
		return instance;
    }
	
	public void init(){
		
		getNetworkInfo();
		
		if(!"".equals(etherIP)){
			getSubnetMaskInfo();
			
			getGatewayInfo();
		}
	}
	
	private void getNetworkInfo(){
		
		try{
			
			String ip = null;
			
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
			
			while(en.hasMoreElements()){
				
				NetworkInterface ni = en.nextElement();
				
				if("eth0".equals(ni.getDisplayName())){
				
					if(ni.isLoopback()){
						continue;
					}
					
					Enumeration<InetAddress> inetAddress = ni.getInetAddresses();
					
					while(inetAddress.hasMoreElements()){
						
						InetAddress ia = inetAddress.nextElement();
						
						if(ia.getHostAddress() != null && ia.getHostAddress().indexOf(".") != -1){
							
							ip = ia.getHostAddress();
							
							/*if(!"".equals(DynamicConfManager.getInstance().getAddress())){
								if(DynamicConfManager.getInstance().getAddress().equals(ip)){
									if("".equals(etherIP)){*/
										etherIP = ip;
										logger.info("Ethernet IP:" + ip);
									/*}
								}
							}*/
						}
					}
					
					byte[] mac = ni.getHardwareAddress();
					
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < mac.length; i++) {
						sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
					}
					
					if("eth0".equals(ni.getDisplayName())){
						etherMacAddress = sb.toString();
						logger.info("Ethernet MAC:" + etherMacAddress);
					}
				}
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	private void getSubnetMaskInfo(){
		
		Process process   = null;
		BufferedReader br = null;
		
		String[] cmd = {"ifconfig", "eth0"};
		
		try{
			
			// 프로세스 실행
			process = Runtime.getRuntime ().exec(cmd);
			process.waitFor();
			
			br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			String line;
			while (( line = br.readLine ()) != null){
				logger.info(line);
				if(line.contains("Mask")){
					subnetMask = line.substring(line.indexOf("Mask")+5);
				}
			}
			
			logger.info("Subnet Mask:" + subnetMask);
			
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
	}
	
	private void getGatewayInfo(){
		
		//gatewayIP = DynamicConfUtils.getInstance().getGateway();
		
		//if("".equals(gatewayIP)){
			
			Process process   = null;
			BufferedReader br = null;
			
			String[] cmd = {"ip", "route"};
			
			try{
				
				// 프로세스 실행
				process = Runtime.getRuntime ().exec(cmd);
				process.waitFor();
				
				br = new BufferedReader(new InputStreamReader(process.getInputStream()));
				
				String line;
				String gateway = "";
				while (( line = br.readLine ()) != null){
					
					logger.info(line);
					
					if(line.contains("default") && line.contains("via") && line.contains("eth0")){
						gateway = line.substring(line.indexOf("via")+4);
						gateway = gateway.substring(0, gateway.indexOf("dev")-1);
					}
				}
				
				logger.info("Gateway:" + gateway);
				
				gatewayIP = gateway;
				
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
		//}
	}

	public String getEtherIP() {
		return etherIP;
	}

	public void setEtherIP(String etherIP) {
		this.etherIP = etherIP;
	}

	public String getWlanIP() {
		return wlanIP;
	}

	public void setWlanIP(String wlanIP) {
		this.wlanIP = wlanIP;
	}

	public String getEtherMacAddress() {
		return etherMacAddress;
	}

	public void setEtherMacAddress(String etherMacAddress) {
		this.etherMacAddress = etherMacAddress;
	}

	public String getWlanMacAddress() {
		return wlanMacAddress;
	}

	public void setWlanMacAddress(String wlanMacAddress) {
		this.wlanMacAddress = wlanMacAddress;
	}

	public String getSubnetMask() {
		return subnetMask;
	}

	public void setSubnetMask(String subnetMask) {
		this.subnetMask = subnetMask;
	}

	public String getGatewayIP() {
		return gatewayIP;
	}

	public void setGatewayIP(String gatewayIP) {
		this.gatewayIP = gatewayIP;
	}
}
