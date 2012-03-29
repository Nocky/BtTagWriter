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
	public boolean readOnly = false;
	
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
}
