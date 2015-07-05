package wzielin3.proz.server.events;


/**
 * when connection with client is lost this event is sent
 * from the ClientNetworkManager to the blocking queue.
 * Objects of this event can either be created on the client side, when
 * user decides to log out, or in the network module when connection with client
 * simply is lost.
 * 
 * @author Wojciech Zieliñski
 */
public class LogOutEvent extends NetworkEvent
{
	/**serialVersionUID for this class*/
	private static final long serialVersionUID = 1L;
}
