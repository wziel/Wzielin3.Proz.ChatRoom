package wzielin3.proz.server.events;

import java.util.Date;

/**
 * Event that is created when client application finds out 
 * that some messages are missing and it wants to get the server to re send them.
 * 
 * @author Wojciech Zieliñski
 */
public class StateRequestEvent extends NetworkEvent
{
	/**serialVersionUID for this class*/
	private static final long serialVersionUID = 1L;
	/**Date since which all messages should be re send*/
	private final Date lastMessageDate;
	
	/**
	 * Basic constructor that takes as a parameter date since which all messages should be re send.
	 * 
	 * @param lastMessageDate date since which all messages should be re send.
	 */
	public StateRequestEvent(final Date lastMessageDate)
	{
		this.lastMessageDate = lastMessageDate;
	}
	
	/**
	 * Method returning Date after which all messages should be re send
	 * 
	 * @return - all messages that happened after this date should be re send
	 */
	public Date getLastMessageDate()
	{
		return lastMessageDate;
	}
}
