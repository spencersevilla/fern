package com.spencersevilla.fern;

import java.util.*;
import java.io.*;
import java.net.InetAddress;

public class FERNObject implements Serializable {
	protected Name name;
	private ArrayList<Record> recordSet;
	public static final FERNObject NO_MATCH = new FERNObject(Name.NO_MATCH);

	public FERNObject(Name n) {
		n.terminate();
		name = n;
		recordSet = new ArrayList<Record>();
	}

	public FERNObject(FERNObject o) {
		name = o.getName();
		recordSet = o.getRecordSet();
	}

	public ArrayList<Record> getRecordSet() {
		return recordSet;
	}

	// for interface display
	public final String toString() {
		return name.toString();
	}

	public Name getName() {
		return new Name(name);
	}

	public final void pp() {
		System.out.println("FERNObject Name: "+ name);
		System.out.println(Record.HEADER);
		for (Record r : recordSet) {
			System.out.println(r.toString());
		}
	}

	public void addRecord(Record r) {
		if (r == null) {
			System.err.println("FERNObject error: null record!");
			return;
		}

		if (!r.getName().equals(name)) {
			System.err.println("FERNObject error: cannot add record!");
			return;
		}

		recordSet.add(r);
	}
	
	public void filterRecordType(int type) {
		if (type == Type.ANY) {
			return;
		}

		Iterator itr = recordSet.iterator();
		while (itr.hasNext()) {
			Record r = (Record) itr.next();
			if (r.getType() != type) {
				itr.remove();
			}
		}
	}

	// this is an awkward function that appends a servicename to
	// the end of a FERNGroup name to create a fully-qualified FERN object.
	public FERNObject(Service service, FERNGroup group) {
		this(service.getName().concatenate(group.name));

		// copy over the record-set. Note that the names must be null
		// since they came from a Service (unqualified Name)...
		for (Record r : service.getRecordSet()) {
			// the LOCALHOST record indicates that we never specified an address for
			// this record/host, so what we do is just ask the group for an address.
			// This is because different groups could be on different subnets, no reason
			// to give an incorrect or unreachable address. This is essentially NAT!
			if (r.equals(Record.LOCALHOST)) {
				Record rec = new Record(name, Type.A, DClass.IN, 0, group.getAddr().getAddress());
				addRecord(rec);
			} else {
				Record rec = new Record(name, r.getType(), r.getDClass(), r.getTTL(), r.getData());
				addRecord(rec);
			}
		}
	}

	// ideal for overwriting!
	public Response forwardMessage(Message message) {
		InetAddress a;
		for (Record r : recordSet) {
			if (r.getType() == Type.A) {
				try {
					a = InetAddress.getByAddress(r.getData());
					return InterGroupServer.sendMessage(message, a, 53);				
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.err.println("ERROR: forwardMessage called on a FERNObject with no valid A-record!!!");
		return null;
	}

	// This function returns 'true' iff the request-name could
	// potentially be construed to refer to the object itself.
	// NOTE that we MUST be referring to an object's FULL NAME!
	public boolean isExactMatch(Message request) {
		boolean retval = true;
		// req = [parc, csl, spencer]
		// objName = [global, parc, csl, spencer]

		String[] req = request.getName().getNameArray();
		String[] object = name.getNameArray();

		// the entry's fullname may be longer than the request
		// but the request can never be longer than the fullname!
		if (req.length > object.length) {
			return false;
		}

		// flip both the collections!!! now they both start with "spencer"
		Collections.reverse(Arrays.asList(req));
		Collections.reverse(Arrays.asList(object));

		for (int i = 0; i < req.length; i++) {
			if (!req[i].equals(object[i])) {
				retval = false;
				break;
			}
		}

		Collections.reverse(Arrays.asList(req));
		Collections.reverse(Arrays.asList(object));
		return retval;
	}

	protected final int calculateScore(Name request) {
		return FERNObject.calculateScore(request, name);		
	}

	public final int calculateScore(Message request) {
		return FERNObject.calculateScore(request.getName(), name);		
	}

	public static final int calculateScore(Name servicename, Name groupname) {
		if (servicename == null || groupname == null) {
			return 0;
		}

		String[] names = servicename.getNameArray();
		String[] groups = groupname.getNameArray();

		int count = 0;
		// the groupname may have a longer name than we specify
		// but this cannot be the other way around!
		// example: "james.ccrg.soe" maps to "ccrg.soe.ucsc" but not to "ccrg.csl.parc"
		// alternatively, "james.ccrg.soe" CAN map to "inrg.soe.ucla" even if unintended
		// THIS means that we have to start searching with the root of the name requested
		int startIndex = 0;
		
		for(startIndex = 0; startIndex < groups.length; startIndex++) {
			if (names[0].equals(groups[startIndex])) {
				break;
			}
		}
		
		// startIndex now has the index (on name) of the first successful match.
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

	public static FERNObject findBestMatch(Message request, ArrayList<? extends FERNObject> array) {
		return FERNObject.findBestMatch(request, array, null);
	}

	public static FERNObject findBestMatch(Message request, ArrayList<? extends FERNObject> array, FERNObject initial) {

		FERNObject bestChoice = null;
		int highScore = 0;

		if (initial != null) {
			bestChoice = initial;
			highScore = initial.calculateScore(request);
		}

		int score;
		for (FERNObject object : array) {
			score = object.calculateScore(request);
			if (score == highScore && score != 0) {
				System.out.println("FERNObject ERROR: for request " + request 
					+ ", both " + bestChoice + " and " + object + " have score " + score);
			} else if (score > highScore) {
				bestChoice = object;
				highScore = score;
			}
		}

		return bestChoice;
	}
}