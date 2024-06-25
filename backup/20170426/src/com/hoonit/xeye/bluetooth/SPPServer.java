package com.hoonit.xeye.bluetooth;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.apache.log4j.Logger;

import com.hoonit.xeye.event.BluetoothPacketEvent;
import com.hoonit.xeye.event.BluetoothPacketListener;

public class SPPServer {
	
	protected final Logger logger = Logger.getLogger(getClass().getName());

	private UUID uuid = new UUID("1101", true);
	
	private StreamConnectionNotifier server = null;
	
	private Vector<BluetoothPacketListener> bluetoothPacketListener;

	private BluetoothPacketEvent bluetoothPacketEvt;
	
	public SPPServer() throws IOException {
		
		//Create the servicve url
		String connectionString = "btspp://localhost:" + uuid +";name=SPP Server";
		
		server = (StreamConnectionNotifier) Connector.open(connectionString);
		
		this.bluetoothPacketListener = new Vector<BluetoothPacketListener>();
	}
	
	public void addBluetoothPacketListener(BluetoothPacketListener obj) {
		bluetoothPacketListener.add(obj);
	}

	public void removeBluetoothPacketListener(BluetoothPacketListener obj) {
		bluetoothPacketListener.remove(obj);
	}

	public String notifyBluetoothPacket(String packet){
		
		String result = "";
		
		bluetoothPacketEvt = new BluetoothPacketEvent(this);
		bluetoothPacketEvt.setPacket(packet);
		
		for(int i = 0; i < bluetoothPacketListener.size(); i++){
			result = bluetoothPacketListener.get(i).notifyBluetoothPacket(bluetoothPacketEvt);
		}
		
		return result;
	}
	
	public Session accept() throws IOException {
        
        StreamConnection channel = server.acceptAndOpen();
        
        return new Session(channel);
    }
	
	public void dispose(){
		
		logger.info("Dispose");
		
        if (server != null) {
        	try {
        		server.close();
        	} catch (Exception e) {
        		logger.error(e.getMessage(), e);
        	}
        }
	}
	
	public class Session implements Runnable {
		
        private StreamConnection channel = null;
        private BufferedReader reader = null;
        private BufferedWriter writer = null;
        
        public Session(StreamConnection channel) throws IOException {
        	
            this.channel = channel;
            
            reader = new BufferedReader(new InputStreamReader
                      (channel.openInputStream(), Charset.forName(StandardCharsets.UTF_8.name())));
            
            writer = new BufferedWriter(new OutputStreamWriter
                    (channel.openOutputStream(), Charset.forName(StandardCharsets.UTF_8.name())));
            
            logger.info("Session is created...");
        }  
        
        public void run() {
        	
            try{
            	
                String packet = reader.readLine();
                
                logger.info("packet: " + packet);
                
                String result = notifyBluetoothPacket(packet);
                
                logger.info("response: " + result);
                
                writer.write(result+"\n");
                writer.flush();
                
            }catch (IOException t) {
               logger.error(t.getMessage(), t);
            }finally{
            	
            	if (reader != null) try {reader.close();} catch (Exception e) {}
                if (writer != null) try {writer.close();} catch (Exception e) {}
                if (channel != null) try {channel.close();} catch (Exception e) {}
                
                logger.info("Session is close...");
            }
        }
    }
}