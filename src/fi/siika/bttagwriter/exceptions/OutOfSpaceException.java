/**
 * OutOfSpaceException.java (bttagwriter)
 *
 * Copyright 2012 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.exceptions;

import fi.siika.bttagwriter.writers.WriteError;

/**
 * 
 */
public class OutOfSpaceException extends WriteException {

	public OutOfSpaceException (String msg, Exception source) {
		super (WriteError.TOO_SMALL, source, msg);
	}

	public OutOfSpaceException (String msg) {
        this(msg, null);
    }
}
