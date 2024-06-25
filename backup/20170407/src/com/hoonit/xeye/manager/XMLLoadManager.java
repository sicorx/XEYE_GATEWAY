package com.hoonit.xeye.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.hoonit.xeye.device.DeviceType;

public class XMLLoadManager {
	
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static XMLLoadManager instance = new XMLLoadManager();
	
	private SAXBuilder builder;
	
	private List<Map<String, Object>> inventoryList;
	
	private List<String> overlappedCheckList;
	
	private List<String> overlappedList;
	
	private XMLLoadManager(){
		
		init();
	}
	
	public static XMLLoadManager getInstance(){
		
		if(instance == null){
			instance = new XMLLoadManager();
		}
		
		return instance;
	}
	
	public void init(){
		
		this.builder = new SAXBuilder();
		
		this.inventoryList = new ArrayList<Map<String, Object>>();
		
		this.overlappedCheckList = new ArrayList<String>();
		
		this.overlappedList = new ArrayList<String>();
		
		FilenameFilter xmlFileNameFilter = new FilenameFilter() {
			   
			@Override
            public boolean accept(File dir, String name) {
            	
				if(name.startsWith("ivt") && name.lastIndexOf('.') > 0){
					
					// get last index for '.' char
					int lastIndex = name.lastIndexOf('.');
                  
					// get extension
					String str = name.substring(lastIndex);
                  
					// match path name extension
					if(str.equals(".xml")){
						return true;
					}
               }
				
               return false;
            }
         };
		
		try{
			
			File inventoryPath = new File("resource/inventory");
			
			if(inventoryPath.exists()){
				
				logger.info("Loading XML files...");
				
				File[] xmlFiles = inventoryPath.listFiles(xmlFileNameFilter);
				
				for(File xmlFile : xmlFiles){
					doLoadXML(xmlFile);
				}
				
				doPrintXMLInfo();
				
			}else{
				logger.info("Inventory path is not exist...");
			}
			
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
	}
	
	public List<Map<String, Object>> getInventoryList() {
		return inventoryList;
	}

	public List<String> getOverlappedList() {
		return overlappedList;
	}

	private void doPrintXMLInfo(){

		if(logger.isDebugEnabled()){
			
			logger.debug("============ INVENTORY INFO ============");
			
			for(Map<String, Object> inventoryMap : inventoryList){
				
				logger.debug("NAME            : " + inventoryMap.get("NAME"));
				logger.debug("DESCRIPTION     : " + inventoryMap.get("DESCRIPTION"));
				logger.debug("READ_INTERVAL   : " + inventoryMap.get("READ_INTERVAL"));
				logger.debug("COM_TYPE        : " + inventoryMap.get("COM_TYPE"));
				
				if("2".equals((String)inventoryMap.get("COM_TYPE"))){
					logger.debug("IP   : " + inventoryMap.get("IP"));
					logger.debug("PORT : " + inventoryMap.get("PORT"));
				}else if("3".equals((String)inventoryMap.get("COM_TYPE"))){
					logger.debug("PORTNAME : " + inventoryMap.get("PORTNAME"));
					logger.debug("BAUDRATE : " + inventoryMap.get("BAUDRATE"));
					logger.debug("DATABITS : " + inventoryMap.get("DATABITS"));
					logger.debug("PARITY   : " + inventoryMap.get("PARITY"));
					logger.debug("STOPBITS : " + inventoryMap.get("STOPBITS"));
				}else if("4".equals((String)inventoryMap.get("COM_TYPE"))){
					logger.debug("IP   : " + inventoryMap.get("IP"));
					logger.debug("PORT : " + inventoryMap.get("PORT"));
				}
				
				List<Map<String, Object>> deviceList = (List<Map<String, Object>>)inventoryMap.get("DEVICE");
			
				for(Map<String, Object> deviceMap : deviceList){
					
					logger.debug("----------- DEVICE LIST -----------");
					
					String deviceOID  = (String)deviceMap.get("OID");
					String protocol   = (String)deviceMap.get("PROTOCOL");
					String type       = (String)deviceMap.get("TYPE");
					String useYN      = (String)deviceMap.get("USE_YN");
					String deviceName = (String)deviceMap.get("NAME");
					String objectName = (String)deviceMap.get("OBJECT");
					
					logger.debug("OID      : " + deviceOID);
					logger.debug("PROTOCOL : " + protocol);
					logger.debug("TYPE     : " + type);
					logger.debug("USE_YN   : " + useYN);
					logger.debug("NAME     : " + deviceName);
					logger.debug("OBJECT   : " + objectName);
					
					if(type.equals(String.valueOf(DeviceType.DI)) || type.equals(String.valueOf(DeviceType.DO))){
						
						List<Map<String, Object>> tagList = (List<Map<String, Object>>)deviceMap.get("TAG");
						
						logger.debug("------Tag Info------");
						
						for(Map<String, Object> tagMap : tagList){
							
							logger.debug("OID        : " + tagMap.get("OID"));
							logger.debug("NAME       : " + tagMap.get("NAME"));
							logger.debug("CHANNEL    : " + tagMap.get("CHANNEL"));
							logger.debug("MONITOR_YN : " + ((String)tagMap.get("MONITOR_YN")).toUpperCase());
							
							logger.debug("--------------------");
						}
					}else{
						
						logger.debug("UNIT_ID  : " + deviceMap.get("UNIT_ID"));
						logger.debug("CHANNEL  : " + deviceMap.get("CHANNEL"));
						logger.debug("BAUDRATE : " + deviceMap.get("BAUDRATE"));
						
						List<Map<String, Object>> tagList = (List<Map<String, Object>>)deviceMap.get("TAG");
						
						logger.debug("------Tag Info------");
						
						for(Map<String, Object> tagMap : tagList){
							
							logger.debug("OID        : " + tagMap.get("OID"));
							logger.debug("NAME       : " + tagMap.get("NAME"));
							logger.debug("ADDRESS    : " + tagMap.get("ADDRESS"));
							logger.debug("DATA_TYPE  : " + tagMap.get("DATA_TYPE"));
							logger.debug("ACCESS     : " + tagMap.get("ACCESS"));
							logger.debug("RATE       : " + tagMap.get("RATE"));
							logger.debug("MONITOR_YN : " + ((String)tagMap.get("MONITOR_YN")).toUpperCase());
							
							if(tagMap.get("BIT_TAG") != null){
								
								List<Map<String, Object>> bitTagList = (List<Map<String, Object>>)tagMap.get("BIT_TAG");
								
								logger.debug("----Bit Tag Info----");
								
								for(Map<String, Object> bitTagMap : bitTagList){
									
									logger.debug("OID        : " + bitTagMap.get("OID"));
									logger.debug("NAME       : " + bitTagMap.get("NAME"));
									logger.debug("TYPE       : " + bitTagMap.get("TYPE"));
									logger.debug("ACCESS     : " + bitTagMap.get("ACCESS"));
									logger.debug("MONITOR_YN : " + ((String)bitTagMap.get("MONITOR_YN")).toUpperCase());
									
									logger.debug("----------------");
								}
							}
							
							logger.debug("--------------------");
						}
					}
				}
			}
		}else{
			
			logger.info("============ INVENTORY INFO ============");
			
			for(Map<String, Object> inventoryMap : inventoryList){
				
				logger.info("NAME : " + inventoryMap.get("NAME"));
				
				List<Map<String, Object>> deviceList = (List<Map<String, Object>>)inventoryMap.get("DEVICE");
				
				logger.info("----------- DEVICE LIST -----------");
				
				for(Map<String, Object> deviceMap : deviceList){
					logger.info("NAME : " + (String)deviceMap.get("NAME"));
				}
			}
		}
	}

	/**
	 * 
	 * resource/inventory/device.xml loading and parsing
	 * 
	 * @throws Exception
	 */
	private void doLoadXML(File xmlFile) throws Exception {
		
		if(xmlFile.exists()){
			
			logger.info(xmlFile.getName() + " parsing...");
			
			parseXML(xmlFile);
			
		}else{
			
			Exception e = new Exception("Directory doesn't exist");
			
			throw e;
		}
	}
	
	private void doCheckOverlap(String ivtName, String deviceOID, String tagOID){
		
		if(overlappedCheckList.contains(deviceOID + "." + tagOID)){
			
			StringBuffer sb = new StringBuffer();
			sb.append("Inventory " + ivtName);
			sb.append("에 다음 OID ");
			sb.append(deviceOID);
			sb.append(".");
			sb.append(tagOID);
			sb.append(" 가 중복선언되었습니다.");
			
			overlappedList.add(sb.toString());
			
		}else{
			overlappedCheckList.add(deviceOID + "." + tagOID);
		}
		
	}
	
	private void parseXML(File xmlFile) throws Exception {
		
		try{
			
			FileInputStream fis = new FileInputStream(xmlFile);
			
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			
			Document document = builder.build(isr);
			
			Element rootElement = document.getRootElement();
			
			String ivtName         = xmlFile.getName().substring(0, xmlFile.getName().lastIndexOf("."));
			String description     = rootElement.getChild("description").getTextTrim();
			String readInterval    = rootElement.getChild("read_interval").getTextTrim();
			Element comTypeElement = rootElement.getChild("com_type");
			String comType         = comTypeElement.getTextTrim();
			
			Map<String, Object> inventoryMap = new HashMap<String, Object>();
			inventoryMap.put("NAME", ivtName);
			inventoryMap.put("DESCRIPTION", description);
			inventoryMap.put("READ_INTERVAL", readInterval);
			inventoryMap.put("COM_TYPE", comType);
			
			// TCP 이면
			if("2".equals(comType)){
				
				Element tcpElement = rootElement.getChild("host");
				
				String ip   = tcpElement.getAttributeValue("ip").trim();
				String port = tcpElement.getAttributeValue("port").trim();
				
				inventoryMap.put("IP", ip);
				inventoryMap.put("PORT", port);
			}
			// SERIAL 이면
			else if("3".equals(comType)){
				
				Element serialElement = rootElement.getChild("serial");
				
				String portName = serialElement.getAttributeValue("port_name").trim();
				String baudrate = serialElement.getAttributeValue("baudrate").trim();
				String databits = serialElement.getAttributeValue("databits").trim();
				String parity   = serialElement.getAttributeValue("parity").trim();
				String stopbits = serialElement.getAttributeValue("stopbits").trim();
				
				inventoryMap.put("PORTNAME", portName);
				inventoryMap.put("BAUDRATE", baudrate);
				inventoryMap.put("DATABITS", databits);
				inventoryMap.put("PARITY", parity);
				inventoryMap.put("STOPBITS", stopbits);
			}
			// BACNet 이면
			else if("4".equals(comType)){
				
				Element tcpElement = rootElement.getChild("host");
				
				String ip   = tcpElement.getAttributeValue("ip").trim();
				String port = tcpElement.getAttributeValue("port").trim();
				
				inventoryMap.put("IP", ip);
				inventoryMap.put("PORT", port);
			}
			
			List<Map<String, Object>> deviceArr = new ArrayList<Map<String, Object>>();
			
			List<Element> deviceList = rootElement.getChildren("device");
			
			for(Element deviceElement : deviceList){
				
				Map<String, Object> deviceMap = new HashMap<String, Object>();
				
				String deviceOID   = deviceElement.getAttributeValue("oid").trim();
				String protocol    = deviceElement.getAttributeValue("protocol").trim();
				String type        = deviceElement.getAttributeValue("type").trim();
				String useYN       = deviceElement.getAttributeValue("use_yn").trim();
				String deviceName  = deviceElement.getChild("name").getTextTrim();
				String objectName  = deviceElement.getChild("object").getTextTrim();
				
				deviceMap.put("OID", deviceOID);
				deviceMap.put("PROTOCOL", protocol);
				deviceMap.put("TYPE", type);
				deviceMap.put("USE_YN", useYN);
				deviceMap.put("NAME", deviceName);
				deviceMap.put("OBJECT", objectName);
							
				int deviceType = Integer.parseInt(type);
				
				// Neid DI, DO 이면
				if(deviceType == DeviceType.DI || deviceType == DeviceType.DO){
					
					Element tagsElement = deviceElement.getChild("tags");
					
					if(tagsElement != null){
						
						List<Map<String, Object>> tagArr = new ArrayList<Map<String, Object>>();
						
						List<Element> tagList = tagsElement.getChildren("tag");
						
						for(Element tagElement : tagList){
							
							String tagOID    = tagElement.getAttributeValue("oid").trim();
							String name      = tagElement.getChild("name").getTextTrim();
							String channel   = tagElement.getAttributeValue("channel").trim();
							String monitorYN = tagElement.getAttributeValue("monitor_yn").trim();
							
							Map<String, Object> tagMap = new HashMap<String, Object>();
							
							tagMap.put("OID", tagOID);
							tagMap.put("NAME", name);
							tagMap.put("CHANNEL", channel);
							tagMap.put("MONITOR_YN", monitorYN);
							
							tagArr.add(tagMap);
							
							// SNMP OID 중복 체크
							doCheckOverlap(ivtName, deviceOID, tagOID);
						}
						
						deviceMap.put("TAG", tagArr);
					}
				}
				// Modbus, BACNet 장비
				else{
					
					String unitID   = deviceElement.getAttributeValue("unit_id").trim();
					String channel  = deviceElement.getAttributeValue("channel").trim();
					String baudRate = deviceElement.getAttributeValue("baud_rate").trim();
					
					deviceMap.put("UNIT_ID", unitID);
					deviceMap.put("CHANNEL", channel);
					deviceMap.put("BAUDRATE", baudRate);
					
					Element tagsElement = deviceElement.getChild("tags");
					
					if(tagsElement != null){
						
						List<Map<String, Object>> tagArr = new ArrayList<Map<String, Object>>();
						
						List<Element> tagList = tagsElement.getChildren("tag");
						
						for(Element tagElement : tagList){
							
							String tagOID    = tagElement.getAttributeValue("oid").trim();
							String name      = tagElement.getChild("name").getTextTrim();
							String address   = tagElement.getAttributeValue("address").trim();
							String dataType  = tagElement.getAttributeValue("data_type").trim();
							String access    = tagElement.getAttributeValue("access").trim();
							String rate      = tagElement.getAttributeValue("rate").trim();
							String monitorYN = tagElement.getAttributeValue("monitor_yn").trim();
							
							Map<String, Object> tagMap = new HashMap<String, Object>();
							
							tagMap.put("OID", tagOID);
							tagMap.put("NAME", name);
							tagMap.put("ADDRESS", address);
							tagMap.put("DATA_TYPE", dataType);
							tagMap.put("ACCESS", access);
							tagMap.put("RATE", rate);
							tagMap.put("MONITOR_YN", monitorYN);
							
							// 서브 태그가 존재하면
							if(tagElement.getChild("bit_tags") != null){
								
								List<Map<String, Object>> bitTagArr = new ArrayList<Map<String, Object>>();
								
								Element bitTagsElement = tagElement.getChild("bit_tags");
								
								List<Element> bitTagList = bitTagsElement.getChildren("bit_tag");
								
								for(Element bitTagElement : bitTagList){
									
									Map<String, Object> bitTagMap = new HashMap<String, Object>();
									
									String bitTagOID    = bitTagElement.getAttributeValue("oid");
									String bitType      = bitTagElement.getAttributeValue("type");
									String bitAccess    = bitTagElement.getAttributeValue("access");
									//String bitRate      = bitTagElement.getAttributeValue("rate");
									String bitMonitorYN = bitTagElement.getAttributeValue("monitor_yn");
									String bitTagName   = bitTagElement.getChild("name").getTextTrim();
									
									bitTagMap.put("OID", bitTagOID);
									bitTagMap.put("NAME", bitTagName);
									bitTagMap.put("TYPE", bitType);
									bitTagMap.put("ACCESS", bitAccess);
									//bitTagMap.put("RATE", bitRate);
									bitTagMap.put("MONITOR_YN", bitMonitorYN);
									bitTagMap.put("PARENT_ADDRESS", address);
									bitTagMap.put("PARENT_DATA_TYPE", dataType);
									
									bitTagArr.add(bitTagMap);
									
									// SNMP OID 중복 체크
									doCheckOverlap(ivtName, deviceOID, bitTagOID);
								}
								
								tagMap.put("BIT_TAG", bitTagArr);
							}else{
								
								// SNMP OID 중복 체크
								doCheckOverlap(ivtName, deviceOID, tagOID);
							}
							
							tagArr.add(tagMap);
						}
						
						deviceMap.put("TAG", tagArr);
					}
				}
				
				deviceArr.add(deviceMap);
				
			} // end for device
			
			inventoryMap.put("DEVICE", deviceArr);
			
			this.inventoryList.add(inventoryMap);
			
		}catch(Exception e){
			Exception ne = new Exception("The error is occurred during parsing the file " + xmlFile.getParent() + File.separator + xmlFile.getName());
			throw ne;
		}
		
	}
	
	public static void main(String[] args){
		
		List<Map<String, Object>> inventoryList = XMLLoadManager.getInstance().getInventoryList();

	}
}
