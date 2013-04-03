package com.spencersevilla.fern;
import com.spencersevilla.fern.groups.*;

import java.util.*;
import java.net.InetAddress;

public abstract class FERNGroup extends FERNObject {

	public static final Name PARENT_NAME = new Name("PARENT");

	protected FERNManager mdns;
	public boolean recursive;
	public FERNGroup parent;
	protected InetAddress addr;
	
// These functions are SPECIFIC to FERNGroup and may NOT be overridden by another class
// ======================================================================================

	public FERNGroup(FERNManager m, Name n) {
		super(n);
		addRecord(FERNGroup.generateRecord(m,n));

		mdns = m;
		recursive = false;
		parent = null;
	}
	
	public static Record generateRecord(FERNManager m, Name n) {
		InetAddress addr = m.getAddr();
		if (addr == null) {
			addr = Service.generateAddress();
		}
		byte[] rdata = addr.getAddress();
		return new Record(n, Type.A, DClass.IN, 0, rdata);	
	}

	// This function identifies every FERNGroup uniquely using its fullname!
	// We might want to cover some other strcmps for formatting/subgroups/etc
	@Override public final boolean equals(Object otherObject) {
		// check for self-comparison
		if ( this == otherObject ) return true;
		// check for null and ensure class membership
		if ( !(otherObject instanceof FERNGroup) ) return false;
		
		FERNGroup that = (FERNGroup) otherObject;
		
		// traps for null-cases 
		return this.name == null ? that.name == null : this.name.equals(that.name);
	}
	
	// // useful hook/massager into createGroupFromArgs
	// public static final FERNGroup createGroupFromString(FERNManager m, String arg_string) {
	// 	String[] args = arg_string.split(":");
	// 	FERNGroup group = null;
		
	// 	if (args.length < 1) {
	// 		System.out.println("error: args.length < 1");
	// 		return null;
	// 	}

	// 	ArrayList<String> arglist = new ArrayList<String>(Arrays.asList(args));
	// 	int type = Integer.parseInt(arglist.remove(0));
		
	// 	return createGroupFromArgs(m, type, arglist);
	// }
	
	// This is the ONE entry-point for parsing a group ID and creating 
	// the appropriate FERNGroup. In theory, this is the ONLY spot 
	// that should have to be changed to support an additional group type.
	public static final FERNGroup createGroupFromArgs(FERNManager m, Name n, int gid, ArrayList<String> args) {
		FERNGroup group = null;

		if (gid == ServerGroup.id) {
			group = ServerGroup.createGroupFromArgs(m, n, args);
		// } else if (gid == ChordGroup.id) {
		// 	group = new ChordGroup(m, n, args);
		// } else 	if (gid == FloodGroup.id) {
		// 	group = new FloodGroup(m, n, args);
		} else {
			group = null;
		}
		
		return group;
	}

	// GOAL: given the request spencer.csl.parc and group parc.global,
	// we wish to produce the full request spencer.csl.parc.global.
	// if a match is not possible (ie query = spencer.csl.att) then
	// return NULL because we cannot combine these two terms.
	public Name extendName(Name message) {
		String[] query = message.getNameArray();
		String[] groupname = name.getNameArray();


		// we know the groupname is full/final, so it MUST contain the root
		// of the query somewhere. If not, then we can't possibly resolve it!
		String firstGroup = query[0];
		int startIndex;
		for (startIndex = 0; startIndex < groupname.length; startIndex++) {
			if (groupname[startIndex].equals(firstGroup)) {
				break;
			}
		}
		if (startIndex == groupname.length) {
			// could not find a match, so bail out!
			return null;
		}

		String[] ret = new String[query.length + startIndex];
		// first, fill-in the values from the groupname
		for (int i = 0; i < startIndex; i++) {
			ret[i] = groupname[i];
		}

		//next, complete the array with values from the query
		for (int i = 0; i < query.length; i++) {
			ret[i + startIndex] = query[i];
		}

		return new Name(ret);
	}
	
	// this returns a boolean that shows if this group is as good 
	// of a match as could possibly be made! "true" here means we will never
	// forward - either we find the service at this group or it doesn't exist
	// note that we subtract one from score for the servicename itself
	protected final boolean isBestPossibleMatch(Name n) {
		int maxLength = n.getLength();
		int score = calculateScore(n);
		return (score == maxLength);
	}
	
