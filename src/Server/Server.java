package Server;

import java.io.IOException;

//
// Source file for the server side. 
//
// Created by Sanny Syberfeldt
// Maintained by Marcus Brohede
//

import java.net.*;
//import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {

	private ArrayList<ClientConnection> m_connectedClients = new ArrayList<ClientConnection>();
	private DatagramSocket m_socket;
	private long millis;
	private long timeElapsed;
	private long msg_millis;
	private long msg_timeElapsed;
	private ArrayList<String> receivedMsg = new ArrayList<String>();
	private ArrayList<String> sentMsgArray = new ArrayList<String>();

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: java Server portnumber");
			System.exit(-1);
		}
		try {
			Server instance = new Server(Integer.parseInt(args[0]));
			instance.listenForClientMessages();
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Server(int portNumber) {
		// Created a socket, attached it to port based on portNumber, and
		// assigned it to m_socket
		try {
			m_socket = new DatagramSocket(portNumber);
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
		millis = System.currentTimeMillis();
		msg_millis = System.currentTimeMillis();
	}

	private void listenForClientMessages() {
		System.out.println("Waiting for client messages... ");

		// Sets a resonable size of the buffer
		byte[] m_buf = new byte[2000];
		// for un-marshalling
		DatagramPacket inPacket = new DatagramPacket(m_buf, m_buf.length);
		// Used to send a marshalled message
		DatagramPacket outPacket;
		String inString = "";
		do {
			// Used so that not the same message is re-used again after 5 sec
			boolean checkMsgReceived = false;
			try {
				m_socket.receive(inPacket);
				inString = new String(inPacket.getData(), 0, inPacket.getLength()).trim();
				System.out.println("RCV: " + "'" + inString + "'");
				checkMsgReceived = true;
			} catch (IOException e) {
				// Shows that no message has been received since the last one
				checkMsgReceived = false;
			} finally {

				/********** CHECK IF ALIVE ***********/

				// Used to get a time for when server should check if clients is
				// connected
				timeElapsed = System.currentTimeMillis() - millis;
				if (timeElapsed > 20000) {
					// Resets millis (otherwise that time will be to great)
					millis = System.currentTimeMillis();
					// For-loop that checks all connected clients connection
					// status (true or false)
					for (int i = 0; i < m_connectedClients.size(); i++) {
						if (m_connectedClients.get(i).getConnected()) {
							System.out.println(m_connectedClients.get(i).getName() + " is still alive");
						} else {
							String disconnectMsg = "User " + m_connectedClients.get(i).getName()
									+ " was disconnected for being idle!";
							sentMsgArray.add(disconnectMsg);
							// Message to all users that a user is kicked for
							// idle
							broadcast(disconnectMsg);
							System.out.println(m_connectedClients.get(i).getName() + " is dead");
							// Removes said client from ArrayList
							m_connectedClients.remove(i);
						}
					}

					// Server sends question to all clients to check if they are
					// alive using broadcast
					broadcast("qAlive");

					/*
					 * Sets all remaining clients connection status to false (so
					 * if they don't answer, their status remains false and they
					 * are removed
					 */
					for (int i = 0; i < m_connectedClients.size(); i++) {
						m_connectedClients.get(i).setConnected(false);
					}

				}

			}

			String _outString = "";
			String ack_outString = "";
			String handshake = "False";
			// Put in-package in a string

			// Put inString in the array, splits at, and removes, space
			String[] _inStringArray = inString.split("\\s+");
			/*
			 * for (int i = 0; i < m_connectedClients.size(); i++) { if
			 * (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress
			 * (), inPacket.getPort())) {
			 * m_connectedClients.get(i).addMsgToArray(inString); } }
			 */
			/*
			 * if(inString.startsWith("ack")){
			 * ClientConnection.checkMsg(inString); }
			 */

			/******* CHECKS IF CLIENTS ANSWERS THAT THEY ARE ALIVE *******/

			// checkMessageSent used so no old message are re-used
			if (_inStringArray[0].equals("isAlive") && checkMsgReceived) {
				for (int i = 0; i < m_connectedClients.size(); i++) {
					if (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress(), inPacket.getPort())) {
						// Sets clients connection to true since it sent
						// "isAlive"
						m_connectedClients.get(i).setConnected(true);
					}
				}
			}

			/********** REMOVE ACK *********/
			boolean ack = false;
			if (_inStringArray[0].equals("ack") && checkMsgReceived) {
				for (int i = 0; i < m_connectedClients.size(); i++) {
					m_connectedClients.get(i).removeMessages(inString);
					ack = true;
				}

			}

			/**************** RESENDING MESSAGES *****************/
			msg_timeElapsed = System.currentTimeMillis() - msg_millis;
			if (msg_timeElapsed > 500) {
				msg_millis = System.currentTimeMillis();
				for (int i = 0; i < m_connectedClients.size(); i++) {
					m_connectedClients.get(i).resend(m_socket);
					// System.out.println("Resending msgs");
				}
			}

			/************* IF MSG NOT RECEIVED ****************/

			if (!ack && !checkMsgReceived(_inStringArray[_inStringArray.length - 1])) {
				System.out.println("!checkMsgReceived " + _inStringArray[_inStringArray.length - 1]);

				/********** HANDSHAKE **********/

				if (_inStringArray[0].equals("0") && checkMsgReceived) {
					if (addClient(_inStringArray[1], inPacket.getAddress(), inPacket.getPort())) {
						for (int i = 0; i < m_connectedClients.size(); i++) {
							if (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress(),
									inPacket.getPort())) {
								// When handshake is done - client connected is
								// true
								m_connectedClients.get(i).setConnected(true);
							}
						}
						// System.out.println("Added Client " +
						// _inStringArray[1] + "!");
						// Used so client knows it got the handshake
						handshake = "True";
						outPacket = new DatagramPacket(handshake.getBytes(), handshake.getBytes().length,
								inPacket.getAddress(), inPacket.getPort());
						try {
							// Sends back the package with "True" to the client
							m_socket.send(outPacket);
							_outString = "9" + " " + _inStringArray[1] + " has joined the chat";
							// Tells all in chat that the client has joined the
							// chat
							broadcast(_outString);
							// System.out.println("CLIENT JOINED CHAT: " +
							// _outString);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} else {
						System.out.println("Client already exists!");
						outPacket = new DatagramPacket(handshake.getBytes(), handshake.getBytes().length,
								inPacket.getAddress(), inPacket.getPort());
						try {
							m_socket.send(outPacket);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}

				/*********** BROADCAST MESSAGE **********/

				// "1" means broadcast
				else if (_inStringArray[0].equals("1") && checkMsgReceived) {
					// ack_outString = "ack" + " " +
					// _inStringArray[_inStringArray.length-1] + " " +
					// _inStringArray[_inStringArray.length-1];
					ack_outString = "ack" + " " + inString;
					sendPrivateMessage(ack_outString, _inStringArray[1]);
					// System.out.println(ack_outString);
					// System.out.print("This is inString: " + inString);
					// broadcast(_outString);
					for (ClientConnection cc : m_connectedClients) {
						if (cc.hasAddressPlusPort(inPacket.getAddress(), inPacket.getPort())) {
							// Gets the sender so it is possible to set its
							// connected true
							cc.setConnected(true);
						}
					}

					/*
					 * for (int i = 0; i < m_connectedClients.size(); i++) { if
					 * (m_connectedClients.get(i).hasAddressPlusPort(inPacket.
					 * getAddress(), inPacket.getPort())) { // Gets the sender
					 * so it is possible to set its // connected true
					 * m_connectedClients.get(i).setConnected(true); } }
					 */

					/*for (int i = 0; i < m_connectedClients.size(); i++) {
						System.out.println("SIZE OF CLIENTARRAY: " + m_connectedClients.size());
						broadcast(inString);
					}*/
					
					broadcast(inString);

				}

				/********* PRIVATE MESSAGE *********/

				// if _inStringArray[0] equals 2 this means that the received
				// message is private
				if (_inStringArray[0].equals("2") && checkMsgReceived) {
					ack_outString = "ack" + " " + inString;

					sendPrivateMessage(ack_outString, _inStringArray[1]);
					System.out.println(ack_outString);
					ClientConnection c;
					for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
						c = itr.next();
						// Checks if receiver of the private message exists by
						// checking username
						if (c.hasName(_inStringArray[3])) {

							// Sets connected true since this means client is
							// connected (it has sent a private message)
							c.setConnected(true);
							// Sends private message to the intended receiver
							sendPrivateMessage(inString, _inStringArray[3]);

							// Send private message to it self, so it can show
							// it on
							// chat window
							sendPrivateMessage(inString, _inStringArray[1]);
							// System.out.println("SEND PRIV MSG: " + inString);
						}

					}
				}

				/******** LIST PARTICIPANTS ********/

				// "3" means that the message is a /list-request
				else if (_inStringArray[0].equals("3") && checkMsgReceived) {
					ack_outString = "ack" + " " + inString;
					sendPrivateMessage(ack_outString, _inStringArray[1]);
					System.out.println(ack_outString);
					for (int i = 0; i < m_connectedClients.size(); i++) {
						if (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress(), inPacket.getPort())) {
							// Gets the sender so it is possible to set its
							// connected true
							m_connectedClients.get(i).setConnected(true);
						}
					}
					_outString = "8 ";
					for (int i = 0; i < m_connectedClients.size(); i++) {
						_outString += m_connectedClients.get(i).getName() + " ";
						// Send each connectedClient back to the client who
						// wrote
						// /list

					}
					// System.out.print("LIST PARTICIPANTS: " + _outString);
					sendPrivateMessage(_outString, _inStringArray[1]);
				}

				/******** LEAVE **********/

				// "4" means /leave-request from client
				else if (_inStringArray[0].equals("4") && checkMsgReceived) {
					ack_outString = "ack" + " " + _outString;
					// System.out.print(_outString);
					sendPrivateMessage(ack_outString, _inStringArray[1]);
					System.out.println(ack_outString);
					_outString = "";
					for (int i = 0; i < m_connectedClients.size(); i++) {
						_outString += "9" + " " + _inStringArray[1] + " left the chat - ";
					}
					for (int i = 0; i < m_connectedClients.size(); i++) {
						if (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress(), inPacket.getPort())) {
							/*
							 * String tempString = ""; for(int m = 2; i < m;
							 * m++){ tempString += _inStringArray[i] + " "; }
							 */

							// Broadcast leaving message to all clients
							broadcast(inString);
							// Removes client from arraylist
							m_connectedClients.remove(i);
						}
					}

				} else {

				}
			}

		} while (true);
	}

	public boolean checkMsgReceived(String timestamp) {
		boolean found = false;
		for (int i = 0; i < receivedMsg.size(); i++) {
			if (receivedMsg.get(i).equals(timestamp)) {
				found = true;
			}
		}
		if (!found) {
			receivedMsg.add(timestamp);
		}
		return found;
	}

	// Used to add new client to arraylist
	public boolean addClient(String name, InetAddress address, int port) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			if (c.hasName(name)) {
				System.out.println("Client with name " + name + " already exists!");
				// Already exists a client with this name
				return false;
			}
		}
		m_connectedClients.add(new ClientConnection(name, address, port));
		return true;
	}

	// Used to send private message
	public void sendPrivateMessage(String message, String name) {
		ClientConnection c;
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			c = itr.next();
			// If name in argument exists in connectedClients - it is possible
			// to send private message to said client
			if (c.hasName(name)) {
				c.sendMessage(message, m_socket, true);
			}
		}
	}

	// Used to send broadcast message
	public void broadcast(String message) {
		for (ClientConnection cc : m_connectedClients) {

			System.out.print("*");
			cc.sendMessage(message, m_socket, true);

		}
		/*
		 * for (Iterator<ClientConnection> itr = m_connectedClients.iterator();
		 * itr.hasNext();) { // Sends message to all clients in arraylist
		 * //System.out.println("BROADCAST " + message);
		 * itr.next().sendMessage(message, m_socket, true);
		 * System.out.print("*"); }
		 */
	}
}
