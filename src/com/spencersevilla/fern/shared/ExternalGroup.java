package com.spencersevilla.fern;

import java.util.*;
import java.net.*;

public class ExternalGroup extends FERNObject {
	public InetAddress addr;
	public int port;

	public ExternalGroup(Name n, InetAddress a, int p) {
		super(n);
		addRecord(ExternalGroup.generateRecord(n,a));
		addr = a;
		port = p;
	}

	public static Record generateRecord(Name n, InetAddress a) {
		byte[] rdata = a.getAddress();
		return new Record(n, Type.A, DClass.IN, 0, rdata);
	}

	public Response forwardRequest(Request request) {
		return InterGroupServer.resolveName(request, addr, 53);
	}
}