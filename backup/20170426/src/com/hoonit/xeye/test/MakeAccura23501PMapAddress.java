package com.hoonit.xeye.test;

import java.io.File;
import java.io.FileWriter;

import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class MakeAccura23501PMapAddress {
	
	public MakeAccura23501PMapAddress(){
		
	}
	
	public int doMake(int idx, Document doc, Element rootElement, String fileNm, String deviceNm, int deviceOid){
		
		int cnt = 20;
		
		if(idx == 16 || idx == 17 || idx == 33 || idx == 34 || idx == 61 || idx == 62 || idx == 76 || idx == 77){
			cnt = 40;
		}
		
		int unitID = 1;
		
		int oid = deviceOid + 1;
		
		int tagOid = 104;
		
		int unitAddress = 411351;
		
		//String deviceNm = "2F 분전반 분기";
		
		//Element rootElement = new Element("item");
		
		for(int i = 0; i < cnt; i++){
			
			int address = unitAddress;
			
			/*Element deviceElement = new Element("device");
			deviceElement.setAttribute("protocol", "2");
			deviceElement.setAttribute("type", "0");
			deviceElement.setAttribute("oid", String.valueOf(oid++));
			deviceElement.setAttribute("unit_id", String.valueOf(unitID));
			deviceElement.setAttribute("channel", "");
			deviceElement.setAttribute("baud_rate", "");
			rootElement.addContent(deviceElement);*/
			Element deviceElement = rootElement.getChild("device");
			
			/*Element deviceNmElement = new Element("name");
			deviceNmElement.addContent(new CDATA(deviceNm + " 분기 #" + unitID));
			deviceElement.addContent(deviceNmElement);
			
			Element objectElement = new Element("object");
			objectElement.addContent(new CDATA("xeye.Modbus"));
			deviceElement.addContent(objectElement);
			
			Element tagsElement = new Element("tags");
			deviceElement.addContent(tagsElement);*/
			Element tagsElement = deviceElement.getChild("tags");
			
			/*Element tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", "");
			tagElement.setAttribute("data_type", "");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			Element tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("통신불량"));
			tagElement.addContent(tagNmElement);
			Element unitElement = new Element("unit");
			unitElement.addContent(new CDATA(" "));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);*/
			
			Element tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			Element tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Current I"));
			tagElement.addContent(tagNmElement);
			Element unitElement = new Element("unit");
			unitElement.addContent(new CDATA("A"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Current I1"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("A"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Current THD"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("%"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Current TDD"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("%"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Current Phasor Ix"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("A"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Current Phasor Iy"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("A"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("CF"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA(""));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("KF"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA(""));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Active Power P"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kW"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Reactive Power Q"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kVAR"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Apparent Power S"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kVA"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("ZCT leakage current"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("A"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Received KWh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kWh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Delivered KWh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kWh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Sum KWh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kWh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Net KWh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kWh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Received KVARh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kVARh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Delivered KVARh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kVARh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Sum KVARh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kVARh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Net KVARh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kVARh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "INT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("KVAh"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kVAh"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Demand KW"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kW"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Prediction Demand KW"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("kW"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Demand current"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("A"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "N");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Prediction Demand current"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("A"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "FLOAT32");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("PF"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA("A"));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 2;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "UINT16");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Phase information of Load connection"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA(""));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 1;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "UINT16");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Validity of PF"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA(""));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 1;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "UINT16");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Phase angle"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA(""));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 1;
			
			tagElement = new Element("tag");
			tagElement.setAttribute("oid", String.valueOf(tagOid++));
			tagElement.setAttribute("address", String.valueOf(address));
			tagElement.setAttribute("data_type", "UINT16");
			tagElement.setAttribute("access", "R");
			tagElement.setAttribute("rate", "");
			tagElement.setAttribute("monitor_yn", "Y");
			tagNmElement = new Element("name");
			tagNmElement.addContent(new CDATA("Leakage overflag"));
			tagElement.addContent(tagNmElement);
			unitElement = new Element("unit");
			unitElement.addContent(new CDATA(""));
			tagElement.addContent(unitElement);
			tagsElement.addContent(tagElement);
			address = address + 1;
			
			unitAddress += 150;
			unitID++;
			//oid++;
		}
		
		//Document doc = new Document(rootElement);
		
		XMLOutputter out = new XMLOutputter();
		out.setFormat(Format.getPrettyFormat());
		
		try{
			out.output(doc, new FileWriter("D:/Accura Inventory/"+fileNm));
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return oid;
	}
	
	public static void main(String[] args){
		
		System.out.println("Start...");
		
		MakeAccura23501PMapAddress a = new MakeAccura23501PMapAddress();
		
		try{
			
			int fileCnt = 90;
			
			int deviceOid = 200;
			
			String ipPrefix = "10.25.132.";
			
			int ip = 61;
			
			String fileNm = "";
			String deviceNm = "";
			
			int fileIdx = 0;
			
			for(int i = 0; i < fileCnt; i++){
			
				if(i == 34 || i == 60){
					fileIdx = 0;
				}
				
				SAXBuilder builder = new SAXBuilder();
				
				File baseXML = new File("D:/Accura Inventory/base.xml");
				
				Document doc = builder.build(baseXML);
				
				Element rootElement = doc.getRootElement();
				
				Element hostElement = rootElement.getChild("host");
				Attribute ipAtt = hostElement.getAttribute("ip");
				ipAtt.setValue(ipPrefix+ip);
				
				Element deviceElement = rootElement.getChild("device");
				Attribute oid = deviceElement.getAttribute("oid");
				oid.setValue(String.valueOf(deviceOid));
				
				fileIdx = fileIdx + 1;
				
				fileNm = "ivt_2F_#"+(i+1)+"_분전반 ";
				
				if(i < 34){
					fileNm += "A-" + (fileIdx) + ".xml";
				}else if(i < 60){
					fileNm += "B-" + (fileIdx) + ".xml";
				}else{
					fileNm += "C-" + (fileIdx) + ".xml";
				}
				
				deviceNm = "2F 분전반 ";
				
				if(i < 34){
					deviceNm += "A-" + (fileIdx);
				}else if(i < 60){
					deviceNm += "B-" + (fileIdx);
				}else{
					deviceNm += "C-" + (fileIdx);
				}
				
				Element nameElement = deviceElement.getChild("name");
				nameElement.setContent(new CDATA(deviceNm));
				
				deviceOid = a.doMake((i+1), doc, rootElement, fileNm, deviceNm, deviceOid);
				
				//deviceOid++;
				
				ip++;
			}
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
		System.out.println("Finished...");
	}
}
