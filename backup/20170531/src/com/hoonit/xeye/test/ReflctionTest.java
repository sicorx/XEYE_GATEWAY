package com.hoonit.xeye.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

public class ReflctionTest {

	public static void main(String[] args){
		
		try{
			
			Class clz = Class.forName("com.hoonit.xeye.device.SampleDevice");
			
			Class[] constructParameterTypes = {int.class, int.class, String.class, java.util.List.class};
			
			Constructor constructor = clz.getDeclaredConstructor(constructParameterTypes);
			
			java.util.List<String> list = new ArrayList<String>();
			list.add("s");
			
			Object obj = constructor.newInstance(1, 1, "a", list);
			
			//IDevice2 device = clz.getGenericInterfaces()[0];
			
			if(obj instanceof IDevice2){
			
				System.out.println(((IDevice2)obj).getDeviceOID());
			}		
			
			/*Type[] interfaces = Class.forName("com.hoonit.xeye.device.SampleDevice").getGenericInterfaces();
			//prints "[java.util.Map<K, V>, interface java.lang.Cloneable, interface java.io.Serializable]"
			System.out.println(Arrays.toString(interfaces));
			//prints "[interface java.util.Map, interface java.lang.Cloneable, interface java.io.Serializable]"
			System.out.println(Arrays.toString(Class.forName("com.hoonit.xeye.device.SampleDevice").getInterfaces()));*/
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
