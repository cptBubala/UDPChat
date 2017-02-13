/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author brom
 */
public class ServerConnection {

	// Artificial failure rate of 30% packet loss
	static double TRANSMISSION_FAILURE_RATE = 0.3;

	private DatagramSocket m_socket = null;
	private InetAddress m_serverAddress = null;
	private int m_serverPort = -1;
	private ArrayList<DatagramPacket> notSentMsg = new ArrayList<DatagramPacket>();

	public ServerConnection(String hostName, int port) {
		m_serverPort = port;

		// Get the address of host based on parameters and assigns it to
		// m_serverAddress
		// Set up socket and assigns it to m_socket

		try {
			m_serverAddress = InetAddress.getByName(hostName);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			m_socket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Adds a 0 to client name and puts it in string that is marshalled and sent
	// via socket in method sendChatMessage
	public boolean handshake(String name) {
		String _outString = "0 " + name;
		sendChatMessage(_outString, true);

		// If connection successful receiveChatMessage returns string "True" and
		// handshake returns true
		if (receiveChatMessage().equals("True")) {
			System.out.println("Connected to server!");
			return true;
		} else {
			// Returns false if connection failed (e.g., if user name was
			// taken);
			return false;
		}
	}

	public String receiveChatMessage() {
		// Receives message from server and puts in inPacket
		DatagramPacket inPacket;
		byte[] m_buf = new byte[2000];
		inPacket = new DatagramPacket(m_buf, m_buf.length);
		try {
			m_socket.receive(inPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Un-marshals message and put in inString
		String inString = new String(inPacket.getData(), 0, inPacket.getLength()).trim();

		return inString;
	}

	public void sendChatMessage(String message, boolean isHandshake) {
		Random generator = new Random();
		double failure = generator.nextDouble();
		DatagramPacket outPacket = null;
		
		if (failure > TRANSMISSION_FAILURE_RATE) {
			// Marshals message to outPacket
			outPacket = new DatagramPacket(message.getBytes(), message.getBytes().length, m_serverAddress,
					m_serverPort);
			try {
				m_socket.send(outPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// If client sends chat message /leave (command to leave chat), that
			// message gets a 4 in the beginning
			// and that means client wants to leave. System.exit(0) shuts down
			// window
			String[] _outMessageArray = message.split(" ");
			if (_outMessageArray[0].equals("4")) {
				System.exit(0);
			}

		} else {
			
			notSentMsg.add(outPacket);
			//sendChatMessage(message);
			System.out.println("Message lost in cyber.. " + notSentMsg.size());
		}

	}

}
