package com.spencersevilla.fern;

import java.util.*;

public class CommandLineParser {
	public static void readCommandLine(String commandline, FERNManager m) {
		try {
			StringTokenizer st = new StringTokenizer(commandline);

	       	String type = st.nextToken();
	       	// for our commenting lines!
	       	if (type.charAt(0) == '#')
				return;

			if (type.equals("ADDR")) {
				String addr = st.nextToken();
				m.setAddr(addr);
				
			} else if (type.equals("SERVICE")) {
	    		String str = st.nextToken();
	    		Name servicename = new Name(str);
	    		m.createService(servicename);

			} else if (type.equals("DNS")) {
				String addr = st.nextToken();
				m.addDNSServer(addr);

			} else if (type.equals("GROUP")) {

				String command = st.nextToken();
				if (command.equals("TOP")) {
					int gid = Integer.parseInt(st.nextToken());
					Name group_name = new Name(st.nextToken());
					ArrayList<String> args = new ArrayList<String>();
					while (st.hasMoreTokens()) {
						args.add(st.nextToken());
					}

					FERNGroup group = m.createGroup(group_name, gid, args);

					if (group == null) {
						throw new RuntimeException();
					}
					
					m.joinGroup(group);

				} else if (command.equals("SUB")) {
					Name parent_name = new Name(st.nextToken());

					// lookup FERNGroup here
					FERNGroup parent = m.findGroupByName(parent_name);

					if (parent == null) {
						throw new RuntimeException();
					}

					int gid = Integer.parseInt(st.nextToken());
					Name subgroup_name = new Name(st.nextToken());

					ArrayList<String> args = new ArrayList<String>();
					while (st.hasMoreTokens()) {
						args.add(st.nextToken());
					}

					FERNGroup group = m.createSubGroup(parent, subgroup_name, gid, args);

					if (group == null) {
						throw new RuntimeException();
					}

					m.joinGroup(group);
				}
			} else if (type.equals("CACHE")) {
				String name = st.nextToken();
				// InetAddress
			}
		} catch (RuntimeException e) {
			System.out.println("CommandLineParser error: could not parse command line: " + commandline);
		}
	}
}