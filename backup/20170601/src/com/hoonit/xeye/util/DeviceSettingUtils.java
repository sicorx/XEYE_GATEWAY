package com.hoonit.xeye.util;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class DeviceSettingUtils {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private static DeviceSettingUtils instance = new DeviceSettingUtils();
	
	private DeviceSettingUtils(){
	}
	
	public static DeviceSettingUtils getInstance(){
		
		if(instance == null){
			instance = new DeviceSettingUtils();
		}
		
		return instance;
	}
	
	public void doSetDevice(String deviceInfo) throws Exception {
		
		JSONObject deviceObj = JSONObject.fromObject(deviceInfo);
		
		SAXBuilder builder = new SAXBuilder();
		
		File xmlFile = new File("resource/inventory/ivt2.xml");
		
		if(xmlFile.exists()){
		
			Document doc = builder.build(xmlFile);
			
			Element rootElement = doc.getRootElement();
			
			// 기존 device 삭제
			rootElement.removeChildren("device");
			
			// PMC
			doSetPMC(deviceObj, rootElement);
			
			// 간판
			doSetSign(deviceObj, rootElement);
			
			// 알몬
			doSetAlmon(deviceObj, rootElement);
			
			// 테몬
			doSetTemon(deviceObj, rootElement);
			
			// 티센서
			doSetTSensor(deviceObj, rootElement);
			
			XMLOutputter out = new XMLOutputter();
			out.setFormat(Format.getPrettyFormat());
			
			try{
				out.output(doc, new FileWriter(xmlFile));
			}catch(Exception e){
				logger.error(e.getMessage(), e);
			}
		}
	}
	
	// PMC
	private void doSetPMC(JSONObject deviceObj, Element rootElement) throws Exception{
		
		SAXBuilder builder = new SAXBuilder();
		
		File xmlFile = new File("resource/conf/template/pmc.xml");
		
		if(xmlFile.exists()){
			
			int pmcCnt = 0;
			
			if(deviceObj.get("pmc") != null){
				if(!"".equals(deviceObj.getString("pmc").trim())){
					pmcCnt = Integer.parseInt(deviceObj.getString("pmc"));
				}
			}
			
			int deviceOID = 10;
			
			if(pmcCnt > 0){
				
				for(int i = 0; i < pmcCnt; i++){
					
					Document doc = builder.build(xmlFile);
					
					Element templateRootElement = doc.getRootElement();
					
					Element templateDeviceElement = templateRootElement.getChild("device");
					templateDeviceElement.detach();
					
					Attribute oid = templateDeviceElement.getAttribute("oid");
					oid.setValue(String.valueOf(deviceOID++));
					
					Attribute useYN = templateDeviceElement.getAttribute("use_yn");
					
					/*if(deviceObj.get("pmc") == null){
						useYN.setValue("N");
					}else{
						useYN.setValue("Y");
					}*/
					useYN.setValue("Y");
					
					rootElement.addContent(templateDeviceElement);
				}
				
			}else{
				
				Document doc = builder.build(xmlFile);
				
				Element templateRootElement = doc.getRootElement();
				
				Element templateDeviceElement = templateRootElement.getChild("device");
				templateDeviceElement.detach();
				
				Attribute oid = templateDeviceElement.getAttribute("oid");
				oid.setValue(String.valueOf(deviceOID++));
				
				Attribute useYN = templateDeviceElement.getAttribute("use_yn");
				useYN.setValue("N");
				
				rootElement.addContent(templateDeviceElement);
			}
		}
	}
	
	// 간판
	private void doSetSign(JSONObject deviceObj, Element rootElement) throws Exception{
		
		SAXBuilder builder = new SAXBuilder();
		
		File xmlFile = new File("resource/conf/template/sign.xml");
		
		if(xmlFile.exists()){
			
			int signCnt = 1;
			
			if(deviceObj.get("sign") != null){
				if(!"".equals(deviceObj.getString("sign").trim())){
					signCnt = Integer.parseInt(deviceObj.getString("sign"));
				}
			}
			
			int deviceOID = 20;
			
			if(signCnt > 0){
				
				Document doc = builder.build(xmlFile);
				
				Element templateRootElement = doc.getRootElement();
				
				Element templateDeviceElement = templateRootElement.getChild("device");
				templateDeviceElement.detach();
				
				Attribute oid = templateDeviceElement.getAttribute("oid");
				oid.setValue(String.valueOf(deviceOID++));
				
				Attribute useYN = templateDeviceElement.getAttribute("use_yn");
				
				if(deviceObj.get("sign") == null){
					useYN.setValue("N");
				}else{
					useYN.setValue("Y");
				}
				
				rootElement.addContent(templateDeviceElement);
			}
		}
	}
	
	private JSONObject isChannelExist(JSONArray tagArr, int channel) throws Exception{
		
		JSONObject result = null;
		
		for(int i = 0; i < tagArr.size(); i++){
			
			JSONObject tagObj = (JSONObject)tagArr.get(i);
			
			if( channel == Integer.parseInt(tagObj.getString("channel").trim()) ){
				result = tagObj;
				break;
			}
		}
		
		return result;
	}
	
	// 알몬
	private void doSetAlmon(JSONObject deviceObj, Element rootElement) throws Exception{
		
		SAXBuilder builder = new SAXBuilder();
		
		File xmlFile = new File("resource/conf/template/almon.xml");
		
		if(xmlFile.exists()){
			
			int diCnt = 4;
			
			JSONArray tagArr = null;
			
			if(deviceObj.get("di") != null){
				tagArr = (JSONArray)deviceObj.get("di");
			}
			
			if(tagArr != null){
			
				int deviceOID = 30;
				
				Document doc = builder.build(xmlFile);
				
				Element templateRootElement = doc.getRootElement();
				
				Element templateDeviceElement = templateRootElement.getChild("device");
				templateDeviceElement.detach();
				
				Attribute oid = templateDeviceElement.getAttribute("oid");
				oid.setValue(String.valueOf(deviceOID));
				
				Attribute useYN = templateDeviceElement.getAttribute("use_yn");
				
				if(deviceObj.get("di") == null){
					useYN.setValue("N");
				}else{
					useYN.setValue("Y");
				}
				
				Element tagsElement = templateDeviceElement.getChild("tags");
				Element tagElement = (Element)tagsElement.getChildren().get(1);
				Element bitTagsElement = tagElement.getChild("bit_tags");
				
				for(int i = 0; i < diCnt; i++){
					
					JSONObject tagObj = isChannelExist(tagArr, (i+1));
					
					// Channel이 존재하면
					if(tagObj != null){
						
						Element bitTagElement = new Element("bit_tag");
						bitTagElement.setAttribute("oid", String.valueOf(i+1));
						bitTagElement.setAttribute("type", "D");
						bitTagElement.setAttribute("access", "R");
						bitTagElement.setAttribute("monitor_yn", "Y");
						Element bitTagNmElement = new Element("name");
						bitTagNmElement.addContent(new CDATA(tagObj.getString("tag_nm")));
						bitTagElement.addContent(bitTagNmElement);
						Element unitElement = new Element("unit");
						unitElement.addContent(new CDATA(""));
						bitTagElement.addContent(unitElement);
						bitTagsElement.addContent(bitTagElement);
						
					}else{
						
						Element bitTagElement = new Element("bit_tag");
						bitTagElement.setAttribute("oid", String.valueOf(i+1));
						bitTagElement.setAttribute("type", "D");
						bitTagElement.setAttribute("access", "R");
						bitTagElement.setAttribute("monitor_yn", "N");
						Element bitTagNmElement = new Element("name");
						bitTagNmElement.addContent(new CDATA(""));
						bitTagElement.addContent(bitTagNmElement);
						Element unitElement = new Element("unit");
						unitElement.addContent(new CDATA(""));
						bitTagElement.addContent(unitElement);
						bitTagsElement.addContent(bitTagElement);
					}
				}
				
				rootElement.addContent(templateDeviceElement);
			}
		}
	}
	
	// 테몬
	private void doSetTemon(JSONObject deviceObj, Element rootElement) throws Exception{
		
		SAXBuilder builder = new SAXBuilder();
		
		File xmlFile = new File("resource/conf/template/temon.xml");
		
		if(xmlFile.exists()){
			
			int aiCnt = 16;
			
			JSONArray tagArr = null;
			
			if(deviceObj.get("ai") != null){
				tagArr = (JSONArray)deviceObj.get("ai");
			}
			
			if(tagArr != null){
			
				int deviceOID = 40;
				
				Document doc = builder.build(xmlFile);
				
				Element templateRootElement = doc.getRootElement();
				
				Element templateDeviceElement = templateRootElement.getChild("device");
				templateDeviceElement.detach();
				
				Attribute oid = templateDeviceElement.getAttribute("oid");
				oid.setValue(String.valueOf(deviceOID));
				
				Attribute useYN = templateDeviceElement.getAttribute("use_yn");
				
				if(deviceObj.get("ai") == null){
					useYN.setValue("N");
				}else{
					useYN.setValue("Y");
				}
				
				Element tagsElement = templateDeviceElement.getChild("tags");
				
				int address = 40154;
				
				for(int i = 0; i < aiCnt; i++){
					
					JSONObject tagObj = isChannelExist(tagArr, (i+1));
					
					// Channel이 존재하면
					if(tagObj != null){
						
						Element tagElement = new Element("tag");
						tagElement.setAttribute("oid", String.valueOf(i+1));
						tagElement.setAttribute("address", String.valueOf(address++));
						tagElement.setAttribute("data_type", "INT16");
						tagElement.setAttribute("access", "R");
						tagElement.setAttribute("rate", "x/100");
						tagElement.setAttribute("monitor_yn", "Y");
						Element tagNmElement = new Element("name");
						tagNmElement.addContent(new CDATA(tagObj.getString("tag_nm")));
						tagElement.addContent(tagNmElement);
						Element unitElement = new Element("unit");
						unitElement.addContent(new CDATA(""));
						tagElement.addContent(unitElement);
						tagsElement.addContent(tagElement);
						
					}else{
						
						Element tagElement = new Element("tag");
						tagElement.setAttribute("oid", String.valueOf(i+1));
						tagElement.setAttribute("address", String.valueOf(address++));
						tagElement.setAttribute("data_type", "INT16");
						tagElement.setAttribute("access", "R");
						tagElement.setAttribute("rate", "x/100");
						tagElement.setAttribute("monitor_yn", "N");
						Element tagNmElement = new Element("name");
						tagNmElement.addContent(new CDATA(""));
						tagElement.addContent(tagNmElement);
						Element unitElement = new Element("unit");
						unitElement.addContent(new CDATA(""));
						tagElement.addContent(unitElement);
						tagsElement.addContent(tagElement);
					}
				}
				
				rootElement.addContent(templateDeviceElement);
			}
		}
	}
	
	private JSONObject isUnitIDExist(JSONArray tagArr, int unitID) throws Exception{
		
		JSONObject result = null;
		
		for(int i = 0; i < tagArr.size(); i++){
			
			JSONObject tagObj = (JSONObject)tagArr.get(i);
			
			if( unitID == Integer.parseInt(tagObj.getString("unit_id").trim()) ){
				result = tagObj;
				break;
			}
		}
		
		return result;
	}
	
	// 티센서
	private void doSetTSensor(JSONObject deviceObj, Element rootElement) throws Exception{
		
		SAXBuilder builder = new SAXBuilder();
		
		File xmlFile = new File("resource/conf/template/tsensor.xml");
		
		if(xmlFile.exists()){
			
			int thCnt = 5;
			
			JSONArray tagArr = null;
			
			if(deviceObj.get("th") != null){
				tagArr = (JSONArray)deviceObj.get("th");
			}
			
			if(tagArr != null){
				
				int deviceOID = 50;
				
				int address = 40170;
				
				for(int i = 0; i < thCnt; i++){
					
					Document doc = builder.build(xmlFile);
					
					Element templateRootElement = doc.getRootElement();
					
					Element templateDeviceElement = templateRootElement.getChild("device");
					templateDeviceElement.detach();
					
					Attribute oid = templateDeviceElement.getAttribute("oid");
					oid.setValue(String.valueOf(deviceOID++));
					
					JSONObject tagObj = isUnitIDExist(tagArr, (i+1));
					
					Attribute useYN = templateDeviceElement.getAttribute("use_yn");
					
					if(tagObj == null){
						useYN.setValue("N");
					}else{
						useYN.setValue("Y");
					}
					
					Element tagsElement = templateDeviceElement.getChild("tags");
					
					List<Element> tagList = tagsElement.getChildren("tag");
					
					for(Element tagElement : tagList){
						
						Attribute tagOid = tagElement.getAttribute("oid");
						
						// 통신불량이 아니면
						if(!"0".equals(tagOid.getValue())){
							
							Attribute addr = tagElement.getAttribute("address");
							addr.setValue(String.valueOf(address++));
							
							Attribute monitorYN = tagElement.getAttribute("monitor_yn");
							
							if(tagObj == null){
								monitorYN.setValue("N");
							}else{
								monitorYN.setValue("Y");
							}
						}
					}
					
					rootElement.addContent(templateDeviceElement);
				}
			}
		}
	}
}
