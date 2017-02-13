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
	private ArrayList<DatagramPacket> notSentMsg = new ArrayList<DatagramPacket>();

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	public void sendMessage(String message, DatagramSocket socket) {

		Random generator = new Random();
		double failure = generator.nextDouble();
		DatagramPacket outPacket = null;

		if (failure > TRANSMISSION_FAILURE_RATE) {
			// Sends a message to client using socket. Marshalls the message
			// by breaking it into bytes and put it in outPacket
			String mOut = message;
			outPacket = new DatagramPacket(mOut.getBytes(), mOut.getBytes().length, m_address, m_port);
			try {
				socket.send(outPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			notSentMsg.add(outPacket);
			// Message got lost
			System.out.println("Message lost in cyberspace.. " + notSentMsg.size());
			// Calls method again because the message was lost
			//sendMessage(message, socket);
			
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
