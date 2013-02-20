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

		// testScore();

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

		// if (args.length > 0) {
		// 	m.conf = args[0];
		// } else {
		// 	m.conf = "config/mdns.conf";
		// }

		// m.start();
		m.mdns.start();

		String line = new String("GROUP TOP 2 ucsc.global join 128.114.59.75 5301");
		CommandLineParser.readCommandLine(line, m.mdns);
		Request r = new Request("ccrg-server.ucsc");
		FERNObject o = m.mdns.resolveService(r);
		System.out.println("RESOLVED: " + o);
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
		readConf(conf);
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

	// readConf uses a conf file to carry ALL the group information! this is the only way
	// for us to "issue commands" to the headless operator
	public void readConf(String conffile) {
		FileInputStream fs;
		InputStreamReader isr;
		BufferedReader br;

		try {
			fs = new FileInputStream(conffile);
			isr = new InputStreamReader(fs);
			br = new BufferedReader(isr);
		}
		catch (Exception e) {
			System.err.println("daemon: could not open " + conffile);
			return;
		}

		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				if (!line.equals("")) {
					CommandLineParser.readCommandLine(line, mdns);					
				}
	        }
		} catch (IOException e) {
			// do nothing special here, we've already broken out of the loop
		}

		try {
			fs.close();
		} catch (IOException e) {
			// do nothing special here
		}
   		return;
	}

	public static void testName() {
		Name a = new Name("a.b.c");
		Name b = new Name("a.b.c");
		Name c = new Name("c.d.e");
		System.out.println("EQUALS: A.B.C == A.B.C ? " + a.equals(b));
		a.fernify();
		System.out.println("fernify a.b.c = " + a);
		a.unfern();
		System.out.println("unfern a.b.c.fern. = " + a);
		System.out.println("getParent a.b.c. = " + a.getParent());
		System.out.println("firstTerm a.b.c. = " + a.firstTerm());
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