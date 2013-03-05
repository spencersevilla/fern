package com.spencersevilla.fern;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.*;

interface RMIServerIntf extends Remote {
    String getMessage() throws RemoteException;
    Response resolveService(Request request) throws RemoteException;
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

    public String getMessage() {
        return MESSAGE;
    }

    public Response resolveService(Request request) {
        return manager.resolveService(request);
    }
}