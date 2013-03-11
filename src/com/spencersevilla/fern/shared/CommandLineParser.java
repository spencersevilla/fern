package com.spencersevilla.fern;

import java.util.*;

public class CommandLineParser {
	public static void readCommandLine(String commandline, FERNInterface fern) {
		try {
			StringTokenizer st = new StringTokenizer(commandline);

	       	String type = st.nextToken();
	       	// for our commenting lines!
	       	if (type.charAt(0) == '#')
				return;

			// if (type.equals("ADDR")) {
			// 	String addr = st.nextToken();
			// 	m.setAddr(addr);
				
			if (type.equals("SERVICE")) {
	    		String command = st.nextToken();
	    		String str = st.nextToken();
	    		Name servicename = new Name(str);
	    		Service serv = new Service(servicename);
	    		serv.addRecord(Record.LOCALHOST);

				if (command.equals("CREATE")) {
	    			fern.registerService(serv);
				} else if (command.equals("REMOVE")) {
	    			fern.removeService(serv);
				}

			// } else if (type.equals("ICN")) {
	  //   		Name sname = new Name("icn");
	  //   		Service s = new Service(sname);
	  //   		String data = new String("XXX");
	  //   		Record r = new Record(null, Type.ICN, DClass.IN, 0, data.getBytes());
	  //   		s.addRecord(r);
	  //   		m.addService(s);

			// } else if (type.equals("DNS")) {
			// 	String addr = st.nextToken();
			// 	m.addDNSServer(addr);

			} else if (type.equals("GROUP")) {

				String command = st.nextToken();
				if (command.equals("TOP")) {
					int gid = Integer.parseInt(st.nextToken());
					Name group_name = new Name(st.nextToken());
					ArrayList<String> args = new ArrayList<String>();
					while (st.hasMoreTokens()) {
						args.add(st.nextToken());
					}

					fern.createGroup(group_name, gid, args);

					// if (group == null) {
					// 	System.out.println("CommandLineParser: could not create group!");
					// 	throw new RuntimeException();
					// }
					
					// m.joinGroup(group);

				} else if (command.equals("SUB")) {
					Name parent_name = new Name(st.nextToken());

					// lookup FERNGroup here
					// FERNGroup parent = m.findGroupByName(parent_name);

					// if (parent == null) {
					// 	System.out.println("CommandLineParser: could not find parent!");
					// 	throw new RuntimeException();
					// }

					int gid = Integer.parseInt(st.nextToken());
					Name subgroup_name = new Name(st.nextToken());

					ArrayList<String> args = new ArrayList<String>();
					while (st.hasMoreTokens()) {
						args.add(st.nextToken());
					}

					fern.createSubGroup(parent_name, subgroup_name, gid, args);

					// if (group == null) {
					// 	System.out.println("CommandLineParser: could not create group!");
					// 	throw new RuntimeException();
					// }

					// m.joinGroup(group);
				} else if (command.equals("LEAVE")) {
					Name group_name = new Name(st.nextToken());
					// FERNGroup group = m.findGroupByName(group_name);
					fern.leaveGroup(group_name);
				}
			// } else if (type.equals("CACHE")) {
			// 	String name = st.nextToken();
				// InetAddress
			}
		} catch (RuntimeException e) {
			System.out.println("CommandLineParser error: could not parse command line: " + commandline);
			e.printStackTrace();
		}
	}
}