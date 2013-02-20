// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package com.spencersevilla.fern;

/**
 * Constants and functions relating to DNS classes.  This is called DClass
 * to avoid confusion with Class.
 *
 * @author Brian Wellington
 */

public final class DClass {

	/** Internet */
	public static final int IN		= 1;

	/** Chaos network (MIT) */
	public static final int CH		= 3;

	/** Chaos network (MIT, alternate name) */
	public static final int CHAOS		= 3;

	/** Hesiod name server (MIT) */
	public static final int HS		= 4;

	/** Hesiod name server (MIT, alternate name) */
	public static final int HESIOD		= 4;

	/** Special value used in dynamic update messages */
	public static final int NONE		= 254;

	/** Matches any class */
	public static final int ANY		= 255;
}
