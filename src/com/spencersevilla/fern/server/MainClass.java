package com.spencersevilla.fern.server;
import com.spencersevilla.fern.*;

import org.apache.commons.daemon.*;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;

import java.io.*;
import java.util.*;
import java.net.InetAddress;

public class MainClass implements Daemon, DaemonUserSignal {
	protected FERNManager mdns;
	// protected SwingGui gui;

	private String conf;

	public static void main(String[] args) throws Exception {

		if (args.length > 1) {
			System.out.println("usage: MainClass [conf_file]");
			System.exit(0);
		}

		MainClass m = new MainClass();
		m.mdns = new FERNManager();
		// m.gui = new SwingGui(m.mdns);

		if (m.mdns == null) {
			System.out.println("error: could not initialize mdns!");
			System.exit(0);
		}

		m.mdns.start();

		// CommandLineParser.readCommandLine(line, m.mdns);
		// CommandLineParser.readCommandLine(line2, m.mdns);
		// Request r = new Request("ucsc.");
		// Response resp = m.mdns.resolveService(r);
		// if (resp != null) {
		// 	System.out.println("RESOLVED REQUEST: " + resp);
		// 	resp.pp();
		// } else {
		// 	System.out.println("COULD NOT RESOLVE REQUEST: " + resp);
		// }
	}

	public MainClass() throws Exception {
	}
	
	// Java Daemon interface here! ============================================

	public void init(DaemonContext context) throws DaemonInitException, Exception {
		mdns = new FERNManager();

		if (mdns == null) {
			throw new DaemonInitException("error: could not initialize mdns!");
		}

		String[] args = context.getArguments();

		if (args.length > 0) {
			// they supplied a custom name/path so use that instead
			conf = args[0];
		} else {
			// default: just uses this filename with respect to $CLASSPATH
			conf = "config/mdns.conf";
		}

		System.out.println("daemon: initialized.");
	}

	public void start() throws Exception {
		de.uniba.wiai.lspi.chord.service.PropertiesLoader.loadPropertyFile();
		mdns.start();
		// readConf(conf);
		System.out.println("daemon: started.");
	}

	public void stop() throws Exception {
		mdns.stop();
		System.out.println("daemon: stopped.");
	}

	public void destroy() {
		mdns.exit();
	}

	public void signal() {
		System.out.println("signal called!");
	}

	public static void testScore() {
		System.out.println("SCORE TESTER::");
		Request req = new Request("ccrg-server.ucsc");
		Name gname = new Name("global");
		FERNObject obj = new FERNObject(gname);

		Name gname2 = new Name("ucsc.global");
		FERNObject obj2 = new FERNObject(gname2);

		ArrayList<FERNObject> ar = new ArrayList<FERNObject>();
		ar.add(obj);
		ar.add(obj2);

		FERNObject o = FERNObject.findBestMatch(req, ar, null);
		System.out.println("object: " + o);
	}
}