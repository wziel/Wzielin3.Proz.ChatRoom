package wzielin3.proz.server.events;

/**
 * Event that is generated when user attempts to connect to server.
 * ClientView creates this event, but doesn't assign ClientNetworkManager value.
 * ClientNetworkManager value is assigned on the server side, by the 
 * ClientNetworkManager that receives this event.
 * On the client side it is processed to created Socket object.
 * On the server side it is processed to determine whether chosen 
 * user name is available.
 * 
 * @author Wojciech Zieliñski
 */
public class LogInEvent extends NetworkEvent
{
	/**serialVersionUID for this class*/
	private static final long serialVersionUID = 1L;
	/**name with which user attempts to log in*/
	private final String userName;
	/**server name used to create Socket object*/
	private final String serverName;
	/**port value used to create Socket object*/
	private final String port;
	
	/**
	 * Constructor for LogInEvent.
	 * 
	 * @param userName - name of the client that attempts to log in
	 */
	public LogInEvent(final String userName, final String serverName,
	final String port)
	{
		this.userName = userName;
		this.serverName = serverName;
		this.port = port;
	}
	
	/**
	 * Method that returns String which represents user name chosen by the client
	 * with which he attempts to connect to the server.
	 * 
	 * @return - String which represents user name chosen by the client
	 * with which he attempts to connect to the server.
	 */
	public String getUserName()
	{
		return userName;
	}
	
	/**
	 * Method that returns string which represents name of the 
	 * server that user attempted to connect to.
	 * 
	 * @return string which represents name of the server 
	 * that user attempted to connect to.
	 */
	public String getServerName()
	{
		return serverName;
	}
	
	/**
	 * Method that returns string which represents port to which 
	 * user attempts to connect to. 
	 * 
	 * @return string which represents port to which 
	 * user attempts to connect to. The type is string because this event
	 * is generated in the client view and it's not view's job to determine
	 * whether it's a proper numerical value.
	 */
	public String getPort()
	{
		return port;
	}
}
