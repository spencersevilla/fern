package com.spencersevilla.mdns;

import java.io.*;
import java.net.*;
import java.util.*;

// implement DNSGroup, Runnable later on
public class ServerGroup extends DNSGroup implements Runnable {
	private InetAddress addr;
	private int port;
	private boolean serving;
	private Thread thread;
	private ArrayList<Service> services;
	public static final int id = 2;

	ServerGroup(MultiDNS m, String n, InetAddress a, int p, boolean s) {
		super(m, n);
		addr = a;
		port = p;
		serving = s;
		services = new ArrayList<Service>();
	}

	ServerGroup(MultiDNS m, ArrayList<String> nameArgs) {
		super(m, nameArgs.get(0));
		services = new ArrayList<Service>();

		if (nameArgs.get(1).equals("create")) {
			if (nameArgs.size() < 3) {
				System.err.println("SG " + fullName + " init error: no local port!");
			} else {
				addr = null;
				port = Integer.parseInt(nameArgs.get(2));
				serving = true;
			}

		} else if (nameArgs.get(1).equals("join")) {
			if (nameArgs.size() < 4) {
				System.err.println("SG " + fullName + " init error: invalid init string!");
			} else {
				try {
					addr = InetAddress.getByName(nameArgs.get(2));
					port = Integer.parseInt(nameArgs.get(3));
					serving = false;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("SG " + fullName + " init error: invalid command " + nameArgs.get(1));
		}
	}

	// DNSGroup methods =========================================================
	public void start() {
		if (!serving) {
			System.out.println("SG " + fullName + ": starting-up, but not serving!");
			return;
		}

		// WE are the server so start it up! (on its own thread)
		thread = new Thread(this);
		thread.start();
	}

	public void run() {
		byte buf[];
        DatagramPacket pack;

        try {
			DatagramSocket socket = new DatagramSocket(port);
			System.out.println("SG " + fullName + ": serving on port " + port);

			while (true) {
				buf = new byte[1024];
				pack = new DatagramPacket(buf, buf.length);
				
		        socket.receive(pack);
		        String data = new String(pack.getData());
				String[] args = data.split(":");
				System.out.println("SG " + fullName + ": received " + data);

				if(args[0].equals("SRV_REG")) {
					String sname = args[1];
					String addr = args[2];
					Service s = new Service(sname, 0, mdns);
					s.addr = addr;
					serviceRegistered(s);
				} else if (args[0].equals("SRV_DEL")) {
					String sname = args[1];
					for (Service s : services) {
						if (s.name.equals(sname)) {
							serviceRemoved(s);
						}
					}
				} else {
					System.err.println("SG " + fullName + " error: invalid command");
				}
			}
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}

	public void stop() {
		System.out.println("SG " + fullName + ": stopped.");
		if (!serving) {
			for (Service s : mdns.serviceList) {
				serviceRemoved(s);
			}
			return;
		}

		// don't really know anything else we have to do here for cleanup...?
		return;
	}
	public void serviceRegistered(Service s) {
		if (serving) {
			services.add(s);
			return;
		}

		try {
			DatagramSocket sock = new DatagramSocket();

			String send = new String("SRV_REG:" + s.name + ":" + s.addr);
			byte[] data = send.getBytes();
			DatagramPacket pack = new DatagramPacket(data, data.length, addr, port);
			sock.send(pack);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return;
	}

	public void serviceRemoved(Service s) {
		if (serving) {
			services.remove(s);
			return;
		}

		try {
			DatagramSocket sock = new DatagramSocket();

			String send = new String("SRV_DEL:" + s.name);
			byte[] data = send.getBytes();
			DatagramPacket pack = new DatagramPacket(data, data.length, addr, port);
			sock.send(pack);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return;

	}

	public InetAddress resolveService(String name) {
		return resolveService(name, 0);
	}

	public InetAddress resolveService(String name, int minScore) {
		if (!serving) {
			return mdns.forwardRequest(name, minScore, addr, 53);
		}

		System.out.println("SG " + fullName + ": resolving " + name);
		String servicename = getServiceName(name);

		InetAddress a = null;
		for (Service s : services) {
			if (s.name.equals(servicename)) {
				try {
					a = InetAddress.getByName(s.addr);
				} catch (UnknownHostException e) {
					// do nothing?
					continue;
				}
				break;
			}
		}

		// Service not found
		if (a == null) {
			System.out.println("SG " + fullName + ": no result for request " + name);
			return null;
		}

		// Now all that's left to do is determine if this was the end-result
		// Or if it's a redirect for another server!
		String[] servicegroups = name.split("\\.");
		if (servicename.equals(servicegroups[0])) {
			System.out.println("SG " + fullName + ": returning " + a + " for request " + name);
			return a;
		} else {
			System.out.println("SG " + fullName + ": forwarding request for " + name + " to " + a);
			int p = 53;
			return mdns.forwardRequest(name, minScore, a, p);
		}
	}
}