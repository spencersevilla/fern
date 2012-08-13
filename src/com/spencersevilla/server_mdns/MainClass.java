package com.spencersevilla.server_mdns;
import com.spencersevilla.mdns.*;

import org.apache.commons.daemon.*;

import java.io.*;
import java.util.*;

public class MainClass implements Daemon, DaemonUserSignal {
	protected MultiDNS mdns;
	private String conf;

	public static void main(String[] args) throws Exception {
		System.out.println("MAIN");

		if (args.length > 1) {
			System.out.println("usage: MainClass [conf_file]");
			System.exit(0);
		}

		MainClass m = new MainClass();
		m.mdns = new MultiDNS();

		if (m.mdns == null) {
			System.out.println("error: could not initialize mdns!");
			System.exit(0);
		}

		if (args.length > 0) {
			m.conf = args[0];
		} else {
			m.conf = "mdns.conf";
		}

		m.start();
	}

	public MainClass() throws Exception {
	}
	
	// Java Daemon interface here! ============================================

	public void init(DaemonContext context) throws DaemonInitException, Exception {
		System.out.println("daemon: init!");
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
			conf = "mdns.conf";
		}
	}

	public void start() throws Exception {
		System.out.println("daemon: start!");
		mdns.start();
		readConf(conf);
	}

	public void stop() throws Exception {
		System.out.println("daemon: stop!");
		mdns.stop();
	}

	public void destroy() {
		System.out.println("daemon: destroy!");
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
			System.out.println("daemon: could not open " + conffile);
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

	        	if (type.equals("GROUP")) {
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
	        	} else if (type.equals("SERVICE")) {
	        		String servicename = st.nextToken();
	        		mdns.createService(servicename);
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