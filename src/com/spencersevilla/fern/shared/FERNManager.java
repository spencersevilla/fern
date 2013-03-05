package com.spencersevilla.fern;

import java.io.*;
import java.net.*;
import java.util.*;

public class FERNManager {
    
    // protected BootstrapServer bs;
	protected InterGroupServer igs;
	protected boolean running;
	private String hostname;
	private InetAddress address;

	// For User Preferences
	// private XStream xstream;
	// private static final String service_file = "config/services.xml";
	// private static final String group_file = "config/groups.xml";
    
	public ArrayList<FERNGroup> groupList;
	public ArrayList<FERNObject> cacheList;
	public ArrayList<Service> serviceList;
	public ArrayList<FERNGroup> allGroups;

	// This function only called ONCE! (initializer)
	public FERNManager() throws Exception {
        running = false;
        hostname = null;
        address = null;

        groupList = new ArrayList<FERNGroup>();
        cacheList = new ArrayList<FERNObject>();
		serviceList = new ArrayList<Service>();
		allGroups = new ArrayList<FERNGroup>();

		// here we initialize (but don't start) all of the servers.
        // bs = new BootstrapServer(this);
		igs = new InterGroupServer(this);

		System.out.println("FERN: initialized!");
	}

	// This function can go back-and-forth with start() and stop()
	public void start() throws Exception {
		// bs.start();
		igs.start();
		System.out.println("FERN: started!");
	}
	
	public void stop() throws Exception {
		// bs.stop();
		igs.stop();
		System.out.println("FERN: stopped!");

		// also pause every service we're a part of
		for (FERNGroup group : groupList) {
			group.stop();
		}
	}

	public void exit() {
		System.out.println("CLEAN UP FERN HERE!");
	}

	public void shutDown() {
		System.exit(0);
	}

	// INTERNAL API (ACCESSABLE ONLY WITHIN PACKAGE!!!) =======================
	
	protected FERNGroup findGroupByName(Name fullName) {
		for (FERNGroup g : groupList) {
			if (g.name.equals(fullName)) {
				return g;
			}
		}
		return null;
	}

	protected FERNGroup createGroup(Name name, int gid, ArrayList<String> args) {
		FERNGroup group = FERNGroup.createGroupFromArgs(this, name, gid, args);

		if (group == null) {
			System.out.println("FERNGroup error: could not create group!");
			return null;
		}

		allGroups.add(group);
		return group;
	}

	protected FERNGroup createSubGroup(FERNGroup parent, Name name, int gid, ArrayList<String> args) {
		// update the name to reflect that it's a subgroup
		Name fullname = name.concatenate(parent.name);

		// check the parent to see if it already exists?
		if (parent.resolveName(new Request(fullname)) != null) {
			System.err.println("FERN error: group already exists!");
			return null;
		}
		
		// create the according FERNGroup...
		FERNGroup g = createGroup(fullname, gid, args);
		if (g == null) {
			System.err.println("FERN error: cannot create subgroup!");
			return null;
		}

		// ...and register it with its parent!
		g.parent = parent;
		parent.registerSubGroup(g);

		return g;
	}

	protected void joinGroup(FERNGroup group) {
		if (groupList.contains(group)) {
			// cannot join group we're already a member of
			return;
		}
		
		if (!group.joinGroup()) {
			return;
		}
		
		groupList.add(group);
		
		// tell it about already existing services
		for (Service s : serviceList) {
			group.registerService(s);
		}
		
		// for some reason this slows us down enough to prevent errors
		try{
			Thread.sleep(1000);
		} catch (Exception e) {
			// do nothing; continue normally
		}
	}

	protected void leaveGroup(FERNGroup g) {
		g.stop();
		groupList.remove(g);
	}

	protected void createService(Name n) {
		Service s = new Service(n);
		s.generateRecord(this.address);
		addService(s);
	}

	protected void addService(Service s) {
		// check for duplicates here
		if (serviceList.contains(s)) {
			return;
		}
		
		// alert all FERNGroup instances
		for (FERNGroup group : groupList) {
			group.registerService(s);
		}

		serviceList.add(s);
	}

	protected void deleteService(Service s) {
		// alert all FERNGroup instances
		for (FERNGroup group : groupList) {
			group.removeService(s);
		}
		
		serviceList.remove(s);
	}

	public Response resolveService(Request request) {
		System.out.println("FERNManager resolving request: " + request);
		// ORDER OF OPERATIONS:
		// First, find a FERNObject which is OUR best-match. This can be
		// a record we're offering, an external group (cached?), a group we're a
		// member of, or any other information we have our hands on.
		FERNObject object = findBestMatch(request);

		// no matches? apparently we can't handle this request at all!
		if (object == null) {
			System.out.println("FERNManager: could not find ANY match for " + request);
			return null;
		}

		// this object fulfills the request, so we're done: return it!
		if (object.isExactMatch(request)) {
			Response r = new Response(object);
			r.setRequest(request);
			return r;
		}

		// our best-match isn't exact, so use it to keep forwarding.
		// note that when this response returns, we *add* our entry
		// to it in order to facilitate caching! Note that we also
		// still need to READ this response for other cached entries :-)
		Response r = object.forwardRequest(request);
		r.addOtherEntry(object);
		return r;
	}

