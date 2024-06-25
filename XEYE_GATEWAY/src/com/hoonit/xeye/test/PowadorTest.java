package com.hoonit.xeye.test;

public class PowadorTest {

	public static void main(String[] args){
		
		String result = "\r*120--35-619.8-124.90-015400-414.0-019.10-013400-19-0018700-a-100kTR-000008645\n";
		
		String st1 = result.substring(0, 1);
		String a   = result.substring(1, 5);
		String s   = result.substring(5, 9);
		String v   = result.substring(9, 15);
		String i   = result.substring(15, 22);
		String p   = result.substring(22, 29);
		String un  = result.substring(29, 35);
		String in  = result.substring(35, 42);
		String pn  = result.substring(42, 49);
		String t   = result.substring(49, 52);
		String e   = result.substring(52, 60);
		String f   = result.substring(60, 62);
		
		System.out.println(result.getBytes().length);
		
		System.out.println(st1);
		System.out.println(a);
		System.out.println(s);
		System.out.println(v);
		System.out.println(i);
		System.out.println(p);
		System.out.println(un);
		System.out.println(in);
		System.out.println(pn);
		System.out.println(t);
		System.out.println(e);
		System.out.println(f);
	}
}
