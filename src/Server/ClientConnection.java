/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

/**
 * 
 * @author brom
 */
public class ClientConnection {

	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private boolean m_connected = false;
	private static ArrayList<String> sentMsg = new ArrayList<String>();

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	public void sendMessage(String message, DatagramSocket socket, boolean first) {

		Random generator = new Random();
		double failure = generator.nextDouble();
		DatagramPacket outPacket = null;
		String msg = "";

		if (failure > TRANSMISSION_FAILURE_RATE) {
			if(first){
				long millis = System.currentTimeMillis();
				msg = message + " " + millis;
			}else{
				msg = message;
			}
			
			// Sends a message to client using socket. Marshalls the message
			// by breaking it into bytes and put it in outPacket
			outPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, m_address, m_port);
			try {
				socket.send(outPacket);
				sentMsg.add(message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			sendAgain(socket);
			// Message got lost
			System.out.println("Message lost in cyberspace.. " + sentMsg.size());
			// Calls method again because the message was lost
			//sendMessage(message, socket);
			
		}

	}
	
	private void sendAgain(DatagramSocket socket){
		System.out.println("in sendAgain - size of sentMsg " + sentMsg.size());
		while(sentMsg.size() > 0){
			for(int i = 0; i < sentMsg.size(); i++){
				String resend = sentMsg.get(i);
				sentMsg.remove(i);
				sendMessage(resend, socket, false);
				System.out.println("in sendAgain for-loop: " + resend);
				//System.exit(0);
			}
		}
	}
	
	public static void checkMsg(String instring){
		for(int i = 0; i < sentMsg.size(); i++){
			String[] inArray = instring.split(" ");
			String[] sentMsgArray = sentMsg.get(i).split(" ");
			if (inArray[0].equals(sentMsgArray[0])) {
				System.out.println("Match! Removes.");
				sentMsg.remove(i);
			}
		}
	}

	public boolean hasName(String testName) {
		return testName.equals(m_name);
	}

	public String getName() {
		return m_name;
	}

	// Returns true if address and port is the same as m_address and m_port
	// Good to use when checking if correct user
	public boolean hasAddressPlusPort(InetAddress address, int port) {
		return (address == m_address && port == m_port);
	}

	// set- and getConnected used to mark if a client still is connected
	public void setConnected(boolean connected) {
		m_connected = connected;
	}

	public boolean getConnected() {
		return m_connected;
	}

}
