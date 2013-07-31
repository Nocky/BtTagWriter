/**
 * TagType.java (bttagwriter)
 *
 * Copyright 2012 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.data;

/**
 * Different types of tags this application can write
 */
public enum TagType {
    /**
     * Simplified (smaller) format, works with Android
     */
    SIMPLIFIED,
    
    /**
     * Standard handover type tag (better compatibility)
     */
    HANDOVER;
}
