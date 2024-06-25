package com.hoonit.xeye.test;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.ModbusCoupler;
import net.wimpi.modbus.net.ModbusTCPListener;
import net.wimpi.modbus.procimg.SimpleDigitalIn;
import net.wimpi.modbus.procimg.SimpleDigitalOut;
import net.wimpi.modbus.procimg.SimpleProcessImage;
import net.wimpi.modbus.procimg.SimpleRegister;

public class ModbusTCPSlave {

	public static void main(String[] args){
		
		/*ModbusTCPListener listener = null;
		int port = Modbus.DEFAULT_PORT;
		
		//2. Prepare a process image
		SimpleProcessImage spi = new SimpleProcessImage();
		spi.addRegister(new SimpleRegister(100));
		spi.addRegister(new SimpleRegister(200));

		//3. Set the image on the coupler
		ModbusCoupler.getReference().setProcessImage(spi);
		ModbusCoupler.getReference().setMaster(false);
		ModbusCoupler.getReference().setUnitID(1);
		
		SimpleProcessImage spi2 = new SimpleProcessImage();
		spi2.addRegister(new SimpleRegister(300));
		spi2.addRegister(new SimpleRegister(400));

		//3. Set the image on the coupler
		ModbusCoupler.getReference().setProcessImage(spi2);
		ModbusCoupler.getReference().setMaster(false);
		ModbusCoupler.getReference().setUnitID(2);
		
		//4. Create a listener with 3 threads in pool
		listener = new ModbusTCPListener(3);
		listener.setPort(port);
		listener.start(); */
		
	}
}
