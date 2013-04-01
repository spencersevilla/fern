
package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Registration extends Message implements Serializable {
	private Record record;

	public Registration(Name n, Record r) {
		super(n);
		record = new Record(r);
	}

	public Registration(Record r) {
		this(r.getName(), r);
	}

	public Record getRecord() {
		return new Record(record);
	}

	public static Response successfulResponse(Registration reg) {
		Response ret = new Response(reg);
		int retval = 0;
	}

	public static Response unsuccessfulResponse(Registration reg) {
		Response ret = new Response(reg);
		int retval = 5;
	}
}