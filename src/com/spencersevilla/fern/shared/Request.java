package com.spencersevilla.fern;

import java.util.*;

public class Request {
	public Name name;
	public int type;

	public final String toString() {
		return name.toString();
	}

	public Request(String n) {
		name = new Name(n);
	}

	public Request(Name n) {
		name = n;
	}
}