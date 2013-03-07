package com.spencersevilla.fern;

import java.util.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;

interface RMIServerIntf extends Remote {
    Response resolveRequest(Request request) throws RemoteException;
    Response register(Registration registration) throws RemoteException;

    int registerService(Service service) throws RemoteException;
    int removeService(Service service) throws RemoteException;

    int createGroup(Name group_name, int gid, ArrayList<String> args) throws RemoteException;
    int createSubGroup(Name parent_name, Name group_name, int gid, ArrayList<String> args) throws RemoteException;
    int leaveGroup(Name group_name) throws RemoteException;
}

class RMIServer extends UnicastRemoteObject implements RMIServerIntf {
    public static final String MESSAGE = "Hello world";
    public static final String RMI_NAME = "//localhost/fernd";

    FERNManager manager = null;
 
    public RMIServer(FERNManager m) throws RemoteException {
        manager = m;
    }
 
    public void start() {
        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(1099); 
        } catch (RemoteException e) {
            //do nothing, error means registry already exists
            System.out.println("RMIServer.start error: java RMI registry already exists!");
        }
 
        try {
            // Bind this object instance to the name "fernd"
            Naming.rebind(RMI_NAME, this);
        } catch (Exception e) {
            System.err.println("RMI server exception:" + e);
            e.printStackTrace();
        }

        System.out.println("Java RMI server started");
    }

    public Response resolveRequest(Request request) {
        return manager.resolveMessage(request);
    }

    public Response register(Registration registration) {
        return manager.resolveMessage(registration);
    }

    public int registerService(Service service) {
        return manager.registerService(service);
    }

    public int removeService(Service service) {
        return manager.removeService(service);
    }

    public int createGroup(Name group_name, int gid, ArrayList<String> args) {
        FERNGroup g = manager.createGroup(group_name, gid, args);

        if (g != null) {
            return 0;
        }
        return -1;
    }
    
    public int createSubGroup(Name parent_name, Name group_name, int gid, ArrayList<String> args) {
        FERNGroup parent = manager.findGroupByName(parent_name);
        if (parent == null) {
            return -1;
        }

        FERNGroup g = manager.createSubGroup(parent, group_name, gid, args);

        if (g != null) {
            return 0;
        }
        return -1;
    }
    
    public int leaveGroup(Name group_name) {
        FERNGroup group = manager.findGroupByName(group_name);
        if (group == null) {
            return -1;
        }

        return manager.leaveGroup(group);
    }
    
    // public int readCommandString(String string) {
    //     // find a way to return errors???
    //     CommandLineParser.readCommandLine(string, manager);
    //     return 0;
    // }

}