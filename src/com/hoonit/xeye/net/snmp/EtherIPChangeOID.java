package com.hoonit.xeye.net.snmp;

import java.util.StringTokenizer;
import java.util.Vector;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.snmp.EnumeratedScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

import com.hoonit.xeye.event.IPChangeEvent;
import com.hoonit.xeye.event.IPChangeListener;

/**
 * Ethernet IP, Subnet Mask, Gateway 변경을 담당하는 OID
 * @author Administrator
 *
 */
public class EtherIPChangeOID extends EnumeratedScalar<OctetString>{
	
	private Vector<IPChangeListener> ipChangeListener;

	private IPChangeEvent ipChangeEvt;
	
	public EtherIPChangeOID(OID oid, MOAccess access) {
		
		super(oid, access, new OctetString());
		
		setVolatile(true);
		
		this.ipChangeListener = new Vector<IPChangeListener>();
    	
    	this.ipChangeEvt = new IPChangeEvent(this);
	}
	
	@Override
	public int setValue(OctetString newValue) {
		
		int result = 0;
		
		if(notifyIPChange(newValue.toString())){
			result = super.setValue(newValue);
		}
		
		return result;
	}
	
	public void addIPChangeListener(IPChangeListener obj) {
		ipChangeListener.add(obj);
	}

	public void removeIPChangeListener(IPChangeListener obj) {
		ipChangeListener.remove(obj);
	}
	
	public boolean notifyIPChange(String val){
		
		boolean result = false;
		
		StringTokenizer st = new StringTokenizer(val, ";");
		
		String address = st.nextToken();
		String netmask = st.nextToken();
		String gateway = st.nextToken();
		
		ipChangeEvt.setAddress(address);
		ipChangeEvt.setNetmask(netmask);
		ipChangeEvt.setGateway(gateway);
		
		for(int i = 0; i < ipChangeListener.size(); i++){
			result = ipChangeListener.get(i).notifyIPChange(ipChangeEvt);
		}
		
		return result;
	}
}
