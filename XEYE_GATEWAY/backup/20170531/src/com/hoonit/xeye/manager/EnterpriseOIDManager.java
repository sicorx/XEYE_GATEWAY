package com.hoonit.xeye.manager;

import com.hoonit.xeye.util.ResourceBundleHandler;

public class EnterpriseOIDManager {

	// .1.3.6.1.4.1.4999.2
	private static String enterpriseOID = ResourceBundleHandler.getInstance().getString("snmp.enterprise.oid");
	
	public static String getEnterpriseOID(){
		return enterpriseOID;
	}
}
