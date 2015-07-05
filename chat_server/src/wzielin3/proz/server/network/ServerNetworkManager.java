package wzielin3.proz.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import wzielin3.proz.server.ChatState;
import wzielin3.proz.server.events.ApplicationEvent;

/**
 * Class responsible for listening for new client connections. It has
 * a collection of all connected clients. It can broadcast messages to all
 * connected clients
 * 
 * @author Wojciech Zieliñski
 */
public class ServerNetworkManager extends Thread
{
	/**set of clients connected to this manager. has to be synchronized because two threads
	 * may use it (main thread by invoking e.g. removeClient() and this thread in run() method.*/
	private final Set<ClientNetworkManager> clients;
	/**queue to which this manager sends its events*/
	private final BlockingQueue<ApplicationEvent> eventsQueue;
	/**Socket to listen for clients*/
	private final ServerSocket serverSocket;
	/**Maximum number of clients currently connected to server*/
	private final static int MAX_CLIENTS_COUNT = 100;
	
	/**
	 * constructor that initializes this network manager. to start listening for
	 * connections the start() method needs to be invoked
	 * 
	 * @param port - port on which the network manager will listen for users
	 * @throws IOException when ServerSocket can't be created on selected port
	 */
	public ServerNetworkManager(final int port, final BlockingQueue<ApplicationEvent> eventsQueue)
	throws IOException
	{
		this.serverSocket = new ServerSocket(port);
		clients = new HashSet<ClientNetworkManager>();
		this.eventsQueue = eventsQueue;
	}
	
	/**
	 * Method that removes given client from the clients set.
	 * 
	 * @param client client to be removed
	 */
	public void removeClient(final ClientNetworkManager client)
	{
		synchronized (clients)
		{
			clients.remove(client);
		}
	}
	
	/**
	 * method that sends a message to every user currently connected to the server
	 * 
	 * @param state ChatState to be broadcasted
	 */
	public void broadcast(final ChatState state)
	{
		synchronized (clients)
		{
			for (final ClientNetworkManager client : clients)
			{
				if(client.getIsLoggedIn())
					client.send(state);
			}
		}
	}
	
	/**
	 * method responsible for listening for new clients connections.
	 */
	@Override
	public void run()
	{
		while (true)
		{
			final Socket clientSocket = acceptNewClientSocket();
			synchronized (clients)
			{
				ClientNetworkManager clientManager = null;
				try
				{
					clientManager = new ClientNetworkManager(clientSocket, eventsQueue);
				}
				catch (IOException e)
				{ 
					try
					{
						clientSocket.close();
					}
					catch (IOException e1)
					{
					}
					continue; /*only one client connection failed. continue.*/
				}	
				clients.add(clientManager);
				clientManager.start();
			}
		}
	}
	
	/**
	 * Method that accepts new client connection and returns the socket
	 * to which client is connected.
	 * @return the socket
	 * to which client is connected.
	 */
	private Socket acceptNewClientSocket()
	{
		while(true)
		{
			Socket clientSocket = null;
			try
			{
				clientSocket = serverSocket.accept();
			}
			catch (IOException e)
			{
				continue;
			}
			if(clients.size() > MAX_CLIENTS_COUNT)
			{
				try
				{
					clientSocket.close();
				}
				catch (IOException e)
				{
					continue;
				}
			}
			return clientSocket;
		}
	}
}
