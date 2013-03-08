package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Record implements Serializable {
	public static final String HEADER = String.format("%-25s%-10s%-10s%-10s%-10s", "NAME", "TYPE", "CLASS", "TTL", "DATA");

	private Name name;
	private int type, dclass;
	private long ttl;
	private byte[] data;

	private final String TAB = new String("\t");

	public Record(Name n, int t, int d, long tt, byte[] dat) {
		name = n;
		type = t;
		dclass = d;
		ttl = tt;
		data = dat;
	}

	public Record(Record r) {
		name = r.getName();
		type = r.getType();
		dclass = r.getDClass();
		ttl = r.getTTL();
		data = r.getData();
	}

	public Name getName() {
		if (name == null) {
			return null;
		}
		
		return new Name(name);
	}

	public int getType() {
		return type;
	}

	public int getDClass() {
		return dclass;
	}

	public long getTTL() {
		return ttl;
	}

	public byte[] getData() {
		return data.clone();
	}

	public String toString() {
		return String.format("%-25s%-10s%-10s%-10s%-10s", name, Type.toString(type), DClass.toString(dclass), ttl, dataString());
	}

	private String dataString() {
		if (type == Type.A) {
			return Record.toDottedQuad(data);
		} else if (type == Type.NS) {
			return new String(data);
		} else if (type == Type.TXT) {
			return new String(data);
		} else {
			return new String("FERN DOES NOT SUPPORT THIS RECORD TYPE YET");
		}
	}

	public static String toDottedQuad(byte [] addr) {
		return ((addr[0] & 0xFF) + "." + (addr[1] & 0xFF) + "." +
			(addr[2] & 0xFF) + "." + (addr[3] & 0xFF));
	}
}