package com.spencersevilla.fern;

import java.util.*;

public class Response {
	private FERNObject object;
	private Request request;
	private ArrayList<FERNObject> otherEntries;
	public static FERNObject NULL_OBJECT = new FERNObject(new Name("NULL_OBJECT"));

	public Response(FERNObject o) {
		if (o == null) {
			o = NULL_OBJECT;
		}

		object = o;
		otherEntries = new ArrayList<FERNObject>();
	}

	public final String toString() {
		return object.toString();
	}

	public FERNObject getObject() {
		return object;
	}

	public void setRequest(Request r) {
		request = r;
	}

	public Request getRequest() {
		return request;
	}

	public void addOtherEntry(FERNObject o) {
		otherEntries.add(o);
	}

	public ArrayList<FERNObject> getOtherEntries() {
		return otherEntries;
	}

	public void pp() {
		System.out.println("Response.java pretty-print must be written!");
		return;
	}
}