	protected final Name findNextHop(Message message) {
	// GOAL: compare the request "spencer.csl.parc"
	// with the groupname "parc.global" to produce the Name
	// "csl" which will be the next group to look for!
	// This function returns the macro "PARENT" if moving UP the tree.
		Name fullreq = extendName(message.getName());
		if (fullreq == null) {
			return null;
		}

		String[] querygroups = fullreq.getNameArray();
		String[] groups = name.getNameArray();

	// NOW querygroups = [global, parc, csl, spencer] and groups = [global, parc].
	// We go down the list until we find the first non-match in the query. If 
	// it's a member of this group, we return the shortName, if it's above 
	// this group we return the Name-macro "PARENT".
		for (int i = 0; i < groups.length; i++) {
			if (!querygroups[i].equals(groups[i])) {
				return PARENT_NAME;
			}
		}

		return new Name(querygroups[groups.length]);
	}

// These functions MAY be overridden but are provided for ease-of-use in development
// ======================================================================================

	public void setAddr(InetAddress a) {
		addr = a;
	}

	public InetAddress getAddr() {
		if (addr == null) {
			System.out.println(name + " WARNING: addr not set!!!");
			return Service.generateAddress();
		}

		return addr;
	}

	// for BootstrapServer
	// protected String getResponse() {
	// 	return null;
	// }
	
	public boolean joinGroup() {
		// no work really necessary here?
		start();
		return true;
	}

	// cleanup method
	public void exit() {
		System.out.println("exiting group: " + name);
	}

	public void registerService(Service s) {
		FERNObject o = new FERNObject(s, this);
		registerObject(o);
	}

	public void removeService(Service s) {
		FERNObject o = new FERNObject(s, this);
		removeObject(o);
	}

	public void registerSubGroup(FERNGroup group) {
		registerObject(group);
	}

	public void removeSubGroup(FERNGroup group) {
		removeObject(group);
	}

	// this function is a MAIN entry-point for FERNGroups. It takes as input
	// a Message and returns the appropriate response (AFTER all recusion +
	// forwarding is completed as necessary.)

	// LOGIC: first, are we in the end-group or not?
	// if we are, then parse the message and generate a (new) response.
	// if not, all we have to do is find the next-hop and return forwardMessage!
	public Response forwardMessage(Message message) {
		if (isFinalGroup(message)) {
			return parseMessage(message);
		} else {
			FERNObject o = getNextHop(message);
			return o.forwardMessage(message);
		}
	}

	public boolean isFinalGroup(Message message) {
		Name parent_name = message.getName().getParent();
		Message parent_message = new Message(parent_name);
		return isExactMatch(parent_message);
	}

	// 	FERNObject o = readMessage(message);

	// 	// this code should never be called! use NO_MATCH instead...
	// 	if (o == null) {
	// 		System.out.println("ERROR: readMessage should NEVER return null!!!");
	// 		return null;
	// 	}

	// 	// we got to the end and there's no match!
	// 	if (o.equals(FERNObject.NO_MATCH)) {
	// 		Response r = new Response(o);
	// 		r.setRequest(message);
	// 		return r;
	// 	}

	// 	// we got to the end and we're done!
	// 	if (o.isExactMatch(message)) {
	// 		Response r = new Response(o);
	// 		r.setRequest(message);
	// 		return r;
	// 	}

	// 	// we have the next-hop but we're not finished, so forward!
	// 	return o.forwardMessage(message);
	// }

	// the "owner" of a record is always its parent-group, so if we
	// "trim" the last name off of the message, it should be an exact-match.


	// public FERNObject readMessage(Message message) {
	// 	// only message-type that really *MUST* be supported...
	// 	if (message instanceof Request) {
	// 		Request req = (Request) message;
	// 		return resolveName(req);

	// 	// let's also give them a default registration method...
	// 	} else if (message instanceof Registration) {
	// 		Registration reg = (Registration) message;
	// 		FERNObject o = new FERNObject(reg.getName());
	// 		o.addRecord(reg.getRecord());
	// 		return registerObject(o);
	// 	}
	// }

// These functions MUST be overridden: they provide the core FERNGroup functionality!
// ======================================================================================

	public abstract int getId();
	public abstract void start();
	public abstract void stop();

	// the MAIN functionality lies in these functions!!!
	public abstract Response parseMessage(Message message);
	public abstract FERNObject getNextHop(Message message);

	// these must return (a) the requested object (b) the next-hop group or (c) FERNObject.NO_MATCH.
	// public abstract FERNObject resolveName(Request request);
	public abstract void registerObject(FERNObject o);
	public abstract void removeObject(FERNObject o);
}