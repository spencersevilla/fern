package com.spencersevilla.fern;

import java.util.*;

public class Request {
	private Name name;
	private int type;

	public final String toString() {
		return name.toString();
	}

	public Request(String n) {
		name = new Name(n);
	}

	public Request(Name n) {
		name = n;
	}

	public Name getName() {
		return new Name(name);
	}
}