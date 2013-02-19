package com.spencersevilla.fern;

import java.util.*;
import java.io.*;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.DClass;

public class FERNObject implements Serializable {
	public Name name;
	protected Record record;

	public FERNObject(Name n, Record r) {
		name = n;
		record = r;
	}

	// public FERNObject(Name n) {
	// 	name = n;
	// 	record = null;
	// 	System.out.println ("WARNING: FERNObject " + n + " created without a record...");
	// }

	public FERNObject(Record r) {
		name = new Name(r.getName());
		record = r;
	}

	// for interface display
	public final String toString() {
		return name.toString();
	}
	
	// this is an awkward function that appends a servicename to
	// the end of a FERNGroup name to create a fully-qualified FERN record.
	public FERNObject(Service service, FERNGroup group) {
		name = service.name.concatenate(group.name);

		byte[] rdata = service.addr.getAddress();
		record = Record.newRecord(name.toDNSName(), Type.A, DClass.IN, 0, rdata);
	}

	public FERNObject forwardRequest(Request request) {
		System.out.println("ERROR: class FERNObject cannot forward requests!!!");
		return null;
	}

	// This function returns 'true' iff the request-name could
	// potentially be construed to refer to the object itself.
	// NOTE that we MUST be referring to an object's FULL NAME!
	public boolean isExactMatch(Request request) {
		boolean retval = true;
		// req = [parc, csl, spencer]
		// objName = [global, parc, csl, spencer]

		String[] req = request.name.getNameArray();
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

	protected final int calculateScore(Request request) {
		return FERNObject.calculateScore(request.name, name);		
	}

	protected static final int calculateScore(Name servicename, Name groupname) {
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

	public static FERNObject findBestMatch(Request request, ArrayList<? extends FERNObject> array) {
		return FERNObject.findBestMatch(request, array, null);
	}

	public static FERNObject findBestMatch(Request request, ArrayList<? extends FERNObject> array, FERNObject initial) {
		FERNObject bestChoice = null;
		int highScore = 0;

		if (initial != null) {
			bestChoice = initial;
			highScore = initial.calculateScore(request);
		}

		int score;
		for (FERNObject object : array) {
			score = object.calculateScore(request);
			if (score == highScore) {
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