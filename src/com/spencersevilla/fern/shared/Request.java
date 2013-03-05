package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Request implements Serializable {
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