	// PRIVATE METHODS HERE ===================================================

	private FERNObject findBestMatch(Request request) {
		// THIS is a major function that goes through our cache, services offered,
		// FERNGroups we're a member of, and any other objects we may be aware of.
		// It returns the best possible name-match across ALL objects.

		// First: check Cache for best-hit
		FERNObject result = FERNObject.findBestMatch(request, cacheList);

		// Second: check list of Groups joined for best-hit
		result = FERNObject.findBestMatch(request, groupList, result);

		// Last: if we've got a Group we're a member of, can we extend the result 
		// with the list of services we offer?
		if (result instanceof FERNGroup) {
			result = checkNames(request, (FERNGroup) result);
		}

		// Return object. (null) means we got NOTHING on this request at all!
		return result;
	}
	
	// I think I want to move this method into the FERNGroup class...
	private FERNObject checkNames(Request request, FERNGroup group) {
		// This function scans our list of Services (which are ONLY named
		// with their shortname) to see if we can combine them to make a 
		// better match on the request-name.
		int groupScore = group.calculateScore(request);

		for (Service s : serviceList) {
			// create new name by appending group to service
			Name n = s.getName().concatenate(group.name);

			// we've got a winner!!!
			if (FERNObject.calculateScore(request.getName(), n) > groupScore) {
				return new FERNObject(s, group);
			}
		}
		// no winners. just go ahead and return the original group then...
		return group;
	}

	// CLEANUP FUNCTIONS HERE =================================================
	public void setAddr(String addr) {
		// only allow this to be set ONCE!
		if (address != null) {
			return;
		}

		try {
			InetAddress a = InetAddress.getByName(addr);
			address = a;
		} catch (UnknownHostException e) {
			System.err.println("FERN error: given an invalid address!");
			e.printStackTrace();
		}
	}

	protected InetAddress getAddr() {
		return address;
	}

