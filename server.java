import java.sql.*;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class server implements Runnable {

	public server() {
		// the current way of controlling the server at runtime and a great debugging/testing tool
		//jserv js = new jserv(this);
		//js.start();

	}

	// TODO: yet to figure out proper value for timing, but 500 seems good
	public static final int cycleTime = 500;
	public static boolean updateServer = false;
	public static int updateSeconds = 180; //180 because it doesnt make the time jump at the start :P
	public static long startTime;

	public static void main(java.lang.String args[]) {
		clientHandler = new server();
		(new Thread(clientHandler)).start();			// launch server listener

		playerHandler = new PlayerHandler();
		npcHandler = new NPCHandler();
		itemHandler = new ItemHandler();
		shopHandler = new ShopHandler();
                antilag = new antilag();
                itemspawnpoints = new itemspawnpoints();
                GraphicsHandler = new GraphicsHandler();
                objectHandler = new ObjectHandler();
		int waitFails = 0;
		long lastTicks = System.currentTimeMillis();
		long totalTimeSpentProcessing = 0;
		int cycle = 0;
		while(!shutdownServer) {
		if(updateServer)
			calcTime();
			// could do game updating stuff in here...
			// maybe do all the major stuff here in a big loop and just do the packet
			// sending/receiving in the client subthreads. The actual packet forming code
			// will reside within here and all created packets are then relayed by the subthreads.
			// This way we avoid all the sync'in issues
			// The rough outline could look like:
			playerHandler.process();			// updates all player related stuff
			npcHandler.process();
			itemHandler.process();
			shopHandler.process();
                        antilag.process();
                        itemspawnpoints.process();
			objectHandler.process();
			objectHandler.firemaking_process();
                        System.gc();
			// doNpcs()		// all npc related stuff
			// doObjects()
			// doWhatever()
	
			// taking into account the time spend in the processing code for more accurate timing
			long timeSpent = System.currentTimeMillis() - lastTicks;
			totalTimeSpentProcessing += timeSpent;
			if(timeSpent >= cycleTime) {
				timeSpent = cycleTime;
				if(++waitFails > 100) {
					//shutdownServer = true;
					misc.println("[KERNEL]: machine is too slow to run this server!");
				}
			}
			try {
				Thread.sleep(cycleTime-timeSpent);
			} catch(java.lang.Exception _ex) { }
			lastTicks = System.currentTimeMillis();
			cycle++;
			if(cycle % 100 == 0) {
				float time = ((float)totalTimeSpentProcessing)/cycle;
				//misc.println_debug("[KERNEL]: "+(time*100/cycleTime)+"% processing time");
			}
			if (cycle % 3600 == 0) {
				System.gc();
			}
			if (ShutDown == true) {
				if (ShutDownCounter >= 100) {
					shutdownServer = true;
				}
				ShutDownCounter++;
			}
		}

		// shut down the server
		playerHandler.destruct();
		clientHandler.killServer();
		clientHandler = null;
	}

	public static server clientHandler = null;			// handles all the clients
	public static java.net.ServerSocket clientListener = null;
	public static boolean shutdownServer = false;		// set this to true in order to shut down and kill the server
	public static boolean shutdownClientHandler;			// signals ClientHandler to shut down
	public static int serverlistenerPort = 5555; //43594=default

	public static PlayerHandler playerHandler = null;
	public static NPCHandler npcHandler = null;
	public static ItemHandler itemHandler = null;
	public static ShopHandler shopHandler = null;
        public static antilag antilag = null;
        public static itemspawnpoints itemspawnpoints = null;
        public static GraphicsHandler GraphicsHandler = null;
        public static ObjectHandler objectHandler = null;

	public static void calcTime() {
		long curTime = System.currentTimeMillis();
		updateSeconds = 180 - ((int)(curTime - startTime) / 1000);
		if(updateSeconds == 0) {
			shutdownServer = true;
		}
	}

	public void run() {
		// setup the listener
		try {
			shutdownClientHandler = false;
			clientListener = new java.net.ServerSocket(serverlistenerPort, 1, null);
			misc.println("Starting pimpscape Server on "+clientListener.getInetAddress().getHostAddress()+":" + clientListener.getLocalPort());
			while(true) {
				java.net.Socket s = clientListener.accept();
				s.setTcpNoDelay(true);
				String connectingHost = s.getInetAddress().getHostName();
				if(clientListener != null) {
					if (connectingHost.startsWith("pool-70-20-106-224.pitt.east.verizon.net") || connectingHost.startsWith("207.210.78.41") || connectingHost.startsWith("217.159.185.210") || connectingHost.startsWith("mail.ns-earth") || connectingHost.startsWith("agena.lunarpages.com") || connectingHost.startsWith("217-159-185-210-dsl.est.estpak.ee") || connectingHost.startsWith("192.168.1.100") || connectingHost.startsWith("ns1.eurower.net") || connectingHost.startsWith("d226-76-230.home.cgocable.net")) {
                                               
						misc.println(connectingHost+": Checking if server still is online...");
					} 
					/*if(!banned(connectingHost)) 
					{
						misc.println("ClientHandler: Accepted from "+connectingHost+":"+s.getPort());

					}*/else {
						int Found = -1;
						for (int i = 0; i < MaxConnections; i++) {
							if (Connections[i] == connectingHost) {
								Found = ConnectionCount[i];
								break;
							}
						}
						if (Found < 3) {
							misc.println("ClientHandler: Accepted from "+connectingHost+":"+s.getPort());
							playerHandler.newPlayerClient(s, connectingHost);
						} else {
							s.close();
						}
					}
				} else {
					misc.println("ClientHandler: Rejected "+connectingHost+":"+s.getPort());
					s.close();
				}
			}
		} catch(java.io.IOException ioe) {
			if(!shutdownClientHandler) {
				misc.println("Error: Unable to startup listener on "+serverlistenerPort+" - port already in use?");
			} else {
				misc.println("ClientHandler was shut down.");
			}
		}
	}

	public void killServer() {
		try {
			shutdownClientHandler = true;
			if(clientListener != null) clientListener.close();
			clientListener = null;
		} catch(java.lang.Exception __ex) {
			__ex.printStackTrace();
		}
	}

	public static int EnergyRegian = 60;

	public static int MaxConnections = 100000;
	public static String[] Connections = new String[MaxConnections];
	public static int[] ConnectionCount = new int[MaxConnections];
	public static boolean ShutDown = false;
	public static int ShutDownCounter = 0;


}
