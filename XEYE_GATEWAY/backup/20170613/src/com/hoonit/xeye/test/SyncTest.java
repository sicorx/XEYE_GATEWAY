package com.hoonit.xeye.test;

public class SyncTest {

	private Object readLock = new Object();
	
	public void run(){
		
		try{
			synchronized(readLock){
				
				Integer.parseInt("aaa");
				
				readLock.wait(3000);
				
				System.out.println("11111");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		SyncTest st = new SyncTest();
		st.run();
	}
}
