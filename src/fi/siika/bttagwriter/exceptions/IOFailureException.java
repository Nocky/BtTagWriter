/*
 * IOFailureException.java (BT Tag Writer)
 *
 * https://github.com/alump/BtTagWriter
 *
 * Copyright 2011-2013 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.exceptions;

import java.io.IOException;

import fi.siika.bttagwriter.writers.WriteError;

/**
 *
 */
public class IOFailureException extends WriteException {

    @Deprecated
    public IOFailureException(String msg) {
        this(msg, null, null);
    }

    @Deprecated
    public enum Step {
        READ, WRITE, FORMAT, CONNECT, CLOSE;
    }

    ;

    @Deprecated
    public IOFailureException(String msg, Step step) {
        this(msg, step, null);
    }

    public IOFailureException(String msg, IOException source) {
        this(msg, null, source);
    }

    public IOFailureException(WriteError errorCode, IOException source, String msg) {
        super(errorCode, source, msg);
    }

    @Deprecated
    public IOFailureException(String msg, Step step, IOException source) {
        super(WriteError.FAILED_TO_WRITE, source, msg);
    }

    public IOException getSource() {
        return (IOException) super.getSource();
    }

    @Deprecated
    public Step getStep() {
        //return this.step;
        return null;
    }

    public String getLongMessage() {
        String ret = this.getMessage();
        if (getSource() != null) {
            ret += ", source: " + getSource().getMessage();
        }
        return ret;
    }
}
