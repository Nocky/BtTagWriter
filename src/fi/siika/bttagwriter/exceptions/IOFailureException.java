/**
 * ConnectionFailedException.java (bttagwriter)
 *
 * Copyright 2012 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.exceptions;

import java.io.IOException;

/**
 * 
 */
public class IOFailureException extends Exception {
	private IOException source;
	
	public IOFailureException(String msg) {
		super (msg);
	}
	
	public IOFailureException(String msg, IOException source) {
		this (msg);
		this.source = source;
	}
	
	public IOException getSource() {
		return source;
	}
}
