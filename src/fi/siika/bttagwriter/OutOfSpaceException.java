/**
 * OutOfSpaceException.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

/**
 * Exception used when tag does not have enough space for content
 */
public class OutOfSpaceException extends Exception {
	
	public OutOfSpaceException (String msg) {
		super (msg);
	}
	
	private static final long serialVersionUID = 1L;
	
}
