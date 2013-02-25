package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Record {
	public static final String HEADER = String.format("%-25s%-10s%-10s%-10s%-10s", "NAME", "TYPE", "CLASS", "TTL", "DATA");

	protected Name name;
	protected int type, dclass;
	protected long ttl;
	protected byte[] data;

	private final String TAB = new String("\t");

	public Record(org.xbill.DNS.Record r) {
		name = new Name(r.getName());
		type = r.getType();
		dclass = r.getDClass();
		ttl = r.getTTL();
		data = r.rdataToWireCanonical();
	}

	public Record(Name n, int t, int d, long tt, byte[] dat) {
		name = n;
		type = t;
		dclass = d;
		ttl = tt;
		data = dat;
	}

	public Record(Record r) {
		name = r.name;
		type = r.type;
		dclass = r.dclass;
		ttl = r.ttl;
		data = r.data;
	}

	public String toString() {
		return String.format("%-25s%-10s%-10s%-10s%-10s", name, Type.toString(type), DClass.toString(dclass), ttl, dataString());
	}

	private String dataString() {
		if (type == Type.A) {
			return org.xbill.DNS.Address.toDottedQuad(data);
		} else if (type == Type.NS) {
			return new String(data);
		} else if (type == Type.TXT) {
			return new String(data);
		} else {
			return new String("FERN DOES NOT SUPPORT THIS RECORD TYPE YET");
		}
	}

	public org.xbill.DNS.Record toDNSRecord(){
		return org.xbill.DNS.Record.newRecord(name.toDNSName(), type, dclass, ttl, data);
	}
}