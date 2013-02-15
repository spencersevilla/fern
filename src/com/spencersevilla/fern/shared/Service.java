package com.spencersevilla.fern;

import java.net.*;
import java.util.Enumeration;

public class Service {
	public Name name;
	public int port;
	public InetAddress addr;
	
	public Service(Name n, int p, InetAddress a) {
		name = n;
		port = p;
		addr = a;
		
		if (addr == null) {
			addr = Service.generateAddress();
		}

		return;
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