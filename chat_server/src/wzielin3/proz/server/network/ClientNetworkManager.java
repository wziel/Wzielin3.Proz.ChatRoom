package wzielin3.proz.server.network;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import wzielin3.proz.server.ChatState;
import wzielin3.proz.server.events.ApplicationEvent;
import wzielin3.proz.server.events.LogOutEvent;
import wzielin3.proz.server.events.NetworkEvent;

/**
 * class that is responsible for connection with one client. When object of this class
 * is created and connection succeeds, it's still not fully connected. It waits for the
 * login request from client to see it's user name. Then if this user name is available
 * connection is confirmed. Only then the connection is fully working
 * and messages can be send over it.
 * 
 * @author Wojciech Zieliñski
 */
public class ClientNetworkManager extends Thread
{
	/**socket of this clients connection*/
	private final Socket socket;
	/**stream from which objects are read*/
	private final ObjectInputStream objectInputStream;
	/**stream to which objects are sent*/
	private final ObjectOutputStream objectOutputStream;
	/**queue to which received objects are sent*/
	private final BlockingQueue<ApplicationEvent> eventsQueue;
	/**boolean value telling if thread should stop. set to false on close() invoked*/
	private volatile boolean keepGoing;
	/**boolean value telling if this client is logged in and can exchange messages*/
	private volatile boolean isLoggedIn;
	
	/**
	 * constructor that initializes connection with specified client.
	 * to start listening to client the start() method needs to be invoked.
	 * from the socket.
	 * 
	 * @param socket - socket to which this client is going to connect
	 * @param eventsQueue - blocking queue to which this client network manager
	 * is going to send events concerning connection with his client
	 * @throws IOException if couldn't create ObjectStreams
	 */
	public ClientNetworkManager(Socket socket,
			BlockingQueue<ApplicationEvent> eventsQueue) throws IOException
	{
		this.socket = socket;
		this.eventsQueue = eventsQueue;
		objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		objectInputStream = new ObjectInputStream(socket.getInputStream());
		keepGoing = true;
		isLoggedIn = false; 
	}
	
	/**
	 * method that sets current status of being logged in for this user as "true".
	 * This method should be invoked when user's name is accepted and 
	 * he can start to exchange messages.
	 * If user is logged in he can exchange messages. in different case he can not
	 */
	public void setLoggedIn()
	{
		this.isLoggedIn = true;
	}
	
	/**
	 * method that returns boolean value representing current status 
	 * of being logged in for this user.
	 * 
	 * @return true if user is logged in. false if he is not.
	 */
	public boolean getIsLoggedIn()
	{
		return isLoggedIn;
	}
	
	/**
	 * method that sends current chat state to the client concerned
	 */
	public void send(ChatState state)
	{
		try
		{
			objectOutputStream.writeObject(state);
		}
		catch (IOException e)
		{
			//if connection is corrupted run method detects it and sends event
			//to controller which then closes this client. no need to do it here
		}
	}
	
	/**
	 * method that closes connection with this client.
	 * should be invoked before removing this client.
	 * calling this method closes this client and there is no going back. after
	 * that object is useless.
	 */
	public void close()
	{
		keepGoing = false;
		isLoggedIn = false;
		try
		{
			if(objectInputStream != null) objectInputStream.close();
		}
		catch (Exception e) { }
		
		try
		{
			if(objectOutputStream != null)  objectOutputStream.close();
		}
		catch (Exception e) { }
		
		try
		{
			if(socket != null) socket.close();
		}
		catch (Exception e) { }
	}
	
	/**
	 * method responsible for listening to the client
	 */
	@Override
	public void run()
	{
		/**time to sleep between two messages received by client. Additional defense mechanism
		 * to protect from rogue clients that send too many messages*/
		final int MILLISECONDS_BETWEEN_MESSAGES = 100;
		while(true)
		{
			try
			{
				NetworkEvent event = (NetworkEvent) objectInputStream.readObject();
				event.setClientNetworkManager(this);
				eventsQueue.put(event);
			}
			catch (ClassNotFoundException | InvalidClassException | InterruptedException e)
			{
				continue;
			}
			catch (IOException e)
			{
				//if serverNetworkManager closes me i don't want to send event to queue
				if(keepGoing == false)
				{
					return;
				}
				//stream failed. have to tell the controller that logout is needed
				NetworkEvent event = new LogOutEvent();
				event.setClientNetworkManager(this);
				try
				{
					eventsQueue.put(event);
					return;
				}
				catch (InterruptedException e1)
				{
				}
			}
			try
			{
				Thread.sleep(MILLISECONDS_BETWEEN_MESSAGES);
			}
			catch (InterruptedException e)
			{
			}
		}
	}
}
