package com.spencersevilla.mdns;

import java.net.*;
import java.util.Enumeration;

public class Service {
	public String name;
	public int port;
	public String addr;
	
	public Service(String n, int p) {
		name = n;
		port = p;
		addr = "0.0.0.0";
		
		try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
	 																en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
															enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()
														&& inetAddress.isSiteLocalAddress()) {
	                	addr = inetAddress.getHostAddress();
						return;
	                }
	            }
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return;
	}
	
	public String toString() {
		return name;
	}
}