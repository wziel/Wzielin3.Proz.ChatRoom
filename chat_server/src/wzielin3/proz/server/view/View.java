package wzielin3.proz.server.view;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import wzielin3.proz.server.events.ApplicationEvent;
import wzielin3.proz.server.network.ServerNetworkManager;

/**
 * Class responsible for view of the server. It displays console and
 * asks for the port number for the server to start. Then based on the
 * port number returns ServerNetworkManager.
 * 
 * @author Wojciech Zieliñski
 */
public class View
{
	/**Scanner to read from console*/
	private static final Scanner in = new Scanner(System.in); 
	/**BlockingQueue needed only to create ServerNetworkManager for the Server*/
	private final BlockingQueue<ApplicationEvent> blockingQueue;
	
	/**
	 * Constructor that takes BlockingQueue to which events will be send
	 * 
	 * @param blockingQueue BlockingQueue to which events will be send
	 */
	public View(final BlockingQueue<ApplicationEvent> blockingQueue)
	{
		this.blockingQueue = blockingQueue;
	}
	
	/**
	 * Method that creates ServerNetworkManager based on user input.
	 * 
	 * @return ServerNetworkManager for this server to run.
	 */
	public ServerNetworkManager getServerNetworkManagerFromConsole()
	{
		ServerNetworkManager networkManager = null;
		while (true)
		{
			try
			{
				int port = getPortFromConsole();
				networkManager = new ServerNetworkManager(port, blockingQueue);
			}
			catch (Exception e)
			{
				System.out.println("Couldn't create server on selected port. Try again.");
				continue;
			}
			return networkManager;
		}
	}
	
	/**
	 * Method reading proper integer port value from console.
	 * 
	 * @return port for ServerNetworkManager
	 */
	private int getPortFromConsole()
	{
		System.out.println("Select port for the server: ");
		while(true)
		{
			String portString = in.nextLine();
			int port;
			try
			{
				port = Integer.parseInt(portString);
			}
			catch (NumberFormatException e)
			{
				System.out.println("Wrong input. One number expected.");
				continue;
			}
			return port;
		}
	}
}
