package com.hoonit.xeye.test;

import java.util.List;

public class SampleDevice implements IDevice2 {

	public SampleDevice(int a, int b, String c, List<String> list){
		System.out.println("a:"+a);
		System.out.println("b:"+b);
		System.out.println("c:"+c);
		
		for(String s : list){
			System.out.println("s:"+s);
		}
	}
	
	public String getDeviceOID(){
		return "test";
	}
}
