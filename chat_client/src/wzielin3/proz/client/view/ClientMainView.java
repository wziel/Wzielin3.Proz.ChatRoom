package wzielin3.proz.client.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import wzielin3.proz.server.ChatMessage;
import wzielin3.proz.server.ChatState;
import wzielin3.proz.server.ChatState.UserStatus;
import wzielin3.proz.server.events.ApplicationEvent;
import wzielin3.proz.server.events.LogInEvent;
import wzielin3.proz.server.events.LogOutEvent;
import wzielin3.proz.server.events.MessageEvent;

/**
 * Class responsible for the client view.
 * It creates and displays JFrame. It generates ApplicationEvents which
 * then are sent to blocking queue.
 * 
 * @author Wojciech Zieliñski
 */
public class ClientMainView
{
	/**JFrame on which all panels are displayed*/
	private final JFrame mainFrame;
	/**JTextField for server address input*/
	private final JTextField serverIPTextField;
	/**JTextField for server port input*/
	private final JTextField serverPortTextField;
	/**JTextField for userName input*/
	private final JTextField userNameTextField;
	/**JButton that clicked fires LoginRequestEvent and sends it to the blockingQueue*/
	private final JButton loginButton;
	/**JButton that clicked fires LogOutEvent and sends it to the blockingQueue*/
	private final JButton logoutButton;
	/**ClientMainView that has created this JPanel*/
	private final JTextArea chatTextArea;
	/**JScrollBar vertical on the chatTextArea*/
	private final JScrollBar chatVeritacalScrollBar;
	/** JTextArea on which user can write his message that he wants to send*/
	private final JTextArea messageTextArea;
	/** JButton that clicked fires MessageSentEvent with text inserted on messageTextArea */
	private final JButton sendMessageButton;
	/**JPanel on which currently logged in users are displayed*/
	private final JPanel userDisplayPanel;
	/**Set of currently logged in user's names*/
	private final Set<JLabel> userNamesLabels;
	/**BlockingQueue to which events are sent*/
	private final BlockingQueue<ApplicationEvent> eventsBlockingQueue;
	/**Mapping of current user state to string that should be displayed*/
	private final HashMap<ChatState.UserStatus, String> stateToMessageMap;
	/**Maximum length of a message that can be sent*/
	private final static int MESSAGE_MAX_LENGTH = 100;
	
	/**
	 * Basic constructor that takes as an argument BlockingQueue to which events will be sent.
	 * 
	 * @param eventsBlockingQueue - blocking queue to which events from this view will be sent.
	 */
	public ClientMainView(final BlockingQueue<ApplicationEvent> eventsBlockingQueue)
	{
		this.eventsBlockingQueue = eventsBlockingQueue;
		
		mainFrame = new JFrame("Client");
		mainFrame.setBounds(50, 50, 800, 500);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//login panel
		JPanel loginPanel = new JPanel();
		loginPanel.setLayout(new GridLayout(1, 8));
		serverIPTextField = new JTextField("localhost");
		loginPanel.add(new JLabel("Server IP:"));
		loginPanel.add(serverIPTextField);
		serverIPTextField.requestFocus();
		serverPortTextField = new JTextField("5000");
		loginPanel.add(new JLabel("Port:"));
		loginPanel.add(serverPortTextField);
		userNameTextField = new JTextField("name");
		loginPanel.add(new JLabel("UserName"));
		loginPanel.add(userNameTextField);
		loginButton = new JButton("Login");
		loginButton.addActionListener(new LogInButtonListener());
		loginPanel.add(loginButton);
		logoutButton = new JButton("Logout");
		logoutButton.addActionListener(new LogOutButtonActionListener());
		logoutButton.setEnabled(false);
		loginPanel.add(logoutButton);
		mainFrame.add(loginPanel, BorderLayout.NORTH);
		//chat panel
		JPanel chatPanel = new JPanel();
		chatPanel.setLayout(new BorderLayout());
		chatTextArea = new JTextArea("Welcome! Log in to connect to server.\n");
		chatTextArea.setEditable(false);
		chatTextArea.setLineWrap(true);
		final JPanel centerPanel = new JPanel(new GridLayout(1,1));
		final JScrollPane scrollPane = new JScrollPane(chatTextArea);
		chatVeritacalScrollBar = scrollPane.getVerticalScrollBar();
		centerPanel.add(scrollPane);
		chatPanel.add(centerPanel, BorderLayout.CENTER);
		sendMessageButton = new JButton("Send");
		sendMessageButton.addActionListener(new SendButtonActionListener());	
		sendMessageButton.setEnabled(false);
		messageTextArea = new JTextArea();
		messageTextArea.setEditable(true);
		messageTextArea.setLineWrap(true);
		messageTextArea.setEnabled(false);
		messageTextArea.addKeyListener(new MessageTextAreaKeyListener());
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(new JScrollPane(messageTextArea), BorderLayout.CENTER);
		southPanel.add(sendMessageButton, BorderLayout.EAST);
		chatPanel.add(southPanel, BorderLayout.SOUTH);
		mainFrame.add(chatPanel, BorderLayout.CENTER);
		//user display panel
		userDisplayPanel = new JPanel();
		userDisplayPanel.setLayout(new BoxLayout(userDisplayPanel, BoxLayout.Y_AXIS));
		userDisplayPanel.add(new JLabel("Users:"));
		userNamesLabels = new HashSet<JLabel>();
		mainFrame.add(userDisplayPanel, BorderLayout.EAST);
		
		mainFrame.setVisible(true);
		stateToMessageMap = new HashMap<>();
		stateToMessageMap.put(UserStatus.JUST_LOGGED_IN, "Connection succeeded!");
		stateToMessageMap.put(UserStatus.LOGGED_OUT, "Logging out succeeded.");
		stateToMessageMap.put(UserStatus.MESSAGE_REJECTED, "Your message wasn't delivered. Try again.");
		stateToMessageMap.put(UserStatus.REJECTED, "You have been removed from the server.");
		stateToMessageMap.put(UserStatus.USER_NAME_REJECTED, "Username not available. Try another one.");
	}
	
