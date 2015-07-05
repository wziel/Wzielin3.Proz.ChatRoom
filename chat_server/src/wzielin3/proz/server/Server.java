package wzielin3.proz.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import wzielin3.proz.server.controller.Controller;
import wzielin3.proz.server.events.ApplicationEvent;
import wzielin3.proz.server.model.ServerModel;
import wzielin3.proz.server.network.ServerNetworkManager;
import wzielin3.proz.server.view.View;

/**
 * Class responsible for creating all objects that the server composes of.
 * 
 * @author Wojciech Zieliñski
 */
public class Server
{
	public static void main(final String[] args)
	{
		final BlockingQueue<ApplicationEvent> blockingQueue =
				new LinkedBlockingQueue<ApplicationEvent>();
		final ServerNetworkManager networkManager = new View(blockingQueue).getServerNetworkManagerFromConsole();
		final ServerModel model = new ServerModel();
		final Controller controller = new Controller(blockingQueue, networkManager, model);
		networkManager.start();
		System.out.println("Server Running");
		controller.start();
		System.out.println("Server stopped");
	}
}
