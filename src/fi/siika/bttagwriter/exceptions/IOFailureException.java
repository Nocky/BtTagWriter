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
	private final IOException source;
	private final Step step;
	
	public IOFailureException(String msg) {
		this(msg, null, null);
	}
	
	public enum Step {
		READ, WRITE, FORMAT, CONNECT, CLOSE;
	};
	
	public IOFailureException(String msg, Step step) {
		this (msg, step, null);
	}
	
	public IOFailureException(String msg, IOException source) {
		this (msg, null, source);
	}
	
	public IOFailureException(String msg, Step step, IOException source) {
		super (msg);
		this.source = source;
		this.step = step;
	}
	
	public IOException getSource() {
		return source;
	}
	
	public Step getStep() {
		return this.step;
	}
	
	public String getLongMessage() {
		String ret = this.getMessage();
		if (source != null) {
			ret += ", source: " + source.getMessage();
		}
		return ret;
	}
}
