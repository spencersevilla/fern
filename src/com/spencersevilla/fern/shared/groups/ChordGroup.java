package com.spencersevilla.fern.groups;
import com.spencersevilla.fern.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

// possibly trim this down?
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Key;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.chord.service.Chord;

// implement FERNGroup, Runnable later on
public class ChordGroup extends FERNGroup implements Runnable {
	public static int id = 1;
	private Thread thread;
	private Chord chord;
	private boolean running;
	public InetAddress laddr = null;
	public int lport = 0;
	public InetAddress daddr = null;
	public int dport = 0;
	private ArrayList<FERNObject> objects;

	public int getId() {
		return ChordGroup.id;
	}

	public void start() {
		// WE are the server so start it up! (on its own thread)
		thread = new Thread(this);
		thread.start();
	}

	public void stop() {
		for (Iterator<FERNObject> it = objects.iterator(); it.hasNext(); ) {
			removeObject(it.next());
		}

		System.out.println("CG " + name + ": stopped.");
	}

	public ChordGroup(FERNManager m, Name n) {
		super(m, n);
		objects = new ArrayList<FERNObject>();
	}
	
	public ChordGroup(FERNManager m, Name n, ArrayList<String> nameArgs) {
		super(m, n);
		objects = new ArrayList<FERNObject>();

		if (nameArgs.get(0).equals("create")) {
			if (nameArgs.size() < 3) {
				System.err.println("CG " + name + " init error: invalid init string!");
				return;
			}

			try {
				laddr = InetAddress.getByName(nameArgs.get(1));
			} catch (Exception e) {
				System.err.println("CG " + name + " init error: invalid address!");
				return;
			}

			lport = Integer.parseInt(nameArgs.get(2));

		} else if (nameArgs.get(0).equals("join")) {
			if (nameArgs.size() < 5) {
				System.err.println("CG " + name + " init error: invalid init string!");
				return;
			}

			try {
				daddr = InetAddress.getByName(nameArgs.get(1));
				laddr = InetAddress.getByName(nameArgs.get(3));
			} catch (Exception e) {
				System.err.println("CG " + name + " init error: invalid address!");
				return;
			}

			dport = Integer.parseInt(nameArgs.get(2));
			lport = Integer.parseInt(nameArgs.get(4));

		} else {
			System.err.println("CG " + name + " init error: invalid command " + nameArgs.get(1));
			return;
		}
	}

	public void run() {
		if (laddr == null) {
				System.err.println("CG " + name + " start: no IP address?");
				return;
		}
		
		if (lport == 0) {
			lport = findFreePort();
		}
		
		if (daddr == null || dport == 0) {
			// don't have enough information to join a chord, so create one!
			createChord();
		} else {
			joinChord();
		}
	}

	// public boolean createSubGroup(String name) {
	// 	if (chord == null) {
	// 		System.err.println("CG " + fullName + " createSubGroup error: chord == null");
	// 		return false;
	// 	}

	// 	Service s = new Service(name, 1000, mdns);
	// 	s.addr = laddr;
	// 	StringKey key = new StringKey(s.name);		
	// 	Set set;
		
	// 	// make sure there are no other results for this group!
	// 	try {
	// 		set = chord.retrieve(key);
	// 		if (set == null) {
	// 			System.err.println("CG " + fullName + " error: retrieved null set");
	// 			return false;
	// 		}
	// 		if (!set.isEmpty()) {
	// 			return false;
	// 		}
	// 	} catch (Exception e) {
	// 		System.err.println("CG " + fullName + " error: chord.retrieve error!");
	// 		e.printStackTrace();
	// 		return false;
	// 	}
		
	// 	// now try inserting
	// 	try {
	// 		chord.insert(key, s.addr.getHostAddress());
	// 		System.out.println("CG " + fullName + ": inserted " + s.addr + " for key: " + s.name);
	// 		services.add(s);
	// 		return true;
	// 	} catch (Exception e) {
	// 		System.err.println("CG " + fullName + " error: could not insert " + s.addr + " for key: " + s.name);
	// 		e.printStackTrace();
	// 	}
		
	// 	return false;
	// }
	
	public void registerObject(FERNObject o) {
		objects.add(o);

		if (chord == null) {
			System.err.println("CG " + name + " registerObject error: chord == null");
			return;
		}
		
		StringKey key = new StringKey(o.getName().firstTerm().toString());
		
		try {
			chord.insert(key, o);
			System.out.println("CG " + name + ": inserted key: " + key);
		} catch (Exception e) {
			System.err.println("CG " + name + " error: could not insert for key: " + key);
			e.printStackTrace();
		}
	}
	
	public void removeObject(FERNObject o) {
		if (chord == null) {
			System.err.println("CG " + name + " serviceRemoved error: chord == null");
			return;
		}
		
		StringKey key = new StringKey(o.getName().firstTerm().toString());
		
		try {
			chord.remove(key, o);
			System.out.println("CG " + name + ": removed object for key: " + key);
		} catch (Exception e) {
			System.err.println("CG " + name + " error: could not remove object for key: " + key);
			e.printStackTrace();
		}
	}
	
	// public void serviceRemoved(Iterator<Service> it) {
		
