package wzielin3.proz.server;

import java.io.Serializable;
import java.util.Date;

/**
 * Class that contains information about string message. It contains information
 * about the author, sent date and content of the message. Objects of this type
 * can be compared (by sent date).
 * 
 * @author Wojciech Zielinski
 */
public class ChatMessage implements Serializable, Comparable<ChatMessage>
{
	/**serialVersionUID for this class*/
	private static final long serialVersionUID = 1L;
	/**Content of the message*/
	private final String content;
	/**Author of the message*/
	private final String author;
	/**Creation Date of the message*/
	private final Date sentDate;
	
	/**
	 * Constructor for this class.
	 * 
	 * @param content - content of the message to be send
	 * @param author - user name of the sending person
	 * @param sentDate - when was this message sent
	 */
	public ChatMessage(final String content,final String author, final Date sentDate)
	{
		this.content = content; 
		this.author = author;
		this.sentDate = sentDate;
	}
	
	/**
	 * Copy constructor for a chatMessage.
	 * 
	 * @param chatMessage ChatMEssage to be copied
	 */
	public ChatMessage(final ChatMessage chatMessage)
	{
		this.content = chatMessage.content;
		this.author = chatMessage.author;
		this.sentDate = chatMessage.sentDate;
	}
	
	/**
	 * Method returning content of the message
	 * 
	 * @return content of the message
	 */
	public String getContent()
	{
		return content;
	}
	
	/**
	 * Method returning author of the message
	 * 
	 * @return author of the message
	 */
	public String getAuthor()
	{
		return author;
	}
	
	/**
	 * Method returning Date on which this message was sent
	 * 
	 * @return Date on which this message was sent
	 */
	public Date getSentDate()
	{
		return sentDate;
	}

	/**
	 * Method that compares two messages, by their sending dates.
	 */
	@Override
	public int compareTo(final ChatMessage o)
	{
		if(this.sentDate.equals(o.sentDate)) return 0;
		if(this.sentDate.before(o.sentDate)) return -1;
		return 1;
	}
}
