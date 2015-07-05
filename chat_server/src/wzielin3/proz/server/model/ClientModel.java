package wzielin3.proz.server.model;

import java.util.Date;

/**
 * Class modeling client's status in the chat. It contains his name and date of him joining the chat.
 * 
 * @author Wojciech Zieliñski
 */
class ClientModel
{
	/**name of the user*/
	private final String userName;
	/**Date on which user was added to the model*/
	private final Date creationDate;
	
	/**
	 * Basic constructor that takes the name of the user
	 * 
	 * @param userName - name of the user
	 */
	public ClientModel(final String userName)
	{
		this.userName = userName;
		this.creationDate = new Date();
	}
	
	/**
	 * method that returns the name of the user
	 * 
	 * @return the name of the user
	 */
	public String getUserName()
	{
		return userName;
	}
	
	/**
	 * Method that returns date of the user joining the chat
	 * 
	 * @return date of the user joining the chat
	 */
	public Date getCreationDate()
	{
		return creationDate;
	}
}
