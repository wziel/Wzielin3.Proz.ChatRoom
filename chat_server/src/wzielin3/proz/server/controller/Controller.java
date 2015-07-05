package wzielin3.proz.server.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import wzielin3.proz.server.ChatMessage;
import wzielin3.proz.server.ChatState;
import wzielin3.proz.server.ChatState.UserStatus;
import wzielin3.proz.server.events.ApplicationEvent;
import wzielin3.proz.server.events.LogInEvent;
import wzielin3.proz.server.events.LogOutEvent;
import wzielin3.proz.server.events.MessageEvent;
import wzielin3.proz.server.events.StateRequestEvent;
import wzielin3.proz.server.model.ServerModel;
import wzielin3.proz.server.network.ClientNetworkManager;
import wzielin3.proz.server.network.ServerNetworkManager;

/**
 * Class responsible for handling connection between the view-network-model.
 * 
 * @author Wojciech Zieliñski
 */
public class Controller
{
	/**Queue from which events are read*/
	private final BlockingQueue<ApplicationEvent> eventsBlockingQueue;
	/**NetworkManager of this server*/
	private final ServerNetworkManager networkManager;
	/**This server's model*/
	private final ServerModel model;
	/**Mapping ApplicationEvents to Strategy objects that handle them*/
	private final Map<Class<? extends ApplicationEvent >, ServerStrategy> eventsToStrategyMap;
	/**Mapping ClientNetworkManagers to user names stored in the model*/
	private final Map<ClientNetworkManager, String> clientToUserNameMap;
	
	/**
	 * Constructor that initializes this controller. To start working the start() method needs to be invoked.
	 * 
	 * @param eventsBlockingQueue blockingQueue from which events from network and view are read
	 * @param networkManager used by this controller to send messages over network
	 * @param model model for the server that uses this controller
	 */
	public Controller(final BlockingQueue<ApplicationEvent> eventsBlockingQueue,
			final ServerNetworkManager networkManager, final ServerModel model)
	{
		this.eventsBlockingQueue = eventsBlockingQueue;
		this.networkManager = networkManager;
		this.model = model;
		clientToUserNameMap = new HashMap<ClientNetworkManager, String>();
		eventsToStrategyMap = 
				new HashMap<Class<? extends ApplicationEvent>, Controller.ServerStrategy>();
		eventsToStrategyMap.put(LogInEvent.class, new LoginInStrategy());
		eventsToStrategyMap.put(MessageEvent.class, new MessageStrategy());
		eventsToStrategyMap.put(LogOutEvent.class, new LogOutStrategy());
		eventsToStrategyMap.put(StateRequestEvent.class, new ResendMessagesStrategy());
	}
	
	/**
	 * Method that listens for the blockingQueue and handles events
	 */
	public void start()
	{
		while (true)
		{
			ApplicationEvent event = null;
			try
			{
				event = eventsBlockingQueue.take();
			}
			catch (final InterruptedException e)
			{
				continue;
			}
			eventsToStrategyMap.get(event.getClass()).execute(event);
		}
	}
	
	/**
	 * Class that provides common base for strategies used to handle events 
	 * 
	 * @author Wojciech Zieliñski
	 */
	private abstract class ServerStrategy
	{
		/**
		 * Method that is invoked in response to ApplicationEvent
		 * 
		 * @param event ApplicationEvent to be handled
		 */
		public abstract void execute(final ApplicationEvent event);
	}
	
