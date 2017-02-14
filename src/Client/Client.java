package Client;

import java.awt.event.*;

public class Client implements ActionListener {

	private String m_name = null;
	private final ChatGUI m_GUI;
	private ServerConnection m_connection = null;
	private long millis;
	private long timeElapsed;
	
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: java Client serverhostname serverportnumber username");
			System.exit(-1);
		}

		try {
			Client instance = new Client(args[2]);
			instance.connectToServer(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			System.err.println("Error: port number must be an integer.");
			System.exit(-1);
		}
	}

	private Client(String userName) {

		m_name = userName;
		// Start up GUI (runs in its own thread)
		m_GUI = new ChatGUI(this, m_name);
		millis = System.currentTimeMillis();
	}

	private void connectToServer(String hostName, int port) {
		// Create a new server connection
		m_connection = new ServerConnection(hostName, port);

		// Handshake returns true if connection has been made
		// Now we can listen for server messages
		if (m_connection.handshake(m_name)) {
			listenForServerMessages();
		} else {
			System.err.println("Unable to connect to server");
		}
	}

	// ENDA HÄR ÄR SKRIVA UT MEDDELANDE I FÖNSTER - ALLT ANNAT FIXAS I RECEIVECHATMESSAGE()
	private void listenForServerMessages() {
		do {		
			String temp = m_connection.receiveChatMessage();
			String[] newStringArray = temp.split(" ");
			String newString ="";
			if(temp.isEmpty()){
				//do nothing
			}else if(temp.startsWith("ack")){
				// do nada
			}
			else{
				

				for(int i = 0; i < newStringArray.length - 1;i++){
					newString = newString + newStringArray[i] + " ";
				}
				System.out.println("newstring is " + newString);
				
			}
			
			m_GUI.displayMessage(newString);
			timeElapsed = System.currentTimeMillis() - millis;
			if(timeElapsed > 500){
				millis = System.currentTimeMillis();
				m_connection.resendMsg();
			}

		} while (true);
	}

	/*
	 * Used number codes based on what the input from client is: 1 - broadcast.
	 * 2 - private. 3 - list. Numbers are added to the message so that in server
	 * it is easy to decide what kind of message it is
	 */

	// Sole ActionListener method; acts as a callback from GUI when user hits
	// enter in input field
	@Override
	public void actionPerformed(ActionEvent e) {
		// Since the only possible event is a carriage return in the text input
		// field,
		// the text in the chat input field can now be sent to the server.
		String outMsg = m_GUI.getInput();
		// If incoming message starts with a / it means that it might be a
		// command
		// This is what is checked here.
		// In order to make it possible to hit enter when no message has been
		// typed,
		// the check for everything is only done if the length of outMsg is > 0
		String[] splitMsg = outMsg.split("\\s+");
		if (outMsg.length() > 0) {
			/*if (outMsg.substring(0, 1).equals("/")) {
				if (splitMsg[0].equals("/tell") || splitMsg[0].equals("/Tell")) {
					outMsg = "2 " + m_name + " " + outMsg;
				} else if (splitMsg[0].equals("/list") || splitMsg[0].equals("/List")) {
					outMsg = "3 " + m_name + " " + outMsg;
				} else if (splitMsg[0].equals("/leave") || splitMsg[0].equals("/Leave")) {
					outMsg = "4 " + m_name + " " + outMsg;
				} else {
					outMsg = "1 " + m_name + " " + outMsg;
				}

				// This is the broadcast
			} else {*/
				outMsg = "1 " + m_name + " " + outMsg;
			//}

			m_connection.sendChatMessage(outMsg, true);
			m_GUI.clearInput();
		}

	}
}