	// ???
	public void addDNSServer(String address) {
		try {
			InetAddress addr = InetAddress.getByName(address);
			igs.addDNSServer(addr);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	// public void readCommandLine(String commandline) {
	// 	try {
	// 		StringTokenizer st = new StringTokenizer(commandline);

	//        	String type = st.nextToken();
	//        	// for our commenting lines!
	//        	if (type.charAt(0) == '#')
	// 			return;

	// 		if (type.equals("ADDR")) {
	// 			String addr = st.nextToken();
	// 			setAddr(addr);
				
	// 		} else if (type.equals("SERVICE")) {
	//     		String servicename = st.nextToken();
	//     		createService(new Name(servicename));


	// 		} else if (type.equals("DNS")) {
	// 			String addr = st.nextToken();
	// 			addDNSServer(addr);

	// 		} else if (type.equals("GROUP")) {

	// 			String command = st.nextToken();
	// 			if (command.equals("TOP")) {
	// 				int gid = Integer.parseInt(st.nextToken());
	// 				ArrayList<String> args = new ArrayList<String>();
	// 				while (st.hasMoreTokens()) {
	// 					args.add(st.nextToken());
	// 				}

	// 				FERNGroup group = createGroup(gid, args);

	// 				if (group == null) {
	// 					throw new RuntimeException();
	// 				}
					
	// 				joinGroup(group);

	// 			} else if (command.equals("SUB")) {
	// 				String parent_name = st.nextToken();

	// 				// lookup FERNGroup here
	// 				FERNGroup parent = findGroupByName(new Name(parent_name));

	// 				if (parent == null) {
	// 					throw new RuntimeException();
	// 				}

	// 				int gid = Integer.parseInt(st.nextToken());
	// 				ArrayList<String> args = new ArrayList<String>();
	// 				while (st.hasMoreTokens()) {
	// 					args.add(st.nextToken());
	// 				}

	// 				FERNGroup group = createSubGroup(parent, gid, args);

	// 				if (group == null) {
	// 					throw new RuntimeException();
	// 				}

	// 				joinGroup(group);
	// 			}
	// 		} else if (type.equals("CACHE")) {
	// 			String name = st.nextToken();
	// 			// InetAddress
	// 		}
	// 	} catch (RuntimeException e) {
	// 		System.out.println("CommandLineParser error: could not parse command line: " + commandline);
	// 	}
	// }
}

	// protected void findOtherGroups() {
	// 	// ArrayList<FERNGroup> groups = bs.findGroups();
	// 	ArrayList<FERNGroup> groups = null;
	// 	System.out.println("FERN ERROR: BootstrapServer IS DISABLED!");

	// 	if (groups == null) {
	// 		return;
	// 	}
		
	// 	// don't add groups we've already seen before (member or otherwise!)
	// 	for (FERNGroup g : groups) {
	// 		if (allGroups.contains(g)) {
	// 			continue;
	// 		}
	// 		allGroups.add(g);
	// 	}
	// }

		// "group" is the best-match-DNS-group available to us!
		// FERNGroup group = findResponsibleGroup(request);

		// // FIRST: is this me?
		// retval = checkSelf(request, group);
		// if (retval != null) {
		// 	return retval;
		// }

		// // NEXT: do we have any cached group information?
		// addr = askCache(servicename, group);
		// if (addr != null) {
		// 	System.out.println("FERN: request for " + servicename + " hit cache.");
		// 	return addr;
		// }

		// // LAST: go ahead and query the group, if it exists
		// if (group == null) {
		// 	// nowhere to foward, we don't know anyone in this hierarchy!
		// 	// note: MAYBE flooding a request could find cached information
		// 	// BUT that's unlikely and could even be a DDOS attack vector
		// 	System.err.println("FERN error: could not find a responsible group for " + servicename);
		// 	return null;
		// }
		
		// addr = group.resolveService(servicename);
		// long elapsed_millis = System.currentTimeMillis() - start_time;
		// System.out.println("FERN: request for " + servicename + " took " + elapsed_millis + " milliseconds.");
		// return addr;

	// public InetAddress forwardRequest(String servicename, FERNGroup group) {
	// 	// forward or rebroadcast the request to a different group if possible
	// 	// minScore ensures that we don't loop: we can only forward to another group
	// 	// if it's a better match than the group this request came from.
		
	// 	FERNGroup g = findResponsibleGroup(servicename, group);

	// 	if (g == null) {
	// 		System.err.println("FERN: cannot forward request " + servicename + ", so ignoring it.");
	// 		return null;
	// 	}
		
	// 	System.out.println("FERN: forwarding request " + servicename + " internally to group " + g);
	// 	return g.resolveService(servicename);
	// }
	
	// public InetAddress forwardRequest(String servicename, FERNGroup group, InetAddress addr, int port) {
	// 	System.out.println("FERN: forwarding request " + servicename + " to " + addr + ":" + port);
	// 	return igs.resolveService(servicename, 0, addr, port);
	// }

	// private void loadServices() {		
	// 	FileInputStream fis = null;
	// 	ObjectInputStream in = null;
	// 	try {
	// 		fis = new FileInputStream(service_file);
	// 		in = new ObjectInputStream(fis);
	// 		String xml = (String)in.readObject();
	// 		Object[] objs = (Object[]) xstream.fromXML(xml);
	// 		for (Object o : objs) {
	// 			Service s = (Service) o;
	// 			createService(s.name);
	// 		}
	// 		in.close();

	// 	} catch (FileNotFoundException e) {
	// 		// create a very elementary appdata.xml file and save?
	// 		System.err.println("FERN: no services.xml file!");
	// 	} catch (Exception e) {
	// 		e.printStackTrace();
	// 	}
	// }
// 
// 	// default behavior is to join all the previous groups it can find
// 	// BUT to not bother creating groups that don't already exist
// 	private void loadGroups() {
// 		FileInputStream fis = null;
// 		ObjectInputStream in = null;
// 		try {
// 			fis = new FileInputStream(group_file);
// 			in = new ObjectInputStream(fis);
// 			String xml = (String)in.readObject();
// 			Object[] objs = (Object[]) xstream.fromXML(xml);
// 			for (Object o : objs) {
// 				FERNGroup g = (FERNGroup) o;
// 				// TODO: clean-up this method?
// 				// createGroup(g.name);
// 			}
// 			in.close();

// 		} catch (FileNotFoundException e) {
// 			// create a very elementary appdata.xml file and save?
// 			System.out.println("no groups.xml file!");
// 		} catch (Exception e) {
// 			e.printStackTrace();
// 		}
// 	}
	
// 	private void saveServices() {
// 		Object[] objs = serviceList.toArray();
// 		String xml = xstream.toXML(objs);
		
// 		FileOutputStream fos = null;
// 		ObjectOutputStream out = null;
// 		try {
// 			fos = new FileOutputStream(service_file);
// 			out = new ObjectOutputStream(fos);
// 			out.writeObject(xml);
// 			out.close();
// 		} catch (Exception e) {
// 			e.printStackTrace();
// 		}
// 	}

// 	private void saveGroups() {
// //		Object[] objs = dnsGroups();
// 		FloodGroup group = new FloodGroup(this, "test2");
// 		String xml = xstream.toXML(group);
		
// 		FileOutputStream fos = null;
// 		ObjectOutputStream out = null;
// 		try {
// 			fos = new FileOutputStream(group_file);
// 			out = new ObjectOutputStream(fos);
// 			out.writeObject(xml);
// 			out.close();
// 		} catch (Exception e) {
// 			e.printStackTrace();
// 		}
// 	}