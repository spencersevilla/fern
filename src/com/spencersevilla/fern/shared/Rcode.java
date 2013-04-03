// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package com.spencersevilla.fern;

/**
 * Constants and functions relating to DNS rcodes (error values)
 *
 * @author Brian Wellington
 */

public final class Rcode {

/** No error */
public static final int NOERROR		= 0;

/** Format error */
public static final int FORMERR		= 1;

/** Server failure */
public static final int SERVFAIL	= 2;

/** The name does not exist */
public static final int NXDOMAIN	= 3;

/** The operation requested is not implemented */
public static final int NOTIMP		= 4;

/** Deprecated synonym for NOTIMP. */
public static final int NOTIMPL		= 4;

/** The operation was refused by the server */
public static final int REFUSED		= 5;

/** The name exists */
public static final int YXDOMAIN	= 6;

/** The RRset (name, type) exists */
public static final int YXRRSET		= 7;

/** The RRset (name, type) does not exist */
public static final int NXRRSET		= 8;

/** The requestor is not authorized to perform this operation */
public static final int NOTAUTH		= 9;

/** The zone specified is not a zone */
public static final int NOTZONE		= 10;

/* EDNS extended rcodes */
/** Unsupported EDNS level */
public static final int BADVERS		= 16;

/* TSIG/TKEY only rcodes */
/** The signature is invalid (TSIG/TKEY extended error) */
public static final int BADSIG		= 16;

/** The key is invalid (TSIG/TKEY extended error) */
public static final int BADKEY		= 17;

/** The time is out of range (TSIG/TKEY extended error) */
public static final int BADTIME		= 18;

/** The mode is invalid (TKEY extended error) */
public static final int BADMODE		= 19;

private
Rcode() {}

}
