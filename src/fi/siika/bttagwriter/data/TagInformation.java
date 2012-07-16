/**
 * TagInformation.java (bttagwriter)
 *
 * Copyright 2012 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.data;

/**
 * Data class for information stored to tags
 */
public class TagInformation implements Cloneable {
	/**
	 * Bluetooth address of device ("00:00:00:00:00:00" format)
	 */
	public String address;
	
	/**
	 * Bluetooth name of device
	 */
	public String name;
	
	/**
	 * If true writer will try to write protected the tag
	 */
	private boolean readOnly = false;
	
	/**
	 * Format used to write information
	 */
	private TagType type = TagType.TAGWRITER;
	
	/**
	 * Pin code or if empty no pin code
	 */
	public String pin;
	
	@Override
	public Object clone () {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public TagType getType() {
	    return type;
	}
	
	public void setType(TagType type) {
	    this.type = type;
	}
	
	public boolean isReadOnly() {
	    return readOnly;
	}
	
	public void setReadOnly (boolean readonly) {
	    readOnly = readonly;
	}
}
