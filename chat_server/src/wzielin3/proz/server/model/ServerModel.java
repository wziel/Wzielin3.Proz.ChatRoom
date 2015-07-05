package wzielin3.proz.server.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import wzielin3.proz.server.ChatMessage;
import wzielin3.proz.server.ChatState;

/**
 * Model of this application. It stores names of all users currently
 * connected, and all chat messages that have been exchanged.
 * 
 * @author Wojciech Zieliñski
 */
public class ServerModel
{
	/**maximum length of user's name*/
	private static final int NAME_MAX_LENGTH = 15;
	/**time in milliseconds acceptable between two messages without re sending*/
	private static final int TIME_MAX_DIFFERENCE = 500;
	/**list of all delivered messages*/
	private final ArrayList<ChatMessage> messages;
	/**Mapping client's names to their models*/
	private final Map<String, ClientModel> nameToModelMap;
	
	/**
	 * Constructor that initializes the model of this server.
	 */
	public ServerModel()
	{
		messages = new ArrayList<ChatMessage>();
		nameToModelMap = new HashMap<String, ClientModel>();
		//add one message so that the list is not empty and clients that connect
		//have a date of last message
		addMessage(new ChatMessage("Server has been created", "Server", new Date()));
	}
	
	/**
	 * method that checks if a client who wants to connect to the server
	 * has a user name that is proper.
	 * 
	 * @param userName name o the client who wants to connect
	 * @return true if client can connect to the server. false in different case.
	 */
	public boolean isNameAllowed(final String userName)
	{
		if(nameToModelMap.containsKey(userName) || userName.length() > NAME_MAX_LENGTH
		|| userName.length() == 0)
		{
			return false;
		}
		return true;
	}

	/**
	 * method that adds new client's to the set
	 * 
	 * @param userName - name of a client to add
	 */
	public void addUser(final String userName)
	{
		nameToModelMap.put(userName, new ClientModel(userName));
	}
	
	/**
	 * method that removes given client's name from the list 
	 * 
	 * @param username - name of a client to remove
	 */
	public void removeUser(final String username)
	{
		nameToModelMap.remove(username);
	}
	
	/**
	 * method that adds message to the messages container
	 * 
	 * @param chatMessage
	 */
	public void addMessage(final ChatMessage chatMessage)
	{	
		messages.add(chatMessage);
	}
	
	
	/**
	 * method that returns all user names of connected clients
	 */
	public TreeSet<String> getAllUserNames()
	{
		return new TreeSet<String>(nameToModelMap.keySet());
	}
	
	/**
	 * method that returns all messages that happened after specified date.
	 * Also returns message that happened at the same moment.
	 * 
	 * @param date Date object after all messages should be returned.
	 * @return list of messages that happened after specified date.
	 */
	private ArrayList<ChatMessage> getAllMessagesAfter(final Date date)
	{
		final ArrayList<ChatMessage> list = new ArrayList<>();
		if(date == null)
		{
			return list;
		}
		//iterate from back to get only latest messages
		for(int i = messages.size() - 1; i >= 0; --i)
		{
			ChatMessage message = messages.get(i);
			if(message.getSentDate().before(date) == false)
			{
				list.add(new ChatMessage(message));
			}
			else 
			{
				break;
			}
		}
		Collections.sort(list);
		return list;
	}	
	
	/**
	 * Method that returns list of messages that have recently been exchanged.
	 * 
	 * @return list of messages that have recently been exchanged. number of messages on the list is arbitrary.
	 */
	private ArrayList<ChatMessage> getRecentMessages()
	{
		//send two, one, or empty
		final ArrayList<ChatMessage> list = new ArrayList<>();
		if(messages.size() > 1)
		{
			list.add(messages.get(messages.size() - 2));
			list.add(messages.get(messages.size() - 1));
		}
		else if(messages.size() > 0)
		{
			list.add(messages.get(messages.size() - 1));
		}
		return list;
	}
	
	/**
	 * method returning current chat state
	 * 
	 * @param userStatus status of the user who is concerned by this ChatState update
	 * @return current chat state
	 */
	public ChatState getChatState(final ChatState.UserStatus userStatus)
	{
		return new ChatState(getRecentMessages(), 
				new TreeSet<String>(nameToModelMap.keySet()), userStatus);
	}

	/**
	 * method returning current chat state with all the messages that happened
	 * after given date
	 * 
	 * @param date - since when messages should be included
	 * @param userStatus status of the user who is concerned by this ChatState update
	 * @return - current chat state with all expected messages
	 */
	public ChatState getChatStateWithMessagesAfter(final Date date, final ChatState.UserStatus userStatus)
	{
		return new ChatState(getAllMessagesAfter(date), 
				new TreeSet<String>(nameToModelMap.keySet()), userStatus);
	}
	
	/**
	 * method that checks whether date give as argument is close enough or after the date
	 * of last received message.
	 * 
	 * @param date date to be checked
	 * @return true if given date is close enough or after the date of last received message.
	 * false if this date is before last received message date.
	 */
	public boolean isValidDate(final Date date)
	{
		if(date == null)
		{
			return false;
		}
		if(messages.size() == 0)
		{
			return true;
		}
		final Date lastMessageDate = messages.get(messages.size()-1).getSentDate();
		long differenceInMiliseconds = lastMessageDate.getTime() - date.getTime();
		if(differenceInMiliseconds > TIME_MAX_DIFFERENCE) //more than half a second is bad
		{
			return false;
		}
		return true;
	}
	
}
