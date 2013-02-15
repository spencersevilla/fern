package com.spencersevilla.fern.groups;
import com.spencersevilla.fern.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerGroupClient extends ServerGroup implements Runnable {
	private ExternalGroup server;

	public ServerGroupClient(FERNManager m, Name n, InetAddress a, int p) {
			super(m, n, a, p);
	}

	public void start() {
		System.out.println("SGClient " + name + ": starting-up");
		server = new ExternalGroup(name, addr, port);
		return;
	}

	public void stop() {
		return;
	}

	public void run() {
		System.out.println("SGClient called run!");
		return;
	}

	public void registerObject(FERNObject object) {
		try {
			Socket client = new Socket(server.addr, server.port);

			// ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
			ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());


			oos.writeObject(REGISTER);
			oos.writeObject(object);
			// Message response = (Message) ois.readObject();
			// DataObject object = parseResponse(response, contentName);

			// ois.close();
			oos.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return;
	}

	public void removeObject(FERNObject object) {
		try {
			Socket client = new Socket(server.addr, server.port);

			// ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
			ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());

			oos.writeObject(REMOVE);
			oos.writeObject(object);
			// Message response = (Message) ois.readObject();
			// DataObject object = parseResponse(response, contentName);

			// ois.close();
			oos.close();
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return;
	}

	public FERNObject resolveName(Request request) {
		return server;
	}
}