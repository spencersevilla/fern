package com.spencersevilla.fern;

import java.util.*;
import org.xbill.DNS.TextParseException;

public class Name {
	private String name;
	private String[] nameArray;
	private int length;

	public Name(String n) {
		if (n == null) {
			System.err.println("Name: ERROR: null string!");
			return;
		}

		name = n;

		String[] array = name.split("\\.");

		Collections.reverse(Arrays.asList(array));
		nameArray = array;

		length = nameArray.length;
	}

	public Name(Name n) {
		this(n.getName());
	}

	public Name(String[] array) {
		if (array == null) {
			System.err.println("Name: ERROR: null array!");
			return;
		}

		Collections.reverse(Arrays.asList(array));

		length = array.length;


		name = array[0];
		for(int i = 1; i < array.length; i++) {
			name = name + "." + array[i];
		}

		Collections.reverse(Arrays.asList(array));
		nameArray = array;
	}

	public String[] getNameArray() {
		return nameArray;
	}

	public String getName() {
		return name;
	}

	public int getLength() {
		return length;
	}

	public final String toString() {
		return name;
	}

	@Override public final boolean equals(Object otherObject) {
		// check for self-comparison
		if ( this == otherObject ) return true;
		// check for null and ensure class membership
		if ( !(otherObject instanceof Name) ) return false;
		
		Name that = (Name) otherObject;
		
		// traps for null-cases
		return this.name == null ? that.name == null : this.name.equals(that.name);
	}

	// org.xbill.DNS.Name wrappers ============================================

	public org.xbill.DNS.Name toDNSName() {
		try {
			String s = name;

			if (!s.endsWith(".")) {
				s = s.concat(".");
			}
			return new org.xbill.DNS.Name(s);
			
		} catch (TextParseException e) {
			System.err.println("BIG ERROR: could not generate xbill.DNS.Name!");
			return null;
		}
	}

	public void terminate() {
		if (!name.endsWith(".")) {
			name = name.concat(".");
		}
	}

	public Name(org.xbill.DNS.Name n) {
		this(n.toString());
	}

	public void fernify() {
		if (!name.endsWith(".")) {
			name = name.concat(".");
		}

		if (!name.endsWith("fern.")) {
			name = name.concat("fern.");
			String[] array = name.split("\\.");
			Collections.reverse(Arrays.asList(array));
			nameArray = array;
			length = nameArray.length;
		}
	}

	public void unfern() {
		if (name.endsWith(".")) {
			name = name.substring(0, name.length() - 1);
		}

		if (name.endsWith(".fern")) {
			name = name.substring(0, name.length() - 5);
			String[] array = name.split("\\.");
			Collections.reverse(Arrays.asList(array));
			nameArray = array;
			length = nameArray.length;
		}
	}

	// UTILITY FUNCTIONS ======================================================

	// turns "spencer.csl.parc" into "csl.parc"
	public Name getParent() {
		if (this.length == 1) {
			return null;
		}

		int cut = name.indexOf(".");
		if (cut == -1) {
			return null;
		}

		return new Name(name.substring(cut + 1));
	}

	// turns "spencer.csl.parc" into "spencer"
	public Name firstTerm() {

		int cut = name.indexOf(".");
		if (cut == -1) {
			return this;
		}

		return new Name(name.substring(0, cut));
	}

	public Name concatenate(Name n2) {
		String name2 = n2.toString();

		if (name.endsWith(".")) {
			name = name.substring(0, name.length() - 1);
		}

		if (name2.startsWith(".")) {
			name2 = name2.substring(1);
		}

		return new Name(name.toString() + "." + name2.toString());
	}
}