	/**
	 * Strategy that handles LoginRequestEvent. it checks if the username of the user is already
	 * in use. if it's not user is accepted. in different case he is rejected.
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class LoginInStrategy extends ServerStrategy
	{
		/**
		 * Method that is invoked in response to LogInEvent.
		 * It asks the model whether given user name is valid, adds (or not)
		 * the user, and sends status update to all users.
		 * @param e LogInEvent to be handled
		 */
		@Override
		public void execute(final ApplicationEvent e)
		{
			if(e instanceof LogInEvent == false)
			{
				return;
			}
			final LogInEvent event = (LogInEvent) e;
			final ClientNetworkManager client = event.getClientNetworkManager();
			if(model.isNameAllowed(event.getUserName()))
			{
				final String username = event.getUserName();
				model.addUser(username);
				networkManager.broadcast(model.getChatState(UserStatus.CONTINUES_WORKING));
				clientToUserNameMap.put(client, username);
				client.setLoggedIn();
				client.send(model.getChatState(UserStatus.JUST_LOGGED_IN));
			}
			else 
			{
				networkManager.removeClient(client);
				client.send(model.getChatState(UserStatus.USER_NAME_REJECTED));
				client.close();
			}
		}
	}
	
	/**
	 * Strategy that handles MessageReceivedEvent
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class MessageStrategy extends ServerStrategy
	{
		/**
		 * Method that is invoked in response to MessageEvent. It checks
		 * whether user who sent this message is up to date with received messages,
		 * and then either accepts the message and resends status update to all,
		 * or rejects the message and resends status update only to the author.
		 * 
		 * @param e MessageEvent to be handled
		 */
		@Override
		public void execute(final ApplicationEvent e)
		{
			if(e instanceof MessageEvent == false)
			{
				return;
			}
			final MessageEvent event = (MessageEvent)e;
			final ClientNetworkManager client = event.getClientNetworkManager();
			if(client.getIsLoggedIn() == false)
			{
				return;
			}
			final Date previousMessageDate = event.getPreviousMessageDate();
			if(model.isValidDate(previousMessageDate))
			{
				final ChatMessage message = new ChatMessage(
						event.getMessageString(), 
						clientToUserNameMap.get(client), 
						new Date());
				model.addMessage(message);
				networkManager.broadcast(model.getChatState(ChatState.UserStatus.CONTINUES_WORKING));
			}
			else 
			{
				final ChatState state = model.getChatStateWithMessagesAfter(
						previousMessageDate, ChatState.UserStatus.MESSAGE_REJECTED);
				client.send(state);
			}
			
		}
		
	}
	
	/**
	 * Strategy that handles LogOutEvent
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class LogOutStrategy extends ServerStrategy
	{
		/**
		 * Method that is invoked in response to LogOutEvent. It disconnects
		 * the user from the server, resends update to him saying that log out succeeded
		 * and resends update to all without this user mentioned in the logged in users status.
		 */
		@Override
		public void execute(final ApplicationEvent e)
		{
			if(e instanceof LogOutEvent == false)
			{
				return;
			}
			final LogOutEvent event = (LogOutEvent) e;
			final ClientNetworkManager client = event.getClientNetworkManager();
			final String username = clientToUserNameMap.get(client);
			clientToUserNameMap.remove(client);
			model.removeUser(username);
			networkManager.removeClient(client);
			client.send(model.getChatState(ChatState.UserStatus.LOGGED_OUT));
			client.close();
			networkManager.broadcast(model.getChatState(ChatState.UserStatus.CONTINUES_WORKING));
		}
		
	}
	
	/**
	 * Strategy that handle ResendMessagesEvent
	 * 
	 * @author Wojciech Zieliñski
	 */
	private class ResendMessagesStrategy extends ServerStrategy
	{
		/**
		 * Method that is invoked in response to ResendMessagesEvent. It sends
		 * a status update to the author of this event with all the messages that
		 * he is missing (based on the date he gave in the ResendMessagesEvent)
		 */
		@Override
		public void execute(final ApplicationEvent e)
		{
			if(e instanceof StateRequestEvent == false)
			{
				return;
			}
			final StateRequestEvent event = (StateRequestEvent) e;
			final ClientNetworkManager client = event.getClientNetworkManager();
			if(client.getIsLoggedIn() == false)
			{
				return;
			}
			final ChatState state = model.getChatStateWithMessagesAfter(
					event.getLastMessageDate(), ChatState.UserStatus.CONTINUES_WORKING);
			client.send(state);
		}
	}
}
