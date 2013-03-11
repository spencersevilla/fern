package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Request extends Message implements Serializable {
	private int type;

	public Request(String n) {
		super(n);
		type = Type.ANY;
	}

	public Request(Name n) {
		super(n);
		type = Type.ANY;
	}

	public void setType(int x) {
		// NO type-checking here???
		type = x;
	}

	public int getType() {
		return type;
	}
}