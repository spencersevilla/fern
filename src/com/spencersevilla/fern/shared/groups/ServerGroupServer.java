package com.spencersevilla.fern.groups;
import com.spencersevilla.fern.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerGroupServer extends ServerGroup implements Runnable {
	private HashMap<Name, FERNObject> objects;
	private HashMap<Name, FERNObject> cacheList;

	public ServerGroupServer(FERNManager m, Name n, InetAddress a, int p) {
			super(m, n, a, p);
			objects = new HashMap<Name, FERNObject>();
			cacheList = new HashMap<Name, FERNObject>();
	}

	public void start() {
		thread = new Thread(this);
		thread.start();
		System.out.println("SGServer " + name + ": started.");
	}

	public void stop() {
		System.out.println("SGServer " + name + ": stopped.");
		return;
	}

	public Response parseMessageFinal(Message message) {
		// here we know that this group is the final-destination!
		if (message instanceof Request) {
			return parseRequest((Request) message);
		} else if (message instanceof Registration) {
			return parseRegistration((Registration) message);
		} else {
			System.out.println("DEBUG: MESSAGE TYPE???");
			return null;
		}
	}

	public Response parseMessageOther(Message message) {

		FERNObject initialObject = getNextHop(message);

		// Since the tree is deterministic, we already know the next hop. Now
		// let's go fishing in the cache and see if we can beat it!
		FERNObject obj = findBestMatch(message, new ArrayList<FERNObject>(cacheList.values()), initialObject);

		// if this is an exact match AND we allow it, we can respond directly
		if (message.allowCacheResponse() && obj.isExactMatch(message)) {
			Response r = new Response(obj);
			r.setRequest(message);
			return r;
		}

		// can't respond directly, so let's forward the message onwards.
		return obj.forwardMessage(message);
	}

	public FERNObject getNextHop(Message message) {
		Name key = findNextHop(message);
		if (key == null) {
			System.out.println("SGServer " + name + " error: invalid findNextHop for message " + message);
			return null;
		}

		FERNObject o = objects.get(key);

		// Object not found
		if (o == null) {
			System.out.println("SGServer " + name + " ERROR: no result for " + key);
		}
		return o;
	}


	public Response parseRequest(Request request) {
		Name key = findNextHop(request);
		if (key == null) {
			System.out.println("SGServer " + name + " error: invalid findNextHop for request " + request);
			return null;
		}

		FERNObject o = objects.get(key);

		// Object not found
		if (o == null) {
			System.out.println("SGServer " + name + ": no result for " + key);
			o = FERNObject.NO_MATCH;
		}

		// Now we're guaranteed to have an object!
		Response resp = new Response(o);
		resp.setRequest(request);
		return resp;
	}

	public Response parseRegistration(Registration reg) {
		System.out.println("DEBUG: PARSE REGISTRATION");
		// Go through each record in turn. If it's one of ours, process it
		// accordingly. If not, we can still add it to the cache!

		for (Record r : reg.getRecordSet()) {
			Name recname = r.getName();
			Name pname = recname.getParent();


			if (pname.equals(name)) {
				// this record is a direct child of this group
				addChildRecord(r);
			} else {
				// this record is NOT a child, so we can cache it
				addCacheRecord(r);
			}
		}

		return Registration.successfulResponse(reg);
	}

	private void addChildRecord(Record r) {
		Name key = r.getName().firstTerm();

		FERNObject object = objects.get(key);
		if (object != null) {
				object.addRecord(r);
		} else {
			object = new FERNObject(r.getName());
			object.addRecord(r);
			objects.put(key, object);
		}
		return;
	}

	private void addCacheRecord(Record r) {

		FERNObject object = objects.get(r.getName());
		if (object != null) {
				object.addRecord(r);
		} else {
			object = new FERNObject(r.getName());
			object.addRecord(r);
			objects.put(r.getName(), object);
		}
		return;
	}

	public void registerObject(FERNObject object) {
		System.out.println("SGServer " + name + " server: registering object " + object);
		if (!object.getName().getParent().equals(name)) {
			System.err.println("SGServer " + name + " error: " + object + "is not a child of " + name);
			return;
		}

		Name key = object.getName().firstTerm();

		if (objects.get(key) != null) {
			System.err.println("SGServer " + name + " error: there already exists a value for " + key);
			return;
		}

		objects.put(key, object);
		return;
	}

	public void removeObject(FERNObject object) {
		if (!object.getName().getParent().equals(name)) {
			System.err.println("SGServer " + name + " error: " + object + "is not a child of " + name);
			return;
		}

		Name key = object.getName().firstTerm();

		System.out.println("SGServer " + name + ": removing object " + object);
		objects.remove(key);
		return;
	}

	// public FERNObject resolveName(Request request) {
	// 	// this generates a child or parent name of the group-in-question
	// 	Name key = findNextHop(request);
	// 	if (key == null) {
	// 		System.out.println("SG " + name + " error: invalid findNextHop?");
	// 		return null;
	// 	}

	// 	FERNObject o = objects.get(key);

	// 	// Object not found
	// 	if (o == null) {
	// 		System.out.println("SG " + name + ": no result for " + key);
	// 		return FERNObject.NO_MATCH;
	// 	}

	// 	// Object found!
	// 	return o;
	// }

	public void run() {
		try {
			ServerSocket listenSock = new ServerSocket(port);

			while(true) {

				Socket connSock = listenSock.accept();

				ObjectInputStream ois = new ObjectInputStream(connSock.getInputStream());
				// ObjectOutputStream oos = new ObjectOutputStream(connSock.getOutputStream());

				String command = (String) ois.readObject();
				FERNObject object = (FERNObject) ois.readObject();

				if (command.equals(ServerGroup.REGISTER)) {
					registerObject(object);
				} else if (command.equals(ServerGroup.REMOVE)) {
					removeObject(object);
				} else {
					System.err.println("SGServer error: received command " + command);
				}
				// Message response = generateResponse(request);

				// oos.writeObject(response);

				ois.close();
				// oos.close();
				connSock.close();
			}
 		} catch (Exception e) {
            e.printStackTrace();
        }
	}
}