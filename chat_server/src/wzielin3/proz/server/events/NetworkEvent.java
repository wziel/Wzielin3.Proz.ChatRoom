package wzielin3.proz.server.events;

import wzielin3.proz.server.network.ClientNetworkManager;

/**
 * events that are sent over network must inherit this class.
 * It allows the server to assign ClientNetworkManager object to it,
 * so that it's known where does it come from.
 * 
 * @author Wojciech Zieliñski
 */
public abstract class NetworkEvent extends ApplicationEvent
{
	/**serialVersionUID for this class*/
	private static final long serialVersionUID = 1L;
	/**connection to client that has created this event*/
	private ClientNetworkManager client;
	
	/**
	 * Basic constructor for this class
	 */
	public NetworkEvent()
	{
		client = null;
	}
	
	/**
	 * Method that sets the ClientNetworkManager that received this event.
	 * It should be used only in the network module of the server, when the
	 * ClientNetworkManager is known.
	 * 
	 * @param client - ClientNetworkManager that received this event.
	 */
	public void setClientNetworkManager(final ClientNetworkManager client)
	{
		this.client = client;
	};
	
	/**
	 * Method that returns ClientNetworkManager that received this event.
	 * It should be used only in the network module of the server, when the
	 * ClientNetworkManager is known.
	 * 
	 * @return ClientNetworkManager that received this event.
	 */
	public ClientNetworkManager getClientNetworkManager()
	{
		return client;
	};
}
