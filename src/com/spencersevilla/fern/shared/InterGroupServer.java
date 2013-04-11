/*  This server is group-agnostic and serves one function: to pass "translate" requests
 *  when they must go to other nodes (such as using the ChordGroup). It is the primary
 *  public interface for a mdns server to request service-resolution from another computer.
 */

package com.spencersevilla.fern;

import java.io.*;
import java.net.*;
import java.util.*;

// import org.xbill.DNS.Message;
import org.xbill.DNS.Header;
import org.xbill.DNS.Section;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Flags;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.TextParseException;

public class InterGroupServer implements Runnable {

	public static final int port = 53;

	protected FERNManager mdns;
	private Thread thread;
    protected DatagramSocket socket;
	private boolean listening;
	private volatile boolean running;
	protected ArrayList<InterGroupThread> threads;
	protected ArrayList<InetAddress> dns_addrs;

	InterGroupServer(FERNManager m) throws Exception {
		mdns = m;
		threads = new ArrayList<InterGroupThread>();
		socket = new DatagramSocket(port);
		dns_addrs = new ArrayList<InetAddress>();
	}

	public void start() {
		running = true;
		System.out.println("IGS: serving on port " + port);

		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() throws Exception {
		// stop responding to new requests
		running = false;

		System.out.println("IGS: stopped");

		// abort all running requests
		for (Iterator<InterGroupThread> it = threads.iterator(); it.hasNext(); ) {
			InterGroupThread t = it.next();
			it.remove();
		}
	}

	public void addDNSServer(InetAddress addr) {
		if (addr == null) {
			return;
		}

		dns_addrs.add(addr);
	}

	public void run() {

        byte buf[];
		byte buf2[];
        DatagramPacket pack;
		DatagramPacket pack2;

        try {
			listening = true;

			while(listening) {
				buf = new byte[1024];
				pack = new DatagramPacket(buf, buf.length);
		        socket.receive(pack);

		        if (!running) {
		        	continue;
		        }

				InterGroupThread t = new InterGroupThread(this, pack);
				threads.add(t);
				t.start();
			}
        	socket.close();

 		} catch (Exception e) {
            e.printStackTrace();
        }
	}

	public static org.xbill.DNS.Name toDNSName(Name n) {
		try {
			String s = n.getName();

			if (!s.endsWith(".")) {
				s = s.concat(".");
			}
			return new org.xbill.DNS.Name(s);
			
		} catch (TextParseException e) {
			System.err.println("BIG ERROR: could not generate xbill.DNS.Name!");
			return null;
		}
	}
	
	public static Name fromDNSName(org.xbill.DNS.Name n) {
		return new Name(n.toString());
	}

	public static org.xbill.DNS.Record toDNSRecord(Record r) {
		return org.xbill.DNS.Record.newRecord(InterGroupServer.toDNSName(r.getName()), r.getType(), r.getDClass(), r.getTTL(), r.getData());
	}

	public static Record fromDNSRecord(org.xbill.DNS.Record r) {
		Name n = InterGroupServer.fromDNSName(r.getName());
		return new Record(n, r.getType(), r.getDClass(), r.getTTL(), r.rdataToWireCanonical());
	}

	public static Response sendMessage(Message message, InetAddress addr, int port) {
		try {
			byte[] sendbuf = generateDNSMessage(message);
			if (sendbuf == null) {
				System.err.println("IGS error: sendbuf is null");
				return null;
			}

			DatagramPacket sendpack = new DatagramPacket(sendbuf, sendbuf.length, addr, port);

			byte[] recbuf = new byte[1024];
			DatagramPacket recpack = new DatagramPacket(recbuf, recbuf.length);
			DatagramSocket sock = new DatagramSocket();
			sock.setSoTimeout(10000);

			System.out.println("IGS: sending message-name " + message + " to " + addr + ":" + port);

			sock.send(sendpack);

			// Timeout Breaks Here!
			sock.receive(recpack);

			org.xbill.DNS.Message response = new org.xbill.DNS.Message(recpack.getData());
			Response result = parseDNSResponse(response, message);

			if (result == null) {
				System.out.println("IGS: " + addr + ":" + port + " returned (null) for message: " + message);
			} else {
				System.out.println("IGS: " + addr + ":" + port + " returned answer for message: " + message);
			}

			return result;
		} catch (SocketTimeoutException e) {
			// timed out! :-(
			System.err.println("IGS error: socket timed out");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] generateDNSMessage(Message m) {
		if (m instanceof Request) {
			Request r = (Request) m;
			return generateRequest(r);
		} else if (m instanceof Registration) {
			Registration r = (Registration) m;
			return generateUpdate(r);
		}
		return null;
	}

	private static byte[] generateRequest(Request request) {
		Name name = request.getName();
		// name.fernify();
		org.xbill.DNS.Name n = InterGroupServer.toDNSName(name);

		org.xbill.DNS.Record query = org.xbill.DNS.Record.newRecord(n, Type.ANY, DClass.IN);
		org.xbill.DNS.Message message = org.xbill.DNS.Message.newQuery(query);
		return message.toWire();
	}

	private static byte[] generateUpdate(Registration registration) {
		ArrayList<Record> recordSet = registration.getRecordSet();
		if (recordSet.size() == 0) {
			// nothing to update/register?
			return null;
		}

		Name name = registration.getName();
		org.xbill.DNS.Name n = InterGroupServer.toDNSName(name);
		org.xbill.DNS.Message message = org.xbill.DNS.Message.newUpdate(n);

		for (Record r : recordSet) {
			org.xbill.DNS.Record dns_rec =  InterGroupServer.toDNSRecord(r);
			message.addRecord(dns_rec, Section.UPDATE);
		}

		return message.toWire();
	}

	private static Response parseDNSResponse(org.xbill.DNS.Message response, Message message) {
		if (message instanceof Request) {
			Request request = (Request) message;
			return parseRequestResponse(response, request);
		} else if (message instanceof Registration) {
			Registration reg = (Registration) message;
			return parseRegistrationResponse(response, reg);
		}
		return null;
	}

	private static Response parseRequestResponse(org.xbill.DNS.Message response, Request request) {
		Header header = response.getHeader();

		if (header.getRcode() == Rcode.NXDOMAIN) {
			// no error-alert here, this is a standard operation.
			return null;
		}

		if (header.getRcode() != Rcode.NOERROR) {
			System.out.println("IGS error: header Rcode is " + header.getRcode());
			return null;
		}

		if (!header.getFlag(Flags.QR)) {
			System.err.println("IGS error: response not a QR");
			return null;
		}

		org.xbill.DNS.Record question = response.getQuestion();
		// sanity check on question here...

		org.xbill.DNS.Record[] records = response.getSectionArray(Section.ANSWER);

		if (records.length == 0) {
			System.out.println("IGS error: empty record set!");
			return null;
		}

		return parseRecordSet(request, records);
	}

	private static Response parseRegistrationResponse(org.xbill.DNS.Message response, Registration request) {
		Header header = response.getHeader();

		if (header.getRcode() == Rcode.NXDOMAIN) {
			// no error-alert here, this is a standard operation.
			return null;
		}

		if (header.getRcode() != Rcode.NOERROR) {
			System.out.println("IGS error: header Rcode is " + header.getRcode());
			return null;
		}

		if (!header.getFlag(Flags.QR)) {
			System.err.println("IGS error: response not a QR");
			return null;
		}

		return null;
	}

	private static Response parseRecordSet(Request request, org.xbill.DNS.Record[] records) {
		// simplest alg: just return the first valid record...
		// org.xbill.DNS.Record r = records[0];
		// Name n = new Name(r.getName());
		// FERNObject object = new FERNObject(n);
		// object.addRecord(new Record(r));

		// return object;

		// FIRST we must sort the records into a set of FERNObjects
		ArrayList<FERNObject> objects = new ArrayList<FERNObject>();

outer:	for(int i = 0; i < records.length; i++) {
			Record rec = InterGroupServer.fromDNSRecord(records[i]);
inner:		for (FERNObject obj : objects) {
				if (rec.getName().equals(obj.getName())) {
					obj.addRecord(rec);
					continue outer;
				}
			}
			// looked through all existing objects and did't find
			// a match, so now we create a new object with this name.
			FERNObject obj = new FERNObject(rec.getName());
			obj.addRecord(rec);
			objects.add(obj);
		}

		// NEXT, we choose the best-match object to set as the response
		Response response = null;
		for (FERNObject obj : objects) {
			if (obj.isExactMatch(request)) {
				response = new Response(obj);
				break;
			}
		}

		if (response == null) {
			response = new Response(Response.NULL_OBJECT);
		}

		// LAST, we make sure to add the other objects and request
		response.setRequest(request);
		for (FERNObject obj : objects) {
			if (!obj.equals(response.getObject())) {
				response.addOtherEntry(obj);
			}
		}

		return response;
	}
}

class InterGroupThread extends Thread {

	static final int FLAG_DNSSECOK = 1;
	static final int FLAG_SIGONLY = 2;

	DatagramPacket inpacket = null;
    FERNManager mdns = null;
	ArrayList<InterGroupThread> array = null;
	DatagramSocket socket = null;
	InterGroupServer server = null;
	
    public InterGroupThread(InterGroupServer s, DatagramPacket p) {
    	server = s;
		inpacket = p;
        mdns = server.mdns;
		array = server.threads;
		socket = server.socket;
	}

	public void run() {
		try {
			byte[] response = generateResponse();

			if (response == null) {
				exit();
				return;
			}

			DatagramPacket outpacket = new DatagramPacket(response, response.length, 
											inpacket.getAddress(), inpacket.getPort());

			socket.send(outpacket);
		} catch (Exception e) {
			e.printStackTrace();
			exit();
		}
	}

	// this function borrows HEAVILY from jnamed.java's generateReply function!
    byte [] generateResponse() throws IOException {
    	org.xbill.DNS.Message query = new org.xbill.DNS.Message(inpacket.getData());
    	Header header = query.getHeader();
    	int maxLength = 0;
		int flags = 0;

    	// basic sanity checks
    	if (header.getFlag(Flags.QR)) {
    		System.err.println("IGS error: no QR flag?");
			return null;
		}
		if (header.getRcode() != Rcode.NOERROR) {
			System.err.println("IGS error: header.getRcode = " + header.getRcode());
			return errorMessage(query, Rcode.FORMERR);
		}

		if (header.getOpcode() == Opcode.QUERY) {
			return readQuery(query);
		} else if (header.getOpcode() == Opcode.UPDATE) {
			return readUpdate(query);
		} else {
			System.err.println("IGS error: header.getOpcode() = " + header.getOpcode());
			return errorMessage(query, Rcode.NOTIMP);
		}
	}

	byte[] readQuery(org.xbill.DNS.Message query) {
		int maxLength = 0;
		int flags = 0;

		if (shouldForward(query)) {
			System.err.println("IGS: queryRegularDNS()");
			return queryRegularDNS(query);
		}

		// parse out the question from the record
		org.xbill.DNS.Record queryRecord = query.getQuestion();
		System.out.println("IGS: received query for " + queryRecord.getName());

		OPTRecord queryOPT = query.getOPT();
		if (socket != null) {
			maxLength = 65535;
		} else if (queryOPT != null) {
			maxLength = Math.max(queryOPT.getPayloadSize(), 512);
		} else {
			maxLength = 512;
		}

		if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0)
			flags = FLAG_DNSSECOK;

		// prep the response with DNS data
		org.xbill.DNS.Message response = new org.xbill.DNS.Message(query.getHeader().getID());
		response.getHeader().setFlag(Flags.QR);
		if (query.getHeader().getFlag(Flags.RD)) {
			response.getHeader().setFlag(Flags.RD);
		}
		response.addRecord(queryRecord, Section.QUESTION);

    	// OKAY! now we've got a DNS Record question here. It should contain a 
    	// name, type, class, ttl, and rdata. Now we should parse the request
    	// and then execute one of four potential paths:
    	// 		1) forward the request to the next server along the way 
    	//		2) return next server's addr (non-recursive #1)
    	//		3) respond in the positive (found the node)
    	//		4) respond in the negative (end-of-the-line)

		byte rcode = generateAnswer(query, response, flags);

		if (rcode != Rcode.NOERROR) {
			return errorMessage(query, rcode);
		}

		// addAdditional?

		// response.setTSIG(null, Rcode.NOERROR, null);
		return response.toWire(maxLength);
	}

	byte[] readUpdate(org.xbill.DNS.Message update) {
		int maxLength = 0;
		int flags = 0;

		// ZONE section is same as QUERY section so this works the same
		org.xbill.DNS.Record zoneRecord = update.getQuestion();
		System.out.println("IGS: received update message for zone " + zoneRecord.getName());
		Name name = new Name(zoneRecord.getName().toString());

		OPTRecord queryOPT = update.getOPT();
		if (socket != null) {
			maxLength = 65535;
		} else if (queryOPT != null) {
			maxLength = Math.max(queryOPT.getPayloadSize(), 512);
		} else {
			maxLength = 512;
		}

		if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0)
			flags = FLAG_DNSSECOK;

		// prep the response with DNS data and formatting
		org.xbill.DNS.Message response = new org.xbill.DNS.Message(update.getHeader().getID());
		response.getHeader().setOpcode(Opcode.UPDATE);
		response.getHeader().setFlag(Flags.QR);

		// per the RFC, we leave all content-fields as zero.
		// All that's left to set is just the Rcode!


		// somehow generate a Registration message here
		Registration reg = new Registration(name);
		org.xbill.DNS.Record[] records = update.getSectionArray(Section.UPDATE);
		for (org.xbill.DNS.Record dnsrec : records) {
			Record fernrec = InterGroupServer.fromDNSRecord(dnsrec);
			reg.addRecord(fernrec);
		}

		// run it through our system to get an answer!
		byte rcode = Rcode.NOERROR;
		Response resp = mdns.resolveMessage(reg);

		if (resp == null) {
			return errorMessage(update, Rcode.NXDOMAIN);
		}

		if (resp.getRetVal() != Rcode.NOERROR) {
			return errorMessage(update, resp.getRetVal());
		}

		response.getHeader().setRcode(Rcode.NOERROR);
		return response.toWire(maxLength);
	}

