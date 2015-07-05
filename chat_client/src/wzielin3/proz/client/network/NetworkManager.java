package wzielin3.proz.client.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import wzielin3.proz.client.view.ClientMainView;
import wzielin3.proz.server.ChatMessage;
import wzielin3.proz.server.ChatState;
import wzielin3.proz.server.events.ApplicationEvent;
import wzielin3.proz.server.events.LogInEvent;
import wzielin3.proz.server.events.LogOutEvent;
import wzielin3.proz.server.events.MessageEvent;
import wzielin3.proz.server.events.StateRequestEvent;

/**
 * Class responsible for client's connection to the server.
 * It sends events and receives status updates from server which then
 * are sent to the view.
 * 
 * @author Wojciech Zieliñski
 */
public class NetworkManager
{
	/**view that communicates with this NetworkManager*/
	private final ClientMainView view;
	/**HasMap that maps events to strategies objects that can handle them*/
	private final Map<Class<? extends ApplicationEvent>, NetworkStrategy> eventToStrategyMap;
	/**BlockingQueue to which event from view are send*/
	private final BlockingQueue<ApplicationEvent> eventsBlockingQueue;
	/**stream that receives objects from the server*/
	private ObjectInputStream objectInputStream;
	/**stream that sends objects to the server*/
	private ObjectOutputStream objectOutputStream;
	/**socket for client-server connections*/
	private Socket socket;
	/**Date of last received message*/
	private Date lastMessageDate;
	
	/**
	 * Constructor that sets the view and blockingQueue for this manager.
	 * 
	 * @param view ClientMainView that communicates with this manager.
	 * @param eventsBlockingQueue BlockingQueue that stores events from the view. 
	 * Events are read in this manager and send to the server.
	 * Some events are handled here partially (e.g. LogInEvent - creating Socket etc.). 
	 */
	public NetworkManager(final ClientMainView view, 
	final BlockingQueue<ApplicationEvent> eventsBlockingQueue)
	{
		this.view = view;
		this.eventsBlockingQueue = eventsBlockingQueue;
		lastMessageDate = null;
		eventToStrategyMap = new HashMap<Class<? extends ApplicationEvent>, NetworkStrategy>();
		eventToStrategyMap.put(LogInEvent.class, new LogInStrategy());
		eventToStrategyMap.put(MessageEvent.class, new MessageStrategy());
		eventToStrategyMap.put(LogOutEvent.class, new LogOutStrategy());	
	}
	
	/**
	 * Method that starts this network manager. Needs to be invoked to start
	 * taking events from queue, establish connection etc.
	 */
	public void start()
	{
		new StateRequestThread().start();
		while(true)
		{
			ApplicationEvent event = null;
			try
			{
				event = eventsBlockingQueue.take();
			}
			catch (InterruptedException e)
			{
				continue;
			}
			if(eventToStrategyMap.containsKey(event.getClass()))
			{
				eventToStrategyMap.get(event.getClass()).execute(event);
			}
		}
	}
	
	/**
	 * Method that tries connecting this manager to the server.
	 * 
	 * @param host host name to connect
	 * @param port port to connect
	 * @throws IOException when connection attempt fails
	 */
	private void connect(final String host, final int port) throws IOException
	{
		socket = new Socket(host, port);
		objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		objectInputStream = new ObjectInputStream(socket.getInputStream());
	}
	
	/**
	 * Method that disconnects this manager from the server. 
	 * It doesn't influence the view.
	 */
	private void disconnect()
	{
		try
		{
			objectInputStream.close();
		}
		catch (final IOException | NullPointerException e) 
		{ 
		}
		
		try
		{
			objectOutputStream.close();
		}
		catch (final IOException | NullPointerException e) 
		{ 
		}
		
		try
		{
			socket.close();
		}
		catch (final IOException | NullPointerException e) 
		{ 
		}
		
		objectInputStream = null;
		objectOutputStream = null;
		socket = null;
		lastMessageDate = null;
	}
	
	/**
	 * Method that attempts to send event to the server. 
	 * 
	 * @param event ApplicationEvent to be send to the server
	 */
	private void sendEventToServer(final ApplicationEvent event)
	{
		if(objectOutputStream == null)
		{
			return; //nowhere to send
		}
		try
		{
			synchronized (objectOutputStream)
			{
				objectOutputStream.writeObject(event);
			}
		}
		catch (final IOException e)
		{
			disconnect();
		}
	}
	
