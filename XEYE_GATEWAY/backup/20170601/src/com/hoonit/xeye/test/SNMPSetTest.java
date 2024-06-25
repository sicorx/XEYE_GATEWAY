package com.hoonit.xeye.test;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SNMPSetTest {
	
	private static String ipAddress = "192.168.123.26";

	private static String port = "161";
  
	private static int snmpVersion = SnmpConstants.version2c;

	private static String community = "public";

	public static void main(String[] args) throws Exception {

		System.out.println("SNMP SET Start");

	    // Create TransportMapping and Listen
	    TransportMapping transport = new DefaultUdpTransportMapping();
	    transport.listen();
	
	    // Create Target Address object
	    CommunityTarget comtarget = new CommunityTarget();
	    comtarget.setCommunity(new OctetString(community));
	    comtarget.setVersion(snmpVersion);
	    comtarget.setAddress(new UdpAddress(ipAddress + "/" + port));
	    comtarget.setRetries(1);
	    comtarget.setTimeout(6000);
    	
	    // Create the PDU object
	    PDU pdu = new PDU();
	    pdu.setType(PDU.SET);
	    
	    // 제조사 LG:0, Samsung:1
	    OID oid = new OID(".1.3.6.1.4.1.4999.2.60.4.0");
	    Variable var = new OctetString("0");
	    VariableBinding varBind = new VariableBinding(oid, var);
	    pdu.add(varBind);
	    
	    // 냉/난방 냉방:1, 난방:0
	    oid = new OID(".1.3.6.1.4.1.4999.2.60.5.0");
	    var = new OctetString("1");
	    varBind = new VariableBinding(oid, var);
	    pdu.add(varBind);
	    
	    // 설정온도
	    oid = new OID(".1.3.6.1.4.1.4999.2.60.6.0");
	    var = new OctetString("18");
	    varBind = new VariableBinding(oid, var);
	    pdu.add(varBind);
	    
	    // ON/OFF ON:1, OFF:0
	    oid = new OID(".1.3.6.1.4.1.4999.2.60.7.0");
	    var = new OctetString("0");
	    varBind = new VariableBinding(oid, var);
	    pdu.add(varBind);
	    
	    // 제어전송
	    oid = new OID(".1.3.6.1.4.1.4999.2.60.8.0");
	    var = new OctetString("1");
	    varBind = new VariableBinding(oid, var);
	    pdu.add(varBind);
	    
	    // Create Snmp object for sending data to Agent
	    Snmp snmp = new Snmp(transport);
	    
	    System.out.println("Request:\nSending Snmp Set Request to Agent...");
	    ResponseEvent response = snmp.set(pdu, comtarget);
	    
	    // Process Agent Response
	    if (response != null) {
	    	
	    	System.out.println("\nResponse:\nGot Snmp Set Response from Agent");
	    	PDU responsePDU = response.getResponse();

	    	if (responsePDU != null) {
	    		
		        int errorStatus = responsePDU.getErrorStatus();
		        int errorIndex = responsePDU.getErrorIndex();
		        String errorStatusText = responsePDU.getErrorStatusText();

		        if (errorStatus == PDU.noError) {
		        	//System.out.println("Snmp Set Response = " + responsePDU.getVariableBindings());
		        	
		        	for(VariableBinding vb : responsePDU.getVariableBindings()){
		        		System.out.println(vb.getOid() + "=" + vb.getVariable().toString());
		        	}
		        	
		        } else {
		            System.out.println("Error: Request Failed");
		            System.out.println("Error Status = " + errorStatus);
		            System.out.println("Error Index = " + errorIndex);
		            System.out.println("Error Status Text = " + errorStatusText);
		        }
	    	} else {
	    		System.out.println("Error: Response PDU is null");
	    	}
	    } else {
	    	System.out.println("Error: Agent Timeout... ");
	    }
	    
	    snmp.close();
	}
}

