package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Request extends Message implements Serializable {
	private int type;

	public Request(String n) {
		super(n);
	}

	public Request(Name n) {
		super(n);
	}
}