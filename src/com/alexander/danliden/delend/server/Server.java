package com.alexander.danliden.delend.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server extends Thread {

	// Server port 39678

	
	/*
	 * ****************
	 * *NETWORK PAKETS*
	 * ****************
	 * 
	 * packetdelay - ping 
	 * 00 - Login 
	 * 01 - Disconnect 
	 * 02 - ping
	 * 03 - new player 
	 * 04 - player moving ID at [2] 
	 * 05 - player stats (health) ID at [2] 
	 * 06 - projectile creation ID at [5] 
	 * 07 - attackState ID at [1]
	 * 08 - teleport animation ID at [1] 
	 * 09 - phases ( animation) ID at [2] 
	 * 10 - Show Damage text ID at [2] 
	 * 11 - someone died (PVP) 
	 * 12 - transport animation(online)
	 * 13 - player mana
	 * ch - chat
	 * 666 - display ping > NOT FUNCTIONAL
	 * 
	 * -- - add player to arena instance > INACTIVE
	 * -. - removes player from arena instance > INACTIVE 
	 * .. - arena confirmed > INACTIVE 
	 * -n - arena invite > INACTIVE 
	 * 99 - checker 
	 * /m - chat function (message) > INACTIVE 
	 * -2 - serverChecker
	 * doyouexist - ServerChecker
	 */

	private DatagramSocket socket;
	private int port = 39678;
	private int MAX_ATTEMPTS = 5; // Max attempts before removing player ( Internet connections attempts )

	private List<ServerClient> clients = new ArrayList<ServerClient>();
	public CopyOnWriteArrayList<ServerClient> worldClients = new CopyOnWriteArrayList<ServerClient>();
	private ServerInstance privateArenaList = new ServerInstance(worldClients);
	private CopyOnWriteArrayList<ServerClient> arenalist = new CopyOnWriteArrayList<ServerClient>();

	private List<Integer> clientResponse = new ArrayList<Integer>();

	private Thread manageClients;
	private Thread commandThread;

	private int arenaPlayerThatGotInvitedID = -1;
	private boolean blockArenaMessages = false;
	private boolean canKick = false;

	/**************************************************************************
	 * ARENA FUNCIONS IS INACTIVE BECAUSE OF TRILLIONS OF BUGS AND A LAZY PROGRAMMER*
	 **************************************************************************/

	public Server() {

		try {
			socket = new DatagramSocket(port);
			console("Booting up");
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}

	public void run() {
		console("Server is up and running!\n");
		console("Server commands");
		console("_________________________");
		console("/check --> ping pong with connecnted clients (Client/Server response)");
		console("/connected --> View all connected player");
		console("/kick [User ID] --> kicks the selected player from the server");
		commands();
		manage();

		while (true) {
			receive();

		}
	}

	private void commands() {
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);

		commandThread = new Thread("Commanding Thread") {
			public void run() {
				while (true) {
					String text = scanner.nextLine();

					if (text.equals("/check")) {
						sendToAllWorldClients("99");
					}
					if (text.equals("/connected")) {
						System.out.println("Players online " + clients.size());
						System.out.println("#########################################");
						System.out.println("---------PLAYERS---------");
						for (int i = 0; i < clients.size(); i++) {
							System.out.println(clients.get(i).name + " (" + clients.get(i).getID() + ") " + "  IP: "
									+ clients.get(i).address);
						}
						System.out.println("#########################################");
						System.out.println("");
						System.out.println("Players in world list " + worldClients.size());
						System.out.println("#########################################");
						System.out.println("---------PLAYERS---------");
						for (int i = 0; i < worldClients.size(); i++) {
							System.out.println(worldClients.get(i).name + " (" + worldClients.get(i).getID() + ") "
									+ "  IP: " + worldClients.get(i).address);
						}
						System.out.println("#########################################");

					}
					if (text.startsWith("/kick")) {
						String client = text.substring(6).trim();
						int clientID = -1;

						try {
							canKick = true;
							clientID = Integer.parseInt(client);
						} catch (NumberFormatException e) {
							canKick = false;
							System.out.println("Command -> /kick [User ID]");
						}
						if (canKick) {
							for (int i = 0; i < clients.size(); i++) {
								if (clients.get(i).getID() == clientID) {
									disconnect(clientID, true);

								}
							}
						}

					}
				}
			}
		};
		commandThread.start();
	}

	private void manage() {
		manageClients = new Thread("Managing") {
			public void run() {
				while (true) {
					sendToAllClients("02");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					for (int i = 0; i < clients.size(); i++) {
						ServerClient c = clients.get(i);
						if (!clientResponse.contains(c.getID())) {
							if (c.attempt >= MAX_ATTEMPTS) {
								// ____ARENA STUFF______
								if (privateArenaList.group.contains(c)) {
									privateArenaList.group.remove(c);
								}
								if (!privateArenaList.group.isEmpty()) {
									String info = "-q";
									String playerLeft = "-l" + c.getID() + ","
											+ privateArenaList.getPlayerOne().getID();
									send(info.getBytes(), privateArenaList.getPlayerOne().address,
											privateArenaList.getPlayerOne().port);
									sendToAllClients(playerLeft);
									worldClients.add(privateArenaList.getPlayerOne());
								}

								// ________________________
								disconnect(c.getID(), false);

							} else {
								c.attempt++;
							}
						} else {
							clientResponse.remove(new Integer(c.getID()));
							c.attempt = 0;
						}
					}

				}
			}
		};
		manageClients.start();
	}

	private void receive() {
		byte[] data = new byte[1024];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		process(packet);
	}

	private void process(DatagramPacket packet) {
		String fulldata = new String(packet.getData()).trim();
		String messagetype = fulldata.substring(0, 2);
		String message = fulldata.substring(2);

		if (fulldata.equalsIgnoreCase("doyouexist")) {
			console("Client joining");
			String response = "yes";
			send(response.getBytes(), packet.getAddress(), packet.getPort());
		}

		if (messagetype.equalsIgnoreCase("-2")) {
			String response = "-2" + "," + clients.size();
			send(response.getBytes(), packet.getAddress(), packet.getPort());
			//System.out.println("Got search packet"); // debug
		}

		if (messagetype.equalsIgnoreCase("00")) {
			int id = UniqueIdentifier.getIdentifier();
			clients.add(new ServerClient(message.trim(), packet.getAddress(), packet.getPort(), id));
			worldClients.add(clients.get(clients.size() - 1));
			String clientID = "00" + id + "," + message.trim();
			send(clientID.getBytes(), packet.getAddress(), packet.getPort());
			String newClient = message.trim() + "(" + id + ") has connected" + "  IP: " + packet.getAddress() + ":"
					+ packet.getPort();
			console(newClient);

			// Dont double add
			ServerClient c = null;
			ServerClient c2 = null;
			for (int i = 0; i < clients.size(); i++) {
				c = clients.get(i);
				if (c.getID() != id) {
					send(("03" + id + "," + message.trim()).getBytes(), c.address, c.port);
				} else if (c.getID() == id) {
					if (clients.size() > 1) {
						for (int h = 0; h < clients.size(); h++) {
							c2 = clients.get(h);
							if (c2.getID() != id) {
								send(("03" + c2.getID() + "," + c2.name).getBytes(), c.address, c.port);
							}
						}
					}
				}

			}

		} else if (messagetype.equals("02")) {
			clientResponse.add(Integer.parseInt(message.trim()));
		} else if (messagetype.equals("04")) {
			String[] information = message.split(",");
			int id = Integer.parseInt(information[2].trim());
			boolean foundDestination = false;

			if (privateArenaList.isFull()) {
				for (int i = 0; i < privateArenaList.group.size(); i++) {
					if (privateArenaList.group.get(i).getID() == id) {
						sendToArenaClients(fulldata);
						foundDestination = true;
					}
				}
			}
			if (!foundDestination)
				sendToAllWorldClients(fulldata);

		} else if (messagetype.equals("05")) {
			String[] information = message.split(",");
			int id = Integer.parseInt(information[2].trim());
			boolean foundDestination = false;

			if (privateArenaList.isFull()) {
				for (int i = 0; i < privateArenaList.group.size(); i++) {
					if (privateArenaList.group.get(i).getID() == id) {
						sendToArenaClients(fulldata);
						foundDestination = true;
					}
				}
			}
			if (!foundDestination)
				sendToAllWorldClients(fulldata);
		} else if (messagetype.equals("06")) {
			String[] information = message.split(",");
			int id = Integer.parseInt(information[5].trim());
			boolean foundDestination = false;

			if (privateArenaList.isFull()) {
				for (int i = 0; i < privateArenaList.group.size(); i++) {
					if (privateArenaList.group.get(i).getID() == id) {
						sendToArenaClients(fulldata);
						foundDestination = true;
					}
				}
			}
			if (!foundDestination)
				sendToAllWorldClients(fulldata);
		} else if (messagetype.equals("07")) {
			String[] information = message.split(",");
			int id = Integer.parseInt(information[1].trim());
			boolean foundDestination = false;

			if (privateArenaList.isFull()) {
				for (int i = 0; i < privateArenaList.group.size(); i++) {
					if (privateArenaList.group.get(i).getID() == id) {
						sendToArenaClients(fulldata);
						foundDestination = true;
					}
				}
			}
			if (!foundDestination)
				sendToAllWorldClients(fulldata);
		} else if (messagetype.equals("08")) {
			String[] information = message.split(",");
			int id = Integer.parseInt(information[1].trim());
			boolean foundDestination = false;

			if (privateArenaList.isFull()) {
				for (int i = 0; i < privateArenaList.group.size(); i++) {
					if (privateArenaList.group.get(i).getID() == id) {
						sendToArenaClients(fulldata);
						foundDestination = true;
					}
				}
			}
			if (!foundDestination)
				sendToAllWorldClients(fulldata);
		} else if (messagetype.equals("09")) {
			String[] information = message.split(",");
			int id = Integer.parseInt(information[2].trim());
			boolean foundDestination = false;

			if (privateArenaList.isFull()) {
				for (int i = 0; i < privateArenaList.group.size(); i++) {
					if (privateArenaList.group.get(i).getID() == id) {
						sendToArenaClients(fulldata);
						foundDestination = true;
					}
				}
			}
			if (!foundDestination)
				sendToAllWorldClients(fulldata);
		} else if (messagetype.equals("10")) {
			String[] information = message.split(",");
			int id = Integer.parseInt(information[2].trim());
			boolean foundDestination = false;

			if (privateArenaList.isFull()) {
				for (int i = 0; i < privateArenaList.group.size(); i++) {
					if (privateArenaList.group.get(i).getID() == id) {
						sendToArenaClients(fulldata);
						foundDestination = true;
					}
				}
			}
		if (!foundDestination)
			sendToAllWorldClients(fulldata);
		}else if (messagetype.equals("11")) {
			sendToAllWorldClients(fulldata);
		}else if (messagetype.equals("12")) {
			sendToAllWorldClients(fulldata);
		}else if(messagetype.equals("13")){
			sendToAllWorldClients(fulldata);	
		}else if(messagetype.equals("ch")){
			sendToAllWorldClients(fulldata);
		}
		else if (fulldata.equals("666")) {
			send("666".trim().getBytes(), packet.getAddress(), packet.getPort());
		}else if (messagetype.equals("/m")) {
			sendToAllWorldClients(fulldata);
		}else if (messagetype.equals("--")) {
			String[] player = message.split(",");
			int playerID = -1;
			console("Got an arena request!");
			boolean full = false;
			boolean inUse = false;

			if (privateArenaList.group.size() > 0 && privateArenaList.group.size() < 2) {
				inUse = true;
			} else {
				inUse = false;
			}

			if (privateArenaList.isFull())
				full = true;
			else
				full = false;

			try {
				playerID = Integer.parseInt(player[0]);
			} catch (NumberFormatException e) {
				console("<ERROR> Could not add player: " + player[0] + " to list, NumberformatException was thrown...");
				return;
			}
			if (!full) {
				if (inUse) {

					if (playerID == arenaPlayerThatGotInvitedID) {
						for (int i = 0; i < clients.size(); i++) {
							if (clients.get(i).getID() == playerID) {
								ServerClient c = clients.get(i);

								if (!privateArenaList.isFull()) {
									if (!privateArenaList.containsPlayer(c)) {
										privateArenaList.addPlayer(c);
										console("Added Player " + c.name + ":" + c.getID()
												+ " to arena list (INVITED PLAYER)");
										String ok = "..";
										send(ok.getBytes(), c.address, c.port);

									}
								}

							}
						}
					}

				} else if (!inUse) {
					if (playerID != -1) {
						for (int i = 0; i < clients.size(); i++) {
							if (clients.get(i).getID() == playerID) {
								ServerClient c = clients.get(i);

								if (!privateArenaList.isFull()) {
									if (!privateArenaList.containsPlayer(c)) {
										privateArenaList.addPlayer(c);
										console("Added Player " + c.name + ":" + c.getID() + " to arena list");
										String ok = "..";
										send(ok.getBytes(), c.address, c.port);

									}
								}

							}
						}
					}
				}
			}
			if (!blockArenaMessages) {
				if (privateArenaList.isFull()) {
					// Start private match
					ServerClient arenaguy1 = null;
					ServerClient arenaguy2 = null;
					arenaguy1 = privateArenaList.group.get(0);
					arenaguy2 = privateArenaList.group.get(1);
					String startMessage = "-s" + arenaguy1.getID() + "," + arenaguy2.getID();
					send(startMessage.getBytes(), arenaguy1.address, arenaguy1.port);
					send(startMessage.getBytes(), arenaguy2.address, arenaguy2.port);
					String messageToWorld = "-h" + arenaguy1.getID() + "," + arenaguy2.getID();
					sendToAllWorldClients(messageToWorld);

					removeFromWorld(arenaguy1.getID());
					removeFromWorld(arenaguy2.getID());
					blockArenaMessages = true;

				}
			}
			if (!privateArenaList.isFull()) {
				// Tell player that you are still in queue
			}

		} else if (messagetype.equals("-.")) {
			String[] player = message.split(",");
			int playerID = -1;
			try {
				playerID = Integer.parseInt(player[0]);
			} catch (NumberFormatException e) {
				console("<ERROR> Could not remove player: " + player[0]
						+ " from list, NumberformatException was thrown...");
				return;
			}
			ServerClient c = null;
			for (int i = 0; i < clients.size(); i++) {
				if (clients.get(i).getID() == playerID)
					c = clients.get(i);
			}
			if (privateArenaList.group.contains(c)) {

				console(c.name + " was removed from arena group");
				privateArenaList.removePlayer(c);
				arenalist.remove(c);
				String leftByChoice = "-c" + c.getID();
				sendToAllClients(leftByChoice);

				if (!privateArenaList.group.isEmpty()) {
					String info = "-q";
					String playerLeft = "-l" + c.getID() + "," + privateArenaList.getPlayerOne().getID();
					send(info.getBytes(), privateArenaList.getPlayerOne().address,
							privateArenaList.getPlayerOne().port);
					sendToAllClients(playerLeft);
					worldClients.add(privateArenaList.getPlayerOne());
				}

			}
		} else if (messagetype.equals("-n")) {
			// **MUCHO IMPORTANTE** \\
			// message -- > <Player who should get the invite(ID)> and <Player who sent the invite(ID)>
			String[] information = message.split(",");

			int invitedPlayerID = Integer.parseInt(information[0]);
			int senderPlayerID = Integer.parseInt(information[1]);

			ServerClient c1 = null;
			ServerClient c2 = null;

			boolean foundPlayer = false;

			if (arenalist.isEmpty()) {

				// Find the invited player
				for (int i = 0; i < clients.size(); i++) {
					if (clients.get(i).getID() == invitedPlayerID) {
						c1 = clients.get(i);
						arenaPlayerThatGotInvitedID = c1.getID();
						arenalist.add(c1);
						foundPlayer = true;
					}
				}

				if (foundPlayer) {
					for (int i = 0; i < clients.size(); i++) {
						if (clients.get(i).getID() == senderPlayerID) {
							c2 = clients.get(i);
							arenalist.add(c2);
						}
					}

					// Send message to invited player (c1)
					String mess = "-n" + c2.name + "," + c2.getID();
					send(mess.getBytes(), c1.address, c1.port);

				}
			}

		}

	}
	// Send a packet (Your data(= information), The users address, the users port)
	private void send(final byte[] data, final InetAddress address, final int port) {
		DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendToArenaClients(String message) {
		for (int i = 0; i < privateArenaList.group.size(); i++) {
			ServerClient client = privateArenaList.group.get(i);
			send(message.getBytes(), client.address, client.port);

		}
	}

	private void sendToAllWorldClients(String message) {
		for (int i = 0; i < worldClients.size(); i++) {
			ServerClient client = worldClients.get(i);
			send(message.getBytes(), client.address, client.port);

		}
	}

	private void sendToAllClients(String message) {
		for (int i = 0; i < clients.size(); i++) {
			ServerClient client = clients.get(i);
			send(message.getBytes(), client.address, client.port);
		}
	}

	private void disconnect(int id, boolean status) {
		ServerClient c = null;
		boolean existed = false;
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getID() == id) {
				c = clients.get(i);
				send("-1".getBytes(), c.address, c.port);
				clients.remove(i);
				existed = true;
				break;
			}
		}

		if (!existed)
			return;

		if (status) {
			console(c.name + "(" + c.getID() + ") has disconnected.  IP: " + c.address);
			sendToAllWorldClients("01" + "true" + "," + c.getID());
		}
		if (!status) {
			console(c.name + "(" + c.getID() + ") timed out.  IP: " + c.address);
			sendToAllWorldClients("01" + "false" + "," + c.getID());
		}

	}

	private void removeFromWorld(int id) {
		ServerClient c = null;
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getID() == id) {
				c = clients.get(i);
				if (worldClients.contains(c)) {
					worldClients.remove(c);
					console("Removed player " + c.name + " from world list");

				}
			}

		}

	}

	private void console(String message) {
		System.out.println("SERVER: " + message);
	}

}
