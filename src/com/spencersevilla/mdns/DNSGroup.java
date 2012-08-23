package com.spencersevilla.mdns;

import java.util.*;
import java.net.InetAddress;

public abstract class DNSGroup {
	public String name;
	public String fullName;
	protected String[] groups;
	protected MultiDNS mdns;
	public boolean recursive;
	public static int id;
	
	public DNSGroup(MultiDNS m, String n) {
		mdns = m;
		recursive = false;
		
		fullName = n;
		groups = n.split("\\.");
		name = groups[0];
		
		// root hierarchy now at the front! (like typical filesystem)
		Collections.reverse(Arrays.asList(groups));
	}
	
	// This function identifies every DNSGroup uniquely using its fullname!
	// We might want to cover some other strcmps for formatting/subgroups/etc
	@Override public boolean equals(Object otherObject) {
		// check for self-comparison
		if ( this == otherObject ) return true;
		// check for null and ensure class membership
		if ( !(otherObject instanceof DNSGroup) ) return false;
		
		DNSGroup that = (DNSGroup) otherObject;
		
		// traps for null-cases 
		return this.fullName == null ? that.fullName == null : this.fullName.equals(that.fullName);
	}
	
	public static final DNSGroup createGroupFromString(MultiDNS m, String arg_string) {
		String[] args = arg_string.split(":");
		DNSGroup group = null;
		
		if (args.length < 1) {
			System.out.println("error: args.length < 1");
			return null;
		}

		ArrayList<String> arglist = new ArrayList<String>(Arrays.asList(args));
		int type = Integer.parseInt(arglist.remove(0));

		if (type == FloodGroup.id) {
			group = new FloodGroup(m, arglist);
		} else if (type == ChordGroup.id) {
			group = new ChordGroup(m, arglist);
		} else {
			group = null;
		}
		
		return group;
	}
	
	// this returns a boolean that shows if this group is as good 
	// of a match as could possibly be made! "true" here means we will never
	// forward - either we find the service at this group or it doesn't exist
	// note that we subtract one from score for the servicename itself
	protected boolean isBestPossibleMatch(String name) {
		String[] names = name.split("\\.");
		int maxLength = names.length;
		int retVal = calculateScore(name);
		return (retVal == maxLength);
	}
	
	// for interface display
	public String toString() {
		return fullName;
	}
	
	// for BootstrapServer
	protected String getResponse() {
		return null;
	}
	
	protected int calculateScore(String servicename) {
		String[] names = servicename.split("\\.");
		Collections.reverse(Arrays.asList(names));
		int count = 0;
		// the DNSGroup may have a longer name than we specify
		// but this cannot be the other way around!
		// example: "james.ccrg.soe" maps to "ccrg.soe.ucsc" but not to "ccrg.csl.parc"
		// alternatively, "james.ccrg.soe" CAN map to "inrg.soe.ucla" even if unintended
		// THIS means that we have to start searching with the root of the DNS name requested
		int startIndex = 0;
		
		for(startIndex = 0; startIndex < groups.length; startIndex++) {
			if (names[0].equals(groups[startIndex])) {
				break;
			}
		}
		
		// startIndex now has the index (on fullNames) of the first successful match.
		if (startIndex == groups.length) {
			// there was no match
			return 0;
		}
		
		// make sure we don't go "over the edge"
		int numReps = Math.min(names.length, groups.length - startIndex);
		
		// this for-loop starts at the first successful comparison
		// and then counts the number of successful comparisons thereafter
		for(count = 0; count < numReps; count++) {
			if (!names[count].equals(groups[startIndex + count])) {
				break;
			}
		}
		
		// last addition: if we maxed-out the group's full name, add 1 to signify that
		// we want to choose it as a parent! ie: searching for "james.csl.parc.global" gives:
		// "isl.parc.global" -> 2
		// "parc.global" -> 3 because it really represents "*.parc.global"
		// "csl.parc.global" -> 4 to distinguish from above, employs same logic
		if (count == (groups.length - startIndex)) {
			count++;
		}
		
		// System.out.println("groups.length = " + groups.length + " startIndex = " + startIndex);
		
		return count;
	}
	
	// the core methods for all groups
	public abstract void start();
	public abstract void stop();
	public abstract void serviceRegistered(Service s);
	public abstract void serviceRemoved(Service s);
	public abstract InetAddress resolveService(String name);
	public abstract InetAddress resolveService(String name, int minScore);
	
	public boolean joinGroup() {
		// no work really necessary here?
		start();
		return true;
	}	
	public boolean createSubGroup(String name) {
		return false;
	}
	
	// cleanup method
	public void exit() {
		System.out.println("exiting group: " + name);
	}

	protected String getServiceName(String fullname) {
		// GOAL: compare the fullname "spencer.csl.parc"
		// with the chord name "parc.global" to produce the string
		// "csl" which will be the key that the chord will search for!
		String[] servicegroups = fullname.split("\\.");
		Collections.reverse(Arrays.asList(servicegroups));
		
		// NOW: we're comparing the array servicegroups [parc, csl, spencer] with
		// the chordgroup fullname array groups [global, parc] to figure out which
		// entry in servicegroups we're looking for!
		int startIndex = 0;
		
		for(startIndex = 0; startIndex < groups.length; startIndex++) {
			if (servicegroups[0].equals(groups[startIndex])) {
				break;
			}
		}
		
		if (startIndex == groups.length) {
			System.err.println("CG " + fullName + ": getServiceName didn't find the name?");
			return null;
		}
		
		// HERE: startIndex has the first "hit" in the chordgroup fullname array
		// The only way this function can operate is by going THROUGH the entire
		// chordgroup's full name and finding it's first child. If this doesn't work,
		// then there's an error! ie a ChordGroup of "parc.usa.global" can only answer
		// queries for one level down, ie "XXX.parc.usa.global".
		int retIndex = groups.length - startIndex;
		if (retIndex < 0 || servicegroups.length <= retIndex) {
			System.err.println("CG " + fullName + ": getServiceName bounds error");
			return null;
		}
		
		// double-check but every entry here should be equal!
		for (int i = 0; i < retIndex; i++) {
			if (!servicegroups[i].equals(groups[startIndex + i])) {
				System.err.println("CG " + fullName + ": getServiceName could not make sense of groups");
				return null;
			}
		}
		
		return servicegroups[retIndex];
		// Chord CANNOT answer anything at all for "*.att.usa.global", "*.usa.global", etc.
		// Find a way to forward up? Maybe a key for "parent"?
	}
}