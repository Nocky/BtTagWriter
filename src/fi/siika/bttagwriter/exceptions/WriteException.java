package fi.siika.bttagwriter.exceptions;

import fi.siika.bttagwriter.writers.WriteError;

/**
 * Exception used when write fails
 */
public class WriteException extends Exception {
    private final WriteError errorCode;
    private final Exception source;

    public WriteException(WriteError error) {
        this(error, null, "Tag write failed.");
    }

    public WriteException(WriteError error, String message) {
        this(error, null, message);
    }

    public WriteException(WriteError error, Exception source, String message) {
        super(message);
        this.source = source;
        errorCode = error;
    }

    public WriteError getErrorCode() {
        return errorCode;
    }

    public Exception getSource() {
        return source;
    }
}
