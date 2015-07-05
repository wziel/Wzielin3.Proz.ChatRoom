package wzielin3.proz.client;

import java.util.concurrent.LinkedBlockingQueue;
import wzielin3.proz.client.network.NetworkManager;
import wzielin3.proz.client.view.ClientMainView;
import wzielin3.proz.server.events.ApplicationEvent;

/**
 * Class responsible for handling Client-side of chat.
 * It creates View for client and NetworkManager to communicate with server.
 * It also creates BlockingQueue to which view sends events and NetworkManager reads them from it.
 * 
 * @author Wojciech Zieliñski
 */
public class Client
{
	public static void main(final String[] args)
	{
		final LinkedBlockingQueue<ApplicationEvent> blockingQueue = new LinkedBlockingQueue<>();
		final ClientMainView view = new ClientMainView(blockingQueue);
		new NetworkManager(view, blockingQueue).start();	
	}

}
