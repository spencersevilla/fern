package com.spencersevilla.fern.groups;
import com.spencersevilla.fern.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerGroupClient extends ServerGroup implements Runnable {
	private FERNObject server;
	// client address (addr) IS NOT server address (saddr)!!!
	private InetAddress saddr;

	public ServerGroupClient(FERNManager m, Name n, InetAddress a, int p, InetAddress myaddr) {
		super(m, n, ServerGroupClient.myAddr(myaddr), p);
		saddr = a;
	}

	private static InetAddress myAddr(InetAddress addr) {
		// WHAT IS MY CLIENT-ADDRESS?!?!?!?!
		if (addr == null) {
			System.out.println("SGC WARNING: GENERATING ADDRESS!");
			addr = Service.generateAddress();
		}
		return addr;
	}

	public void start() {
		System.out.println("SGC " + name + ": starting-up");
		server = new FERNObject(name);
		Record r = new Record(name, Type.A, DClass.IN, 0, saddr.getAddress());
		server.addRecord(r);
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
			Socket client = new Socket(saddr, port);

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
			Socket client = new Socket(saddr, port);

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