	byte generateAnswer(org.xbill.DNS.Message query, org.xbill.DNS.Message response, int flags) {
		org.xbill.DNS.Record queryRecord = query.getQuestion();
		Name name = new Name(queryRecord.getName().toString());
		Request request = new Request(name);

		int type = queryRecord.getType();
		int dclass = queryRecord.getDClass();
		byte rcode = Rcode.NOERROR;
		int ttl = 0;

		if (dclass != DClass.IN) {
			return Rcode.NOTIMP;
		}

		Response resp = mdns.resolveMessage(request);

		if (resp == null) {
			return Rcode.NXDOMAIN;
		}
		// resp.pp();

		if (resp.getObject().equals(FERNObject.NO_MATCH)) {
			return Rcode.NXDOMAIN;
		}

		for (Record fern_rec : resp.getObject().getRecordSet()) {
			org.xbill.DNS.Record rec = InterGroupServer.toDNSRecord(fern_rec);
			RRset rset = new RRset(rec);



			addRRset(InterGroupServer.toDNSName(resp.getObject().name), response, rset, Section.ANSWER, flags);
		}

		for (FERNObject obj : resp.getOtherEntries()) {
			for (Record fern_rec : obj.getRecordSet()) {
				org.xbill.DNS.Record rec = InterGroupServer.toDNSRecord(fern_rec);
				RRset rset = new RRset(rec);
				addRRset(InterGroupServer.toDNSName(obj.name), response, rset, Section.ANSWER, flags);
			}
		}

		return Rcode.NOERROR;

		// if (type == Type.A) {
		// } else if (type == Type.CNAME) {
		// } else if (type == Type.NS) {
		// }
	}

