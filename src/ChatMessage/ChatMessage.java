package ChatMessage;

public class ChatMessage {
	String m_name;
	String m_command;
	String m_receiver;
	int m_timeStamp;
	String m_message;
	
	public ChatMessage(String name, String command, String receiver, int timeStamp, String message){
		m_name = name;
		m_command = command;
		m_receiver = receiver;
		m_timeStamp = timeStamp;
		m_message = message;
	}
}