	/**
	 * Method that takes a ChatState as a parameter and based on the state
	 * defined in this objects sets the view. This method is thread safe.
	 * 
	 * @param state ChatState object that tells the view how it should look
	 */
	public void setChatState(final ChatState state)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if(stateToMessageMap.containsKey(state.getUserStatus()))
				{
					print("\n" + stateToMessageMap.get(state.getUserStatus()) + "\n");
				}
				setConnected(state.isLoggedIn());
				clearUserNames();
				if(state.isLoggedIn())
				{
					addUserNames(state.getLoggedInUserNames());
					final List<ChatMessage> messages = state.getChatMessages();
					Collections.sort(messages);
					for (final ChatMessage message : messages)
					{
						addMessage(message);
					}
				}
				mainFrame.revalidate();
		  		mainFrame.repaint();
			}
		});
		
	}
	
	/**
	 * Method invoked by network manager when view should be displayed for 
	 * chat that has lost connection with the server for unknown reasons.
	 * This method is thread safe.
	 */
	public void setDisconnected()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				setConnected(false);
				clearUserNames();
				print("\nConnection has been lost. Try reconnecting.\n");
				mainFrame.revalidate();
		  		mainFrame.repaint();
			}
		});
	}
	
	/**
	 * Method that sets the view depending on the connection status.
	 * 
	 * @param isConnected true if view should be displayed for connected user.
	 */
	private void setConnected(final boolean isConnected)
	{
		serverIPTextField.setEnabled(!isConnected);
		serverPortTextField.setEnabled(!isConnected);
		userNameTextField.setEnabled(!isConnected);
		loginButton.setEnabled(!isConnected);
		logoutButton.setEnabled(isConnected);	
		sendMessageButton.setEnabled(isConnected);
		messageTextArea.setEnabled(isConnected);
	}
	
	/**
	 * Method that clears the displayed user names on the userDisplayPanel.
	 */
	private void clearUserNames()
	{
		for (final JLabel jLabel : userNamesLabels)
		{
			userDisplayPanel.remove(jLabel);
		}
		userNamesLabels.clear();
	}
	
	/**
	 * method that adds user names to userDisplayPanel
	 * 
	 * @param userNames - set string names to be added
	 */
	private void addUserNames(final Set<String> userNames)
	{
		for (final String name : userNames)
		{
			final JLabel label = new JLabel(name);
			userNamesLabels.add(label);
			userDisplayPanel.add(label);
		}
	}
	
	/**
	 * Method that adds newly received message to the window
	 * 
	 * @param chatMessage - message to be added to view
	 */
	private void addMessage(final ChatMessage chatMessage)
	{
		final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM HH:mm:ss", Locale.US);
		final Date date = chatMessage.getSentDate();
		print("\n" + dateFormat.format(date) + ", " + chatMessage.getAuthor() + ":");
		print("\n" + chatMessage.getContent() + "\n");
	}
	
	/**
	 * Method that prints String on the chatTextArea.
	 * 
	 * @param string String to printed.
	 */
	private void print(final String string)
	{
		chatTextArea.append(string);
		mainFrame.revalidate();
  		mainFrame.repaint();
		chatVeritacalScrollBar.setValue(chatVeritacalScrollBar.getMaximum());
	}
	

	/**
	 * Method that sends messageEvent to the blocking queue with string 
	 * that currently has been inserted into sendMessageTextArea, and sets
	 * the sendMessageTextArea content to an empty string .
	 */
	private void sendMessage()
	{
		final String message = messageTextArea.getText(); 
		if(message.length() == 0)
		{
			return;
		}
		messageTextArea.setText("");
		try
		{
			eventsBlockingQueue.put(new MessageEvent(message));
		}
		catch (final InterruptedException e1)
		{
		}
	}
	
	/**
	 * Class that is responsible for listening to the keys typed on the
	 * message input TextArea. It doesn't allow to long messages and 
	 * on enter pressed it sends a message. It also doesn't allow
	 * white spaces other than SPACE, and doesn't allow two SPACEs near each other.
	 * 
	 * @author Wojciech Zieliñski
	 */
	private final class MessageTextAreaKeyListener extends KeyAdapter
	{
		@Override
		public void keyPressed(final KeyEvent e)
		{
			final char key = e.getKeyChar();
			//allow only spaces as white spaces
			if(Character.isWhitespace(key) && key != KeyEvent.VK_SPACE)
			{
				e.consume();
				return;
			}
		}
		
		@Override
		public void keyTyped(final KeyEvent e)
		{
			final String message = messageTextArea.getText();
			final char key = e.getKeyChar();
			//enter means sending 
			if(e.getKeyChar() == KeyEvent.VK_ENTER)
			{
				e.consume();
				sendMessage();
				return;
			}
			//text too long
			if(message.length() >= MESSAGE_MAX_LENGTH)
			{
				e.consume();
				messageTextArea.setText(message.substring(0, MESSAGE_MAX_LENGTH));
				return;
			}
			//don't allow space as the first character
			if(message.length() == 0 && Character.isWhitespace(key))
			{
				e.consume();
				return;
			}
			//don't allow two spaces near each other
			if(key == KeyEvent.VK_SPACE && Character.isWhitespace(message.charAt(message.length() - 1)))
			{
				e.consume();
				return;
			}
		}
	}

	/**
	 * ActionListener that responds to log in button click
	 * 
	 * @author Wojciech Zieliñski
	 */
	private final class LogInButtonListener implements ActionListener
	{
		/**
		 * Method that creates LogInEvent and sends it to BlockingQueue
		 */
		@Override
		public void actionPerformed(final ActionEvent arg0)
		{
			final String ipString = serverIPTextField.getText();
			final String userNameString = userNameTextField.getText();
			final String port = serverPortTextField.getText();
			setConnected(true);
			messageTextArea.setEnabled(false);
			sendMessageButton.setEnabled(false);
			logoutButton.setEnabled(false);
			print("\nWaiting for answer from server...\n");
			try
			{
				eventsBlockingQueue.put(
						new LogInEvent(userNameString, ipString, port));
			}
			catch (final InterruptedException e)
			{
				setConnected(false);
			}
		}
	}
	
	/**
	 * ActionListener that responds to log out button click.
	 * 
	 * @author Wojciech Zieliñski
	 */
	private final class LogOutButtonActionListener implements ActionListener
	{
		/**
		 * Method that creates LogOutEvent and sends it to BlockingQueue
		 */
		@Override
		public void actionPerformed(final ActionEvent arg0)
		{
			try
			{
				eventsBlockingQueue.put(new LogOutEvent());
			}
			catch (final InterruptedException e)
			{
			}
		}
	}
	
	/**
	 * ActionListener that reacts to the message sending button clicked.
	 * 
	 * @author Wojciech Zieliñœki
	 */
	private final class SendButtonActionListener implements ActionListener
	{
		@Override
		public void actionPerformed(final ActionEvent e)
		{
			sendMessage();
		}
	}
}
