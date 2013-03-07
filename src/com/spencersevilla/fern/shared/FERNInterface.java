package com.spencersevilla.fern;

import java.util.*;
import java.rmi.Naming;

/* This is our publicly-exposed API! Functions here can be called
 * from ANY other application that wishes to interact with FERN.
 *
 *  PUBLIC API IS AS FOLLOWS:
 *  FERNInterface() throws Exception;
 *  Response resolveRequest(Request request);
 *  Response register(Registration registration);
 *  int registerService(Service service);
 *  int removeService(Service service);
 *  int createGroup(Name group_name, int gid, ArrayList<String> args);
 *  int createSubGroup(Name parent_name, Name group_name, int gid, ArrayList<String> args);
 *  int leaveGroup(Name group_name);
 */

public class FERNInterface {
    // "obj" is the reference of the remote object using JavaRMI
    RMIServerIntf obj = null; 

	public FERNInterface() throws Exception {
		obj = (RMIServerIntf) Naming.lookup(RMIServer.RMI_NAME);
    }

    public Response resolveRequest(Request request) {
        try {
            return obj.resolveRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Response register(Registration registration) {
        try {
            return obj.register(registration);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int registerService(Service service) {
        try {
            return obj.registerService(service);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int removeService(Service service) {
        try {
            return obj.removeService(service);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int createGroup(Name group_name, int gid, ArrayList<String> args) {
        try {
            return obj.createGroup(group_name, gid, args);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int createSubGroup(Name parent_name, Name group_name, int gid, ArrayList<String> args) {
        try {
            return obj.createSubGroup(parent_name, group_name, gid, args);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int leaveGroup(Name group_name) {
        try {
            return obj.leaveGroup(group_name);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

 //    public static void main(String[] args) throws Exception {
	// 	FERNInterface i = new FERNInterface();

 //        String line = new String("GROUP TOP 2 global create 5301");
 //        String line2 = new String("SERVICE laptop");
 //        Request r = new Request("laptop.global");

 //        i.readCommandString(line);
 //        i.readCommandString(line2);

 //        Response resp = i.resolveRequest(r);
        
 //        if (resp != null) {
 //            System.out.println("RESOLVED REQUEST: " + resp);
 //            resp.pp();
 //        } else {
 //            System.out.println("COULD NOT RESOLVE REQUEST: " + resp);
 //        }
	// }
}