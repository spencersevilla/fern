
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
}