	// 	Service s = it.next();
	// 	if (chord == null) {
	// 		System.err.println("CG " + fullName + " serviceRemoved error: chord == null");
	// 		return;
	// 	}
		
	// 	StringKey key = new StringKey(s.name);
	// 	it.remove();
		
	// 	try {
	// 		chord.remove(key, s.addr);
	// 		System.out.println("CG " + fullName + ": removed " + s.addr + " for key: " + s.name);
	// 	} catch (Exception e) {
	// 		System.err.println("CG " + fullName + " error: could not remove " + s.addr + " for key: " + s.name);
	// 		e.printStackTrace();
	// 	}
	// }
	
	public FERNObject resolveName(Request request) {
		if (chord == null) {
			System.err.println("CG " + name + " resolveService error: chord == null");
			return null;
		}
		
		System.out.println("CG " + name + ": resolving " + request);

		// STEP 1: Produce the key we're looking for!
		Name req = findNextHop(request);
		StringKey key = new StringKey(req.toString());
		Set set;

		try {
			set = chord.retrieve(key);
		} catch (Exception e) {
			System.err.println("CG " + name + " error: chord.retrieve!");
			e.printStackTrace();
			return null;
		}
		
		if (set == null) {
			System.err.println("CG " + name + " error: set was null!");
			return null;
		}
		
		if (set.isEmpty()) {
			System.err.println("CG " + name + " error: set was empty!");
			return null;
		}
		
		FERNObject o = (FERNObject) set.iterator().next();
		
		// OKAY: we've gotten an object for this query. the CALLING function
		// must decide if it's the end-answer or if it wants to continue forwarding
		// so either way, we just return it now!
		return o;
	}
	
	// cleanup method
	public void exit() {
		if (running) {
			try {
				chord.leave();
				System.out.println("CG " + name + ": exited chord");
			} catch (Exception e) {
				System.err.println("CG " + name + ": could not exit chord!");
				e.printStackTrace();
			}
		}
	}
	
	// Chord-specific functions! =======================================================
	private void createChord() {
		// de.uniba.wiai.lspi.chord.service.PropertiesLoader.loadPropertyFile();
		String protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		URL localURL = null;
		
		try {
			localURL = new URL(protocol + "://" + laddr.getHostAddress() + ":" + lport + "/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		chord = new de.uniba.wiai.lspi.chord.service.impl.ChordImpl();
		try {
			chord.create(localURL);
		} catch (ServiceException e) {
			throw new RuntimeException("CG " + name + " error: could not create", e);
		}
		
		System.out.println("CG " + name + ": created chord, serving at " + laddr + ":"+ lport);

		// WE created a chord that's not top-level! register ourself as the 
		// "parent" of this group since, presumably, we are a member of the
		// parent-group and can forward traffic onward appropriately.
		// if (groups.length > 1) {
		// 	Service s = new Service("parent", 0, mdns);
		// 	serviceRegistered(s);
		// }
	}
	
	private void joinChord() {
		// de.uniba.wiai.lspi.chord.service.PropertiesLoader.loadPropertyFile();
		String protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		URL localURL = null;
		URL joinURL = null;
		
		if (daddr == null || dport == 0) {
			System.err.println("CG " + name + ": cannot join chord, don't know where to find it!");
			return;
		}
				
		try {
			localURL = new URL(protocol + "://" + laddr.getHostAddress() + ":" + lport + "/");
			joinURL = new URL(protocol + "://" + daddr.getHostAddress() + ":" + dport +"/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		chord = new de.uniba.wiai.lspi.chord.service.impl.ChordImpl();
		try {
			chord.join(localURL, joinURL);
		} catch (ServiceException e) {
			throw new RuntimeException("CG " + name + " error: could not join!", e);
		}
		
		System.out.println("CG " + name + ": joined chord at " + daddr + ":" + dport + ", serving at " + laddr + ":" + lport);

		// We joined a Chord but we're ALSO a member of its parent-group,
		// so go ahead and add ourselves to the key "parent" :-)
		// if (parent != null) {
		// 	Service s = new Service("parent", 0, mdns);
		// 	serviceRegistered(s);
		// }
	}
	
	public static int findFreePort() {
		ServerSocket server;
		int port;
		try {
			server = new ServerSocket(0);
			port = server.getLocalPort();
		  	server.close();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	  	return port;
	}

	// public static void main(String[] args) {
	// 	String s;
		
	// 	if (args.length > 0) {
	// 		s = args[0];
	// 	} else {
	// 		s = "join";
	// 	}
		
	// 	Service serv = new Service("creator", 500, null);
		
	// 	if (s.equals("register")) {
	// 		ChordGroup c = new ChordGroup(null, "testchord");
	// 		c.joinChord();
	// 		c.serviceRegistered(serv);
	// 	} else if (s.equals("resolve")) {
	// 		ChordGroup c = new ChordGroup(null, "testchord");
	// 		c.joinChord();
	// 		String res = c.resolveService("creator");
	// 		System.out.println("CHORD RETURNED: " + res);
	// 	} else {
	// 		//create chord
	// 		ChordGroup c = new ChordGroup(null, "testchord");
	// 		c.createChord();
	// 	}
	// }
}
