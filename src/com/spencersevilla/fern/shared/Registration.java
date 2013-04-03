
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

	public ArrayList<Record> getRecordSet() {
		return null;
	}

	public static Response successfulResponse(Registration reg) {
		Response ret = new Response(reg);
		ret.setRetVal(Rcode.NOERROR);
		return ret;
	}

	public static Response unsuccessfulResponse(Registration reg) {
		Response ret = new Response(reg);
		ret.setRetVal(Rcode.REFUSED);
		return ret;
	}
}