	/**
	 * Thread that listens to the server and receives sent objects.
	 * the objects of type ChatState are forwarded to the view to be displayed
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class ServerListener extends Thread
	{
		@Override
		public void run()
		{
			while(true)
			{
				ChatState state = null;
				try
				{
					state = (ChatState) objectInputStream.readObject();
				}
				catch(final IOException | NullPointerException | ClassNotFoundException e)
				{
					view.setDisconnected();
					disconnect(); 
					return;
				}
				if(state.isLoggedIn() == false)
				{
					view.setChatState(state);
					disconnect();
					return;
				}
				handleChatStateChange(state);	
			}
		}
	}
	
	/**
	 * Method that takes care of all the operations that need to be executed
	 * when a new ChatState object is received. It takes care of changing
	 * the view and sending request for messages to server if needed.
	 * 
	 * @param state newly received ChatState object
	 */
	private void handleChatStateChange(final ChatState state)
	{
		if(lastMessageDate == null)
		{
			lastMessageDate = getLatestDate(state.getChatMessages());
		}
		if(state.isCompatibleWithDate(lastMessageDate))
		{
			state.deleteAllMessagesBefore(lastMessageDate);
			view.setChatState(state);
			final Date lastMessageDate = getLatestDate(state.getChatMessages());
			if(lastMessageDate != null)
			{
				this.lastMessageDate = lastMessageDate;
			}
		}
	}
	
	/**
	 * Method that searches through the list of ChatMessages for a message
	 * which was sent later than all the others. The value is then returned.
	 * If the list is empty, null is returned. 
	 * 
	 * @param messages list of ChatMessages to be searched
	 */
	private Date getLatestDate(final List<ChatMessage> messages)
	{
		if(messages.size() == 0)
		{
			return null;
		}
		Date lastDate = messages.get(0).getSentDate();
		for (final ChatMessage message : messages)
		{
			if(message.getSentDate().after(lastDate))
			{
				lastDate = message.getSentDate();
			}
		}
		return lastDate;
	}
	
	/**
	 * Objects of this class are responsible for sending requests for new ChatState
	 * to the server every period of time. It helps making sure that user is up to date
	 * with his messages.
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class StateRequestThread extends Thread
	{
		/**static value defining how long thread should sleep between sending requests*/
		static final int SLEEP_MILISECONDS = 2000;
		
		/**
		 * method that sends periodically request to server to state update.
		 */
		@Override
		public void run()
		{
			while (true)
			{
				sendEventToServer(new StateRequestEvent(lastMessageDate));
				try
				{
					Thread.sleep(SLEEP_MILISECONDS);
				}
				catch (final InterruptedException e){}
			}
		}
	}
	
	/**
	 * Common base for all classes that can handle events from the view
	 * 
	 * @author Wojciech Zieliñski
	 */
	private abstract class NetworkStrategy
	{
		/**
		 * Method that handles Event.
		 *  
		 * @param event ApplicationEvent to be handled.
		 */
		public abstract void execute(final ApplicationEvent event);
	}
	
	/**
	 * Class that responds to LogInEvent.
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class LogInStrategy extends NetworkStrategy
	{
		/**
		 * Method that handles ApplicationEvent of type LogInEvent. If event is
		 * not and instance of that class execution ends. If event is and instance of
		 * LogInEvent then NetworkManager attempts to connect the server - if it fails
		 * view is notified and connection is ended. If connection succeeds the same
		 * LogInEvent is passed to the server to be handled properly. Connection success
		 * doesn't change the view. Server must first reply with state update message.
		 * 
		 * @param event Application event to be handled. Should be an instance of LogInEvent.
		 */
		@Override
		public void execute(final ApplicationEvent event)
		{
			if(event instanceof LogInEvent == false)
			{
				return;
			}
			try
			{
				final LogInEvent logInEvent = (LogInEvent) event;
				final String serverName = logInEvent.getServerName();
				final int port = Integer.parseInt(logInEvent.getPort());
				connect(serverName, port);
				new ServerListener().start();
			}
			catch (final IOException | NumberFormatException e)
			{
				disconnect();
				view.setDisconnected();
				return;
			}
			sendEventToServer(event);
		}
	}

	/**
	 * Strategy that handles MessageEvent.
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class MessageStrategy extends NetworkStrategy
	{
		/**
		 * Method that forwards MessageEvent to the server.
		 * It that checks whether given ApplicationEvent is of type MessageEvent.
		 * If it's not execution finishes. If it is of type MessageEvent, then
		 * the date of last received message is attached to it and it's forwarded to the server.
		 * 
		 * @param event ApplicationEvent to be handled. Should be of type MessageEvent.
		 */
		@Override
		public void execute(final ApplicationEvent event)
		{
			if(event instanceof MessageEvent == false)
			{
				return;
			}
			((MessageEvent)event).setPreviousMessageDate(lastMessageDate);
			sendEventToServer(event);
		}
	}
	
	/**
	 * Strategy that responds to LogOutEvent.
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class LogOutStrategy extends NetworkStrategy
	{
		/**
		 * Method that handles ApplicationEvent of type LogOutEvent.
		 * If ApplicationEvent is not of type LogOutEvent execution ends.
		 * In different case the same LogOutEvent is simply forwarded to server. 
		 */
		@Override
		public void execute(final ApplicationEvent event)
		{
			if(event instanceof LogOutEvent == false)
			{
				return;
			}
			sendEventToServer(event);
		}
	}
}
