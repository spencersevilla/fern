package com.spencersevilla.fern;

import java.net.*;
import java.util.*;

public class Service {
	private Name name;
	private ArrayList<Record> recordSet;

	
	public Service(Name n) {
		name = n;
		recordSet = new ArrayList<Record>();

		return;
	}

	public Name getName() {
		return new Name(name);
	}

	public ArrayList<Record> getRecordSet() {
		return new ArrayList<Record>(recordSet);
	}

	public void addRecord(Record r) {
		if (r.getName() != null) {
			System.out.println("ERROR: record cannot have a name!");
			return;
		}

		recordSet.add(r);
	}

	public void removeRecord(Record r) {
		recordSet.remove(r);
	}

	public void generateRecord(InetAddress a) {
		if (a == null) {
			a = Service.generateAddress();
		}

		byte[] rdata = a.getAddress();
		Record r = new Record(null, Type.A, DClass.IN, 0, rdata);
		addRecord(r);
	}
	
	public static InetAddress generateAddress() {
		try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
	 																en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
															enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()
														&& inetAddress.isSiteLocalAddress()) {
	                	return inetAddress;
	                }
	            }
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}

		// couldn't find any address at all???
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	public String toString() {
		return name.toString();
	}
}