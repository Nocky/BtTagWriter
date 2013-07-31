/*
 * WriteError.java (BT Tag Writer)
 *
 * https://github.com/alump/BtTagWriter
 *
 * Copyright 2011-2013 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter.writers;

/**
 * Writing errors
 */
public enum WriteError {
    /**
     * Write was cancelled
     */
    CANCELLED,
    /**
     * Connection to tag was lost
     */
    CONNECTION_LOST,
    /**
     * Failed to format tag
     */
    FAILED_TO_FORMAT,
    /**
     * Too small tag for data
     */
    TOO_SMALL,
    /**
     * Tag not approved
     */
    TAG_NOT_ACCEPTED,
    /**
     * General write error
     */
    FAILED_TO_WRITE,
    /**
     * Tag is write protected
     */
    WRITE_PROTECTED,
    /**
     * General system error (software failure)
     */
    SYSTEM_ERROR;
}
