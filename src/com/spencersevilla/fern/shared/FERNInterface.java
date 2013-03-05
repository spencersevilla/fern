package com.spencersevilla.fern;

import java.rmi.Naming;

public class FERNInterface {
    // "obj" is the reference of the remote object
    RMIServerIntf obj = null; 

	public FERNInterface() throws Exception {
		obj = (RMIServerIntf) Naming.lookup(RMIServer.RMI_NAME);
		// return obj.getMessage(); 
    }

    public String getMessage() {
    	try {
			return obj.getMessage();
    	} catch (Exception e) {
    		e.printStackTrace();
    		return null;
    	}
    }

    public Response resolveService(Request request) {
        try {
            return obj.resolveService(request);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
		FERNInterface i = new FERNInterface();

        Request r = new Request("laptop.global");
        Response resp = i.resolveService(r);
        
        if (resp != null) {
            System.out.println("RESOLVED REQUEST: " + resp);
            resp.pp();
        } else {
            System.out.println("COULD NOT RESOLVE REQUEST: " + resp);
        }
	}
}