/**
 * TagTechWriter.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.writers;

import java.io.UnsupportedEncodingException;

import android.nfc.NdefMessage;
import android.nfc.Tag;
import fi.siika.bttagwriter.data.BtTagGenerator;
import fi.siika.bttagwriter.data.TagInformation;
import fi.siika.bttagwriter.exceptions.OutOfSpaceException;

/**
 * Interface class for all different specific technology writers
 */
public abstract class TagTechWriter {
	
	private final static byte TLV_NDEF_MESSAGE = 3;
	
	/**
	 * Interface called to write information to given tag
	 * @param tag
	 * @param info
	 * @param type Type of tag written
	 * @return
	 * @throws Exception
	 */
	public abstract int writeToTag (Tag tag, TagInformation info)
		throws Exception;
	
	/**
	 * Put specific close functionality behind this function
	 * @return
	 */
	public abstract void close(Tag tag) throws Exception;
	
	/**
	 * Generate payload with single ndef message. Adds TLV frame for it.
	 * @param info Information used to generate payload
	 * @param type Type of tag written
	 * @param sizeLimit Limit in bytes
	 * @return Payload in byte array
	 * @throws UnsupportedEncodingException 
	 */
	protected static byte[] generatePayload (TagInformation info,
	        int sizeLimit) throws OutOfSpaceException,
	        UnsupportedEncodingException {
		
		final int SPACE_TAKEN_BY_TLV = 2;
		
		NdefMessage ndefMessage = BtTagGenerator.generateNdefMessageForBtTag (
			info, (short)(sizeLimit - SPACE_TAKEN_BY_TLV));
		
		if (ndefMessage == null) {
			throw new OutOfSpaceException("Not enough space for payload");
		}
		
		// Construct the payload
		byte[] message = ndefMessage.toByteArray();
		int msgLen = message.length;
		
		if ((msgLen + 2) > sizeLimit) {
			throw new OutOfSpaceException("Not enough space for message");
		}
		
		byte[] payload = new byte[msgLen + SPACE_TAKEN_BY_TLV];
		payload[0] = TLV_NDEF_MESSAGE;
		payload[1] = (byte)(msgLen);
		for (int i = 0; i < msgLen; ++i) {
			payload[SPACE_TAKEN_BY_TLV+i] = message[i];
		}
		return payload;
	}
	
}
