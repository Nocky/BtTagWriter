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
	
	private static final long serialVersionUID = 1L;
	
	public OutOfSpaceException (String msg) {
		super (msg);
	}
	
}
