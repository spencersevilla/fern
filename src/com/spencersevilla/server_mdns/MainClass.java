package com.spencersevilla.server_mdns;
import com.spencersevilla.mdns.*;

import org.apache.commons.daemon.*;

import java.io.*;
import java.util.*;
import java.net.InetAddress;

public class MainClass implements Daemon, DaemonUserSignal {
	protected MultiDNS mdns;
	protected SwingGui gui;

	private String conf;

	public static void main(String[] args) throws Exception {

		if (args.length > 1) {
			System.out.println("usage: MainClass [conf_file]");
			System.exit(0);
		}

		MainClass m = new MainClass();
		m.mdns = new MultiDNS();
		m.gui = new SwingGui(m.mdns);

		if (m.mdns == null) {
			System.out.println("error: could not initialize mdns!");
			System.exit(0);
		}

		if (args.length > 0) {
			m.conf = args[0];
		} else {
			m.conf = "config/mdns.conf";
		}

		m.start();

		// TESTING HERE!!!
		// try {
		// 	InetAddress dns_server = InetAddress.getByName("13.1.136.66");
		// 	InetAddress addr = m.mdns.forwardRequest("www.google.com.", 0, dns_server, 53);
		// 	System.out.println("request returned: " + addr);
		// } catch (Exception e) {
		// 	e.printStackTrace();
		// }
	}

	public MainClass() throws Exception {
	}
	
	// Java Daemon interface here! ============================================

	public void init(DaemonContext context) throws DaemonInitException, Exception {
		mdns = new MultiDNS();

		if (mdns == null) {
			throw new DaemonInitException("error: could not initialize mdns!");
		}

		String[] args = context.getArguments();

		if (args.length > 0) {
			// they supplied a custom name/path so use that instead
			conf = args[0];
		} else {
			// default: just uses this filename with respect to $CLASSPATH
			conf = "config/mdns.conf";
		}

		System.out.println("daemon: initialized.");
	}

	public void start() throws Exception {
		mdns.start();
		readConf(conf);
		System.out.println("daemon: started.");
	}

	public void stop() throws Exception {
		mdns.stop();
		System.out.println("daemon: stopped.");
	}

	public void destroy() {
		mdns.exit();
	}

	public void signal() {
		System.out.println("signal called!");
	}

	// readConf uses a conf file to carry ALL the group information! this is the only way
	// for us to "issue commands" to the headless operator
	public void readConf(String conffile) {
		FileInputStream fs;
		InputStreamReader isr;
		BufferedReader br;

		try {
			fs = new FileInputStream(conffile);
			isr = new InputStreamReader(fs);
			br = new BufferedReader(isr);
		}
		catch (Exception e) {
			System.err.println("daemon: could not open " + conffile);
			return;
		}

		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line);

				if (!st.hasMoreTokens()) {
					continue;
				}

	        	String type = st.nextToken();

	        	// for our commenting lines!
	        	if (type.charAt(0) == '#')
					continue;

				if (type.equals("ADDR")) {
					String addr = st.nextToken();
					mdns.setAddr(addr);
					
				} else if (type.equals("SERVICE")) {
	        		String servicename = st.nextToken();
	        		mdns.createService(servicename);


				} else if (type.equals("DNS")) {
					String name = st.nextToken();
					String addr = st.nextToken();
					mdns.addDNSServer(name, addr);

				} else if (type.equals("GROUP")) {
	        		String command = st.nextToken();
	        		if (command.equals("TOP")) {
	        			int gid = Integer.parseInt(st.nextToken());
	        			ArrayList<String> args = new ArrayList<String>();
	        			while (st.hasMoreTokens()) {
	        				args.add(st.nextToken());
	        			}

	        			DNSGroup group = mdns.createGroup(gid, args);

	        			if (group == null) {
	        				continue;
	        			}
	        			mdns.joinGroup(group);

	        		} else if (command.equals("SUB")) {
	        			String parent_name = st.nextToken();

	        			// lookup DNSGroup here
	        			DNSGroup parent = mdns.findGroupByName(parent_name);

	        			if (parent == null) {
	        				continue;
	        			}

	        			int gid = Integer.parseInt(st.nextToken());
	        			ArrayList<String> args = new ArrayList<String>();
	        			while (st.hasMoreTokens()) {
	        				args.add(st.nextToken());
	        			}

	        			DNSGroup group = mdns.createSubGroup(parent, gid, args);

	        			if (group == null) {
	        				continue;
	        			}
	        			mdns.joinGroup(group);
	        		}
	        	}

	        }
		} catch (IOException e) {
			// do nothing special here, we've already broken out of the loop
		}

		try {
			fs.close();
		} catch (IOException e) {
			// do nothing special here
		}
   		return;
	}
}