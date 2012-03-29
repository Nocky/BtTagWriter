/**
 * NdefTechWriter.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.writers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;
import fi.siika.bttagwriter.TagWriter;
import fi.siika.bttagwriter.data.BtTagGenerator;
import fi.siika.bttagwriter.data.TagInformation;
import fi.siika.bttagwriter.exceptions.IOFailureException;
import fi.siika.bttagwriter.exceptions.OutOfSpaceException;

/**
 * Class that takes care of writing to Ndef and to NdefFormatable tags
 */
public class NdefTechWriter extends TagTechWriter {
	
	private final static String TAG ="NdefTechWriter";
	

	/* (non-Javadoc)
	 * @see fi.siika.bttagwriter.TagTechWriter#writeToTag(android.nfc.Tag, fi.siika.bttagwriter.TagWriter.TagInformation)
	 */
	@Override
	public int writeToTag(Tag tag, TagInformation info) throws IOFailureException,
	OutOfSpaceException, UnsupportedEncodingException, FormatException {
		
		Ndef ndef = Ndef.get(tag);
		
		if (ndef != null) {
			return writeToNdef (ndef, info);
		} else {
			NdefFormatable form = NdefFormatable.get(tag);
			if (form != null) {
				return writeToNdefFormatable (form, info);
			}
		}
		
		return TagWriter.HANDLER_MSG_FAILED_TO_WRITE;
	}
	
	@Override
	public void close (Tag tag) throws Exception {
		Ndef ndef = Ndef.get(tag);
		
		if (ndef != null) {
			ndef.close();
		} else {
			NdefFormatable form = NdefFormatable.get(tag);
			form.close();
		} 
	}
	
	private int writeToNdef (Ndef tag, TagInformation info) throws IOFailureException,
		OutOfSpaceException, UnsupportedEncodingException, FormatException {
		
		int ret = TagWriter.HANDLER_MSG_SUCCESS;
		
		try {
			tag.connect();
		} catch (IOException e) {
			throw new IOFailureException("Failed to connect with Ndef");
		}
		
		NdefMessage msg = BtTagGenerator.generateNdefMessageForBtTag(info,
			tag.getMaxSize());
		
	
		try {
			tag.writeNdefMessage(msg);
		} catch (IOException e1) {
			throw new IOFailureException("Failed to write to Ndef");
		}
		
		
		if (info.readOnly) {
			try {
				tag.makeReadOnly();
			} catch (IOException e) {
				throw new IOFailureException("Failed to make Ndef RO");
			}
		}
		
		try {
			tag.close();
		} catch (IOException e) {
			throw new IOFailureException("Failed to close Ndef");
		}
		
		Log.d (TAG, "Ndef written");
		
		return ret;
	}
	
	private int writeToNdefFormatable (NdefFormatable tag, TagInformation info)
		throws IOFailureException, OutOfSpaceException, UnsupportedEncodingException, FormatException {
		
		int ret = TagWriter.HANDLER_MSG_SUCCESS;
		
		try {
			if (tag.isConnected() == false) {
				tag.connect();
			}
		} catch (IOException e) {
			throw new IOFailureException("Failed to connect NdefFormatable");
		}
		
		NdefMessage msg = BtTagGenerator.generateNdefMessageForBtTag(info, -1);
		
		if (info.readOnly) {
			try {
				tag.formatReadOnly(msg);
			} catch (IOException e) {
				throw new IOFailureException("Failed to RO format NdefFormatable");
			}
		} else {
			try {
				tag.format(msg);
			} catch (IOException e) {
				throw new IOFailureException("Failed to format NdefFormatable");
			}
		}
		
		try {
			tag.close();
		} catch (IOException e) {
			throw new IOFailureException("Failed to close NdefFormatable");
		}
		
		Log.d (TAG, "NdefFormatable written");
		
		return ret;
		
	}

}
