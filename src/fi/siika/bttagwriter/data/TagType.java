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
     * Simple format that needs BtTagWriter to read and pair
     */
    TAGWRITER,
    
    /**
     * Standard handover type tag (works with Android 4.1+ without need to
     * have have this application installed).
     */
    HANDOVER;
}
