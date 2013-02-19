package com.spencersevilla.fern;

import java.util.*;
import java.net.*;

import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import org.xbill.DNS.DClass;

public class ExternalGroup extends FERNObject {
	public InetAddress addr;
	public int port;

	public ExternalGroup(Name n, InetAddress a, int p) {
		super(n, ExternalGroup.generateRecord(n,a));
		addr = a;
		port = p;
	}

	public static Record generateRecord(Name n, InetAddress a) {
		byte[] rdata = a.getAddress();
		return Record.newRecord(n.toDNSName(), Type.A, DClass.IN, 0, rdata);
	}

	public FERNObject forwardRequest(Request request) {
		return InterGroupServer.resolveName(request, addr, 53);
	}
}