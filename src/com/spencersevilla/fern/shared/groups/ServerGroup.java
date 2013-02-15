package com.spencersevilla.fern.groups;
import com.spencersevilla.fern.*;

import java.io.*;
import java.net.*;
import java.util.*;

public abstract class ServerGroup extends FERNGroup implements Runnable {
	public static final String REGISTER = new String("REGISTER");
	public static final String REMOVE = new String("REMOVE");
	public static final String REQUEST = new String("REQUEST");

	protected InetAddress addr;
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
		if (nameArgs.get(0).equals("create")) {

			if (nameArgs.size() < 2) {
				System.err.println("SG createGroupFromArgs error: no local port!");
				return null;
			} else {
				InetAddress addr = Service.generateAddress();
				int port = Integer.parseInt(nameArgs.get(1));
				return new ServerGroupServer(m, n, addr, port);
			}

		} else if (nameArgs.get(0).equals("join")) {
			if (nameArgs.size() < 3) {
				System.err.println("SG createGroupFromArgs error: invalid init string!");
				return null;
			} else {
				try {
					InetAddress addr = InetAddress.getByName(nameArgs.get(1));
					int port = Integer.parseInt(nameArgs.get(2));
					return new ServerGroupClient(m, n, addr, port);
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
	public abstract void registerObject(FERNObject o);
	public abstract void removeObject(FERNObject o);
	public abstract FERNObject resolveName(Request request);
}