package com.hoonit.xeye.net.snmp;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UnsignedInteger32;

import com.hoonit.xeye.event.DataEvent;
import com.hoonit.xeye.event.DataListener;
import com.hoonit.xeye.event.SetEvent;
import com.hoonit.xeye.event.SetListener;

public class DeviceMIB implements MOGroup, DataListener {

	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	private MOServer moServer;
	
	private OctetString myContext;
	
	private List<MOScalar> oidList;
	
	private Vector<SetListener> setListener;

	private SetEvent setEvt;
	
	public DeviceMIB() {
		
		setListener = new Vector<SetListener>();
		
		setEvt = new SetEvent(this);
	}
	
	public void addSetListener(SetListener obj) {
		setListener.add(obj);
	}

	public void removeSetListener(SetListener obj) {
		setListener.remove(obj);
	}

	public boolean notifySet(String oid, String data){
		
		boolean result = false;
		
		setEvt.setOid("."+oid);
		setEvt.setData(data);
		
		for(int i = 0; i < setListener.size(); i++){
			result = setListener.get(i).notifySet(setEvt);
		}
		
		return result;
	}
	
	public void setOIDList(List<MOScalar> oidList){
		this.oidList = oidList;
	}
	
	public List<MOScalar> getOIDList(){
		return this.oidList;
	}
	
	public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException
	{
		if(oidList != null){
			for(int i = 0; i < oidList.size(); i++){
				server.register(oidList.get(i), context);
			}
		}
		
		moServer = server;
		myContext = context;
	}
	
	public void unregisterMOs(MOServer server, OctetString context) {
		
		if(oidList != null){
			for(int i = 0; i < oidList.size(); i++){
				server.unregister(oidList.get(i), context);
			}
		}
		
		moServer = null;
		myContext = null;
	}
	
	public void notifyData(DataEvent e){
		
		if(e.getEtcResultMap() != null)
			doApplyData(e.getEtcResultMap());
		
		if(e.getResultMap() != null)
			doApplyData(e.getResultMap());
		
	}
	
	private void doApplyData(Map<String, String> dataMap){

		Iterator<String> it = dataMap.keySet().iterator();
		
		while(it.hasNext()){
			
			String key = it.next();
			String value = dataMap.get(key);
			
			for(MOScalar scalar :  oidList){
				
				if(key.equals("."+scalar.getOid().toString())){
					
					if(scalar instanceof Integer32OID){
						
						((Integer32OID)scalar).setValueByObserver(new Integer32(Integer.parseInt(value)));
						
					}else if(scalar instanceof UnsignedInteger32OID){
						
						((UnsignedInteger32OID)scalar).setValueByObserver(new UnsignedInteger32(Long.valueOf(value)));
						
					}else if(scalar instanceof OctetStringOID){
						
						((OctetStringOID)scalar).setValueByObserver(new OctetString(value));
						
					}
					
					break;
				}
			}
		}
	}
}