	boolean shouldForward(org.xbill.DNS.Message query) {
		org.xbill.DNS.Record queryRecord = query.getQuestion();

		if (queryRecord == null) {
			return true;
		}

		String nameString = queryRecord.getName().toString();
		int type = queryRecord.getType();

		// check for our TLD suffix
		// if (!nameString.endsWith("fern.") && !nameString.endsWith("fern")) {
		// 	return true;
		// }

		// we've sanity-checked this record and it's okay for us to handle it!
		// (it is an A record-query belonging to the .fern. TLD)
		return false;
	}

	// this function forwards the query onto an ACTUAL DNS server and then just
	// returns whatever message it's given
	byte [] queryRegularDNS(org.xbill.DNS.Message query) {
		if (server.dns_addrs.isEmpty()) {
			return null;
		}

		InetAddress dns_server = server.dns_addrs.get(0);
		if (dns_server == null) {
			return null;
		}

		try {
			DatagramSocket sock = new DatagramSocket();
			sock.setSoTimeout(10000);

			byte[] sendbuf = query.toWire();
			byte[] recbuf = new byte[1024];

			DatagramPacket sendpack = new DatagramPacket(sendbuf, sendbuf.length, dns_server, 53);
			DatagramPacket recpack = new DatagramPacket(recbuf, recbuf.length);

			sock.send(sendpack);
			sock.receive(recpack);

			byte[] retval = new byte[recpack.getLength()];
			retval = Arrays.copyOf(recbuf, recpack.getLength());

			return retval;
		} catch (SocketTimeoutException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	byte [] errorMessage(org.xbill.DNS.Message query, int rcode) {	
		return buildErrorMessage(query.getHeader(), rcode,
					 query.getQuestion());
	}

	byte [] buildErrorMessage(Header header, int rcode, org.xbill.DNS.Record question) {
		org.xbill.DNS.Message response = new org.xbill.DNS.Message();
		response.setHeader(header);
		for (int i = 0; i < 4; i++)
			response.removeAllRecords(i);
		if (rcode == Rcode.SERVFAIL)
			response.addRecord(question, Section.QUESTION);
		header.setRcode(rcode);
		return response.toWire();
	}

	void addRRset(org.xbill.DNS.Name name, org.xbill.DNS.Message response, RRset rrset, int section, int flags) {
		for (int s = 1; s <= section; s++)
			if (response.findRRset(name, rrset.getType(), s))
				return;
		if ((flags & FLAG_SIGONLY) == 0) {
			Iterator it = rrset.rrs();
			while (it.hasNext()) {
				org.xbill.DNS.Record r = (org.xbill.DNS.Record) it.next();
				if (r.getName().isWild() && !name.isWild())
					r = r.withName(name);
				response.addRecord(r, section);
			}
		}
		if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
			Iterator it = rrset.sigs();
			while (it.hasNext()) {
				org.xbill.DNS.Record r = (org.xbill.DNS.Record) it.next();
				if (r.getName().isWild() && !name.isWild())
					r = r.withName(name);
				response.addRecord(r, section);
			}
		}
	}

	public void exit() {
		array.remove(this);
	}
}