package com.hoonit.xeye.test;

import java.util.HashMap;
import java.util.Map;

public class MapTest {

	public static void main(String[] args){
		
		System.out.println("시작");
		
		Map<String, String> map = new HashMap<String, String>();
		
		for(int i = 0; i < 10000; i++){
			map.put(String.valueOf(i), String.valueOf(i));
		}
		
		System.out.println("맵 로드 완료");
		
		try{
			Thread.sleep(1000);
		}catch(Exception e){
		}
		
		System.out.println("1000=" + map.get(String.valueOf(1000)));
		System.out.println("3000=" + map.get(String.valueOf(3000)));
		System.out.println("6000=" + map.get(String.valueOf(6000)));
		System.out.println("9000=" + map.get(String.valueOf(9000)));
	}
}
