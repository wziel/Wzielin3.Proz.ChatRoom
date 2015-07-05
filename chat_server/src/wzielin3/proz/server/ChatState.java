package wzielin3.proz.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;


/**
 * Objects of this class represent current state of the chat room.
 * It contains names of all users that currently are logged in to the server.
 * It also contains few messages that have recently been send. It doesn't contain
 * all the messages that have been exchanged because their number could be to great.
 * There are usually at least two messages - to help the user determine whether
 * he is up to date. The ChatState class also contains information about the user 
 * being logged in or logged out (his current status on the chat).
 * 
 * @author Wojciech Zieliñski
 */
public class ChatState implements Serializable
{
	/**serialVersionUID for this class*/
	private static final long serialVersionUID = 1L;
	/**List of currently exchanged messages. It should be sorted by Date (first old)*/
	private final ArrayList<ChatMessage> chatMessages;
	/**Set of string names of users currently logged in. TreeSet is used so 
	 * that the names are sorted - it's easier to display and browse through them*/
	private final Set<String> names;
	/**Status of the user at the moment*/
	private final UserStatus userStatus;
	
	/**
	 * Constructor that sets all the values that are needed to determine chat state
	 * 
	 * @param chatMessages - messages that have been recently exchanged and need to be sent.
	 * @param names - names of users that are currently logged in.
	 * @param userStatus current status of the user to which this messages is going to be sent.
	 */
	public ChatState(final ArrayList<ChatMessage> chatMessages,
	final Set<String> names, final UserStatus userStatus)
	{
		this.chatMessages = chatMessages;
		Collections.sort(chatMessages);
		this.names = names;
		this.userStatus = userStatus;
	}
	
	/**
	 * Method that returns messages that have recently been exchanged.
	 * 
	 * @return messages that have recently been exchanged.
	 */
	public ArrayList<ChatMessage> getChatMessages()
	{
		return new ArrayList<ChatMessage>(chatMessages);
	}
	
	/**
	 * Method that returns a set of names of all the users that are currently logged in.
	 * 
	 * @return a set of names of all the users that are currently logged in.
	 */
	public TreeSet<String> getLoggedInUserNames()
	{
		return new TreeSet<String>(names);
	}
	
	/**
	 * Method that returns current status of the user that receives this message
	 * 
	 * @return current status of the user that receives this message
	 */
	public UserStatus getUserStatus()
	{
		return userStatus;
	}
	
	/**
	 * Method that returns true if user that receives this message is logged in to the server.
	 * false if he has been logged out.
	 * 
	 * @return true if user that receives this message is logged in to the server.
	 * false if he has been logged out.
	 */
	public boolean isLoggedIn()
	{
		return userStatus == UserStatus.CONTINUES_WORKING ||
				userStatus == UserStatus.JUST_LOGGED_IN ||
				userStatus == UserStatus.MESSAGE_REJECTED;
	}
	
	/**
	 * Method that determines whether this ChatState update is compatible
	 * with user whose last received message happened on a given date
	 * 
	 * @param lastMessageDate Date of last received message by the user that 
	 * is concerned by this ChatState update.
	 * @return true if this ChatState update is compatible and can be processed
	 * by the client. false if it's not (request for new state should be sent by client).
	 */
	public boolean isCompatibleWithDate(final Date lastMessageDate)
	{
		if(chatMessages.size() == 0 || lastMessageDate == null)
		{
			return true;
		}
		for (final ChatMessage message : chatMessages)
		{
			//it means that message we have previously received is contained in this State
			if(message.getSentDate().after(lastMessageDate) == false)
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Method that deletes from this ChatState update all messages that happened
	 * before given Date (including!). Watch out - after calling this method, this ChatState
	 * changes it's fields. Calling isCompatibleWithDate after calling this method
	 * can result in wrong answer. Should always call isCompatibleWithDate before
	 * and call this method just to clean up messages that are not needed.
	 * 
	 * @param lastMessageDate
	 */
	public void deleteAllMessagesBefore(final Date lastMessageDate)
	{
		for (int i = 0; i<chatMessages.size(); ++i)
		{
			final ChatMessage message = chatMessages.get(i);
			if(message.getSentDate().after(lastMessageDate) == false)
			{
				chatMessages.remove(message);
				--i;
			}
		}
	}
	
	/**
	 * Enumeration representing current status of client that receives this status update.
	 * 
	 * @author Wojciech Zieliñski
	 */
	public enum UserStatus
	{
		/**User is logged in, nothing special happened, just received state update.*/
		CONTINUES_WORKING,
		/**User just logged in, his connection has been accepted, message exchange possible.*/
		JUST_LOGGED_IN,
		/**User has sent a message but it has been rejected by the server. But he is still logged in.*/
		MESSAGE_REJECTED,
		/**User sent log out request and this request has been accepted. Connection will be closed.*/
		LOGGED_OUT,
		/**User attempted log in, but his user name has been rejected. Should try to change it.*/
		USER_NAME_REJECTED,
		/**User has been rejected by the server for unknown reason.*/
		REJECTED
	}
}
