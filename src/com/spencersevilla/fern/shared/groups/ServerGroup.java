package com.spencersevilla.fern.groups;
import com.spencersevilla.fern.*;

import java.io.*;
import java.net.*;
import java.util.*;

public abstract class ServerGroup extends FERNGroup implements Runnable {
	public static final String REGISTER = new String("REGISTER");
	public static final String REMOVE = new String("REMOVE");
	public static final String REQUEST = new String("REQUEST");

	// protected InetAddress addr;
	protected int port;
	protected Thread thread;
	public static int id = 2;

	public final int getId() {
		return ServerGroup.id;
	}

	public ServerGroup(FERNManager m, Name n, InetAddress a, int p) {
		super(m, n);
		addr = a;
		port = p;
	}

	public static ServerGroup createGroupFromArgs(FERNManager m, Name n, ArrayList<String> nameArgs) {
		InetAddress myaddr = null;
		if (nameArgs.get(0).equals("create")) {

			if (nameArgs.size() < 2) {
				System.err.println("SG createGroupFromArgs error: no local port!");
				return null;
			} else {
				int port = Integer.parseInt(nameArgs.get(1));

				// if we supplied a self-address, use it. if not, autogen!
				if (nameArgs.size() == 3) {
					try {
						myaddr = InetAddress.getByName(nameArgs.get(2));
					} catch (UnknownHostException e) {
						e.printStackTrace();
						return null;
					}
				} else {
					myaddr = Service.generateAddress();
				}

				return new ServerGroupServer(m, n, myaddr, port);
			}

		} else if (nameArgs.get(0).equals("join")) {
			if (nameArgs.size() < 3) {
				System.err.println("SG createGroupFromArgs error: invalid init string!");
				return null;
			} else {
				try {
					InetAddress addr = InetAddress.getByName(nameArgs.get(1));
					int port = Integer.parseInt(nameArgs.get(2));

					if (nameArgs.size() == 4) {
						myaddr = InetAddress.getByName(nameArgs.get(3));
					} else {
						myaddr = Service.generateAddress();
					}

					return new ServerGroupClient(m, n, addr, port, myaddr);
				} catch (UnknownHostException e) {
					e.printStackTrace();
					return null;
				}
			}
		} else {
			System.err.println("SG createGroupFromArgs error: invalid command " + nameArgs.get(0));
			return null;
		}	
	}

	// cleanup method
	public final void exit() {
		System.out.println("SG " + name + ": exiting group");
	}

	// push core functionality into implementation-classes
	public abstract Response parseMessage(Message message);
	public abstract FERNObject getNextHop(Message message);
	
	public abstract void registerObject(FERNObject o);
	public abstract void removeObject(FERNObject o);
	// public abstract FERNObject resolveName(Request request);
}