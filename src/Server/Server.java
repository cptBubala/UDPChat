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
	}

	private void listenForClientMessages() {
		System.out.println("Waiting for client messages... ");
		
		// Sets a resonable size of the buffer
		byte[] m_buf = new byte[2000];
		// for un-marshalling
		DatagramPacket inPacket = new DatagramPacket(m_buf, m_buf.length);
		// Used to send a marshalled message
		DatagramPacket outPacket;

		do {
			// Used so that not the same message is re-used again after 5 sec
			boolean checkMsgReceived = false;
			try {
				m_socket.receive(inPacket);
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
			String handshake = "False";
			// Put in-package in a string
			String inString = new String(inPacket.getData(), 0, inPacket.getLength()).trim();
			// Put inString in the array, splits at, and removes, space
			String[] _inStringArray = inString.split("\\s+");
			
			/*if(inString.startsWith("ack")){
				ClientConnection.checkMsg(inString);
			}*/
			

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
			
			if(_inStringArray[0].equals("ack") && checkMsgReceived){
				ClientConnection.removeMsg(inString);
			}

			/********** HANDSHAKE **********/

			if (_inStringArray[0].equals("0") && checkMsgReceived) {
				if (addClient(_inStringArray[1], inPacket.getAddress(), inPacket.getPort())) {
					for (int i = 0; i < m_connectedClients.size(); i++) {
						if (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress(), inPacket.getPort())) {
							// When handshake is done - client connected is true
							m_connectedClients.get(i).setConnected(true);
						}
					}
					System.out.println("Added Client " + _inStringArray[1] + "!");
					// Used so client knows it got the handshake
					handshake = "True";
					outPacket = new DatagramPacket(handshake.getBytes(), handshake.getBytes().length,
							inPacket.getAddress(), inPacket.getPort());
					try {
						// Sends back the package with "True" to the client
						m_socket.send(outPacket);
						_outString = _inStringArray[1] + " has joined the chat";
						// Tells all in chat that the client has joined the chat
						broadcast(_outString);
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

			/********* PRIVATE MESSAGE *********/

			// if _inStringArray[0] equals 2 this means that the received
			// message is private
			if (_inStringArray[0].equals("2") && checkMsgReceived) {
				_outString = "ack" + " " + _outString;
				System.out.print(_outString);
				broadcast(_outString);
				ClientConnection c;
				for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
					c = itr.next();
					// Checks if receiver of the private message exists by
					// checking username
					if (c.hasName(_inStringArray[3])) {
						_outString = "[PRIVATE]" + " from " + _inStringArray[1] + ": ";
						for (int i = 4; i < _inStringArray.length; i++) {
							_outString += _inStringArray[i] + " ";
						}
						// Sets connected true since this means client is
						// connected (it has sent a private message)
						c.setConnected(true);
						// Sends private message to the intended receiver
						sendPrivateMessage(_outString, _inStringArray[3]);

						String ownString = "[PRIVATE]" + " to " + _inStringArray[3] + ": ";
						for (int i = 4; i < _inStringArray.length; i++) {
							ownString += _inStringArray[i] + " ";
						}
						// Send private message to it self, so it can show
						// it on
						// chat window
						sendPrivateMessage(ownString, _inStringArray[1]);
					}

				}
			}

			/*********** BROADCAST MESSAGE **********/

			// "1" means broadcast
			else if (_inStringArray[0].equals("1") && checkMsgReceived) {
				String ack_outString = "ack" + " " + _outString;
				broadcast(ack_outString);
				System.out.print(_outString);
				//broadcast(_outString);
				for (int i = 0; i < m_connectedClients.size(); i++) {
					if (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress(), inPacket.getPort())) {
						// Gets the sender so it is possible to set its
						// connected true
						m_connectedClients.get(i).setConnected(true);
					}
				}
				// Adds all parts of the message together into one string
				for (int i = 2; i < _inStringArray.length; i++) {
					_outString += _inStringArray[i] + " ";
				}
				// Broadcasts the message to all client
				broadcast(_inStringArray[1] + ": " + _outString);
			}

			/******** LIST PARTICIPANTS ********/

			// "3" means that the message is a /list-request
			else if (_inStringArray[0].equals("3") && checkMsgReceived) {
				_outString = "ack" + " " + _outString;
				System.out.print(_outString);
				broadcast(_outString);
				for (int i = 0; i < m_connectedClients.size(); i++) {
					if (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress(), inPacket.getPort())) {
						// Gets the sender so it is possible to set its
						// connected true
						m_connectedClients.get(i).setConnected(true);
					}
				}
				// Sends "In chat now: " to client
				sendPrivateMessage("In chat now:", _inStringArray[1]);

				for (int i = 0; i < m_connectedClients.size(); i++) {
					_outString = m_connectedClients.get(i).getName();
					// Send each connectedClient back to the client who wrote
					// /list
					sendPrivateMessage(_outString, _inStringArray[0]);
				}
			}

			/******** LEAVE **********/

			// "4" means /leave-request from client
			else if (_inStringArray[0].equals("4") && checkMsgReceived) {
				_outString = "ack" + " " + _outString;
				System.out.print(_outString);
				broadcast(_outString);
				for (int i = 0; i < m_connectedClients.size(); i++) {
					if (m_connectedClients.get(i).hasAddressPlusPort(inPacket.getAddress(), inPacket.getPort())) {
						/*
						 * For-loop used to get the entire leave-message that a
						 * client can leave if wanted (like: "/leave Good bye")
						 */
						for (int j = 3; j < _inStringArray.length; j++) {
							_outString += _inStringArray[j] + " ";
						}
						// Broadcast leaving message to all clients
						broadcast(_inStringArray[1] + " left the chat - " + "' " + _outString + "'");
						// Removes client from arraylist
						m_connectedClients.remove(i);
					}
				}
			} else {

			}
			
			timeElapsed = System.currentTimeMillis() - millis;
			if(timeElapsed > 500){
				millis = System.currentTimeMillis();
				resendMsg(m_socket);
			}

		} while (true);
	}
	
	public void removeMessages(String message){
		String[] inMsg = message.split(" ");
		for(int i = 0; i < ClientConnection.sentMsg.size(); i++){
			String[] sentMsgArray = ClientConnection.sentMsg.get(i).split(" ");
			if(sentMsgArray[sentMsgArray.length-1].equals(inMsg[inMsg.length-1])){
				System.out.println("Size " + ClientConnection.sentMsg.size());
				System.out.println("Removed msg " + ClientConnection.sentMsg.size());
				ClientConnection.sentMsg.remove(i);
				break;
			}
		}
	}
	
	public void resendMsg(DatagramSocket socket){
		for(int i = 0; i < ClientConnection.sentMsg.size(); i++){
			ClientConnection c;
			for(Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();){
				c = itr.next();
				System.out.println("in resendmsg " + ClientConnection.sentMsg.get(i));
				c.sendMessage(ClientConnection.sentMsg.get(i), socket, false);
			}
			
		}
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
		for (Iterator<ClientConnection> itr = m_connectedClients.iterator(); itr.hasNext();) {
			// Sends message to all clients in arraylist
			System.out.println("BROADCAST " + message);
			itr.next().sendMessage(message, m_socket, true);
			
		}
	}
}
