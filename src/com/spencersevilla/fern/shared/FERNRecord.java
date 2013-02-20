package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class FERNRecord {
	protected Name name;
	protected int type, dclass;
	protected long ttl;
	protected byte[] data;

	private final String TAB = new String("\t");

	public FERNRecord(org.xbill.DNS.Record r) {
		name = new Name(r.getName());
		type = r.getType();
		dclass = r.getDClass();
		ttl = r.getTTL();
		data = r.rdataToWireCanonical();
	}

	public FERNRecord(Name n, int t, int d, int tt, byte[] dat) {
		name = n;
		type = t;
		dclass = d;
		ttl = tt;
		data = dat;
	}

	public String toString() {
		return new String(name + TAB + type + TAB + dclass + TAB + ttl + TAB + dataString());
	}

	private String dataString() {
		if (type == Type.A) {
			return org.xbill.DNS.Address.toDottedQuad(data);
		} else {
			return null;
		}
	}

	public org.xbill.DNS.Record toDNSRecord(){
		return org.xbill.DNS.Record.newRecord(name.toDNSName(), type, dclass, ttl, data);
	}
}