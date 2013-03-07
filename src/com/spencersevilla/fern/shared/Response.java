package com.spencersevilla.fern;

import java.util.*;
import java.io.*;

public class Response implements Serializable {
	private int retval;
	private FERNObject object;
	private Message request;
	private ArrayList<FERNObject> otherEntries;
	public static FERNObject NULL_OBJECT = new FERNObject(new Name("NULL_OBJECT"));

	public Response(FERNObject o) {
		if (o == null) {
			o = NULL_OBJECT;
		}

		object = new FERNObject(o);
		otherEntries = new ArrayList<FERNObject>();
	}

	public final String toString() {
		return object.toString();
	}

	public FERNObject getObject() {
		return object;
	}

	public void setRequest(Message r) {
		request = r;
	}

	public Message getRequest() {
		return request;
	}

	public void addOtherEntry(FERNObject o) {
		otherEntries.add(new FERNObject(o));
	}

	public ArrayList<FERNObject> getOtherEntries() {
		return otherEntries;
	}

	public void pp() {
		System.out.println("Request: " + request);
		System.out.println("Returned Object Name: " + object);
		System.out.println("Records: ");
		System.out.println(Record.HEADER);
		for (Record r : object.getRecordSet()) {
			System.out.println(r);
		}
		for (FERNObject o : otherEntries) {
			for (Record r : o.getRecordSet()) {
				System.out.println(r);
			}
		}
		
		return;
	}
}