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
	ArrayList<String> sentMsg = new ArrayList<String>();
	private ArrayList<String> allMessages = new ArrayList<String>();

	public ClientConnection(String name, InetAddress address, int port) {
		m_name = name;
		m_address = address;
		m_port = port;
	}

	public void sendMessage(String message, DatagramSocket socket, boolean first) {

		Random generator = new Random();
		double failure = generator.nextDouble();
		DatagramPacket outPacket = null;
		String msg = message;
		//String msg = prepareMsg(message);
		String[] msgArray = msg.split(" ");
		//System.out.println("THIS IS IN BEGINNING OF SENDMESSAGE " + msg);
		String tempmsg = "";
		
		if (failure > TRANSMISSION_FAILURE_RATE) {
			if (first) {
				if(msgArray[0].equals("8") || msgArray[0].equals("9") || msgArray[0].equals("qAlive")){
					tempmsg = msg;
				}else{
					for(int i = 0; i < msgArray.length-1; i++){
						tempmsg += msgArray[i] + " ";
					}
				}
				
				msg = tempmsg + " " + System.currentTimeMillis();
				//System.out.println("Msg in sendMessage: " + msg);
			}

			// Sends a message to client using socket. Marshalls the message
			// by breaking it into bytes and put it in outPacket
			outPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, m_address, m_port);
			try {
				socket.send(outPacket);
				boolean found = false;
				for (int i = 0; i < sentMsg.size(); i++) {
					if (sentMsg.get(i).equals(msg)) {
						found = true;
					}
				}
				if (msgArray.length > 0) {
					if (!found && !msgArray[0].equals("ack")) {
						sentMsg.add(msg);
						//System.out.println("Added to sentMsg-array in if: " + msg);
					}
				}

				/*
				 * if (first) { sentMsg.add(message); }
				 */

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			boolean found = false;
			for (int i = 0; i < sentMsg.size(); i++) {
				if (sentMsg.get(i).equals(msg)) {
					found = true;
				}
			}
			if (msgArray.length > 0) {
				if (!found && !msgArray[0].equals("ack")) {
					sentMsg.add(msg);
					//System.out.println("Added to sentMsg-array in else: " + msg);
					System.out.println("Message lost in cyber.. ");
				}
			}

		}

	}

	public void addMsgToArray(String msg) {
		allMessages.add(msg);
	}

	public void removeMessages(String message) {
		String[] inMsg = message.split(" ");
		for (int i = 0; i < sentMsg.size(); i++) {
			// String[] sentMsgArray = sentMsg.get(i).split(" ");
			if (sentMsg.get(i).endsWith(inMsg[inMsg.length - 1])) {
				System.out.println("Sent msg " + sentMsg.get(i) + " and inMsgArray - " + inMsg[inMsg.length - 1]);
				// System.out.println("Removed msg " + sentMsg.size() + " " +
				// sentMsgArray[sentMsgArray.length-1]);
				sentMsg.remove(i);
				break;
			}
		}
	}

	public void resend(DatagramSocket socket) {
		for (int i = 0; i < sentMsg.size(); i++) {
			sendMessage(sentMsg.get(i), socket, false);
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
