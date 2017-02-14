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

	static double TRANSMISSION_FAILURE_RATE = 0.1;

	private final String m_name;
	private final InetAddress m_address;
	private final int m_port;
	private boolean m_connected = false;
	static ArrayList<String> sentMsg = new ArrayList<String>();

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
			if (first) {
				msg = message + " " + System.currentTimeMillis();
			} else {
				msg = message;
			}
			// Sends a message to client using socket. Marshalls the message
			// by breaking it into bytes and put it in outPacket
			outPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, m_address, m_port);
			try {
				socket.send(outPacket);
				if(first){
					sentMsg.add(message);
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			msg = message;
			if (first) {
				sentMsg.add(msg);
				System.out.println("Message lost in cyber.. ");
			}

		}

	}

	public static void removeMsg(String instring) {
		for (int i = 0; i < sentMsg.size(); i++) {
			String[] inArray = instring.split(" ");
			String[] sentMsgArray = sentMsg.get(i).split(" ");
			System.out.println("Size of array " + sentMsg.size());
			if (inArray[0].equals(sentMsgArray[0])) {
				System.out.println("Match! Removes. Size of array " + sentMsg.size());
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
