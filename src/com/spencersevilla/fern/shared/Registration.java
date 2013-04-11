
package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Registration extends Message implements Serializable {
	private ArrayList<Record> recordSet;

	public Registration(Name n) {
		super(n);
		recordSet = new ArrayList<Record>();
	}

	public void addRecord(Record r) {
		recordSet.add(r);
	}

	public ArrayList<Record> getRecordSet() {
		return (ArrayList<Record>) recordSet.clone();
	}

	public static Response successfulResponse(Message m) {
		Response ret = new Response(m);
		ret.setRetVal(Rcode.NOERROR);
		return ret;
	}

	public static Response unsuccessfulResponse(Message m) {
		Response ret = new Response(m);
		ret.setRetVal(Rcode.REFUSED);
		return ret;
	}
}