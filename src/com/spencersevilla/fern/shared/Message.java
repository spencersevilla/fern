package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Message implements Serializable {
	protected Name name;

	public Message(Name n) {
		name = n;
	}

	public Message(String n) {
		name = new Name(n);
	}

	public final String toString() {
		return name.toString();
	}

	public Name getName() {
		return new Name(name);
	}
}