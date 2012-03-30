/**
 * OutOfSpaceException.java (bttagwriter)
 *
 * Copyright 2012 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.exceptions;

/**
 * 
 */
public class OutOfSpaceException extends Exception {
	
	private final Exception source;
	
	public OutOfSpaceException (String msg, Exception source) {
		super (msg);
		this.source = source;
	}
	
	public OutOfSpaceException (String msg) {
        this(msg, null);
    }
	
	public Exception getSource() {
	    return source;
	}
}
