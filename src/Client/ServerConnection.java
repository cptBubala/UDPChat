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
	private ArrayList<String> sentMsg = new ArrayList<String>();
	private ArrayList<String> displayedMsg = new ArrayList<String>();

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

		try {
			// Used for socket-time out
			// Stops receiving after 5 sec and moves on
			m_socket.setSoTimeout(5000);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Adds a 0 to client name and puts it in string that is marshalled and sent
	// via socket in method sendChatMessage
	public boolean handshake(String name) {
		String _outString = "0 " + name;
		DatagramPacket outPacket = new DatagramPacket(_outString.getBytes(), _outString.getBytes().length,
				m_serverAddress, m_serverPort);
		try {
			m_socket.send(outPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

	// Receives messages
	public String receiveChatMessage() {
		// Receives message from server and puts in inPacket
		DatagramPacket inPacket;
		byte[] m_buf = new byte[2000];
		inPacket = new DatagramPacket(m_buf, m_buf.length);

		// Un-marshals message and put in inString
		String inString = "";
		boolean checkMsgReceived = false;
		try {
			m_socket.receive(inPacket);
			checkMsgReceived = true;
			inString = new String(inPacket.getData(), 0, inPacket.getLength()).trim();
			System.out.println("RCV: " + "'" + inString + "'");
		} catch (IOException e) {
			checkMsgReceived = false;
		}

		if (checkMsgReceived) {

			if (!inString.startsWith("ack")) {
				sendAck(inString);
			}
			// inString = controlMsg(inString);
		}

		/*
		 * else { inString = ""; }
		 */
		// System.out.println("Before returning inString in receiveChatMessage:
		// " + inString);

		String[] newStringArray = inString.split(" ");
		/*
		 * for(int i = 0; i < newStringArray.length - 1;i++){ newString =
		 * newString + newStringArray[i] + " "; }
		 */
		// System.out.println("In receiveMsg: inString is " + inString);

		/*
		 * String checkIfDisplayed = showMessage(inString); for (int m = 0; m <
		 * displayedMsg.size(); m++) { if
		 * (checkIfDisplayed.equals(displayedMsg.get(m))) { System.out.println(
		 * "Message already displayed " + checkIfDisplayed + " " +
		 * displayedMsg.get(m)); inString = "??=)!!"; break; } }
		 */

		return inString; // fram hit verkar det fungera på första
							// meddelandet att eerik has joined the chat.
							// Sen bara ack?
	}

	private void sendAck(String msg) {
		if (!msg.equals("True")) {
			String tempMsg = "ack" + " " + msg;
			// System.out.println("tempMsg in sendAck " + tempMsg);
			sendChatMessage(tempMsg, false);
		}
	}

	// Sends message and adds it to arraylist
	public synchronized void sendChatMessage(String message, boolean first) {
		Random generator = new Random();
		double failure = generator.nextDouble();
		String msg = "";
		if (failure > TRANSMISSION_FAILURE_RATE) {
			if (first) {
				msg = message + " " + System.currentTimeMillis();
			} else {
				msg = message;
			}
			String[] msgArray = msg.split(" ");
			// Marshals message to outPacket
			DatagramPacket outPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, m_serverAddress,
					m_serverPort);
			try {
				m_socket.send(outPacket);
				System.out.println("Send: " + "'" + msg + "'");
				boolean found = false;
				for (int i = 0; i < sentMsg.size(); i++) {
					if (sentMsg.get(i).equals(msg)) {
						found = true;
					}
				}
				if (!found && !msgArray[0].equals("ack")) {
					sentMsg.add(msg);
					// System.out.println("Added to sentMsg-array in if: " +
					// msg);
				}

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
				// System.out.println("Size of sentMsg right before shutdown: "
				// + sentMsg.size());
				for (int i = 0; i < sentMsg.size(); i++) {
					// System.out.println(sentMsg.get(i));
				}
				System.exit(0);
			}

		} else {
			if (first) {
				msg = message + " " + System.currentTimeMillis();
			} else {
				msg = message;
			}
			boolean found = false;
			for (int i = 0; i < sentMsg.size(); i++) {
				if (sentMsg.get(i).equals(msg)) {
					found = true;
				}
			}
			if (!found) {
				sentMsg.add(msg);
				// System.out.println("Added to sentMsg-array in else: " + msg);
				System.out.println("Message lost in cyber.. ");
			}

		}

	}

	public synchronized void removeMessages(String message) {
		String[] inMsg = message.split(" ");
		for (int i = 0; i < sentMsg.size(); i++) {
			String[] sentMsgArray = sentMsg.get(i).split(" ");
			// System.out.println("sentMsgArray + inMsg '" +
			// (sentMsgArray[sentMsgArray.length - 1]) + "'" + " '" +
			// (inMsg[inMsg.length - 1]));

			if (sentMsgArray[sentMsgArray.length - 1].equals(inMsg[inMsg.length - 1])) {
				// System.out.println("Size " + sentMsg.size() + " msg to
				// delete: " + sentMsg.get(i));
				sentMsg.remove(i); // Seems to work
				// System.out.println("Removed msg " + sentMsg.size());
				break;
			}

		}
	}

	public synchronized void resendMsg() {
		for (int i = 0; i < sentMsg.size(); i++) {
			System.out.println("RESENDING IN CLIENT: " + sentMsg.get(i));
			sendChatMessage(sentMsg.get(i), false);
			System.out.println("RESENT!");
		}
	}

}
