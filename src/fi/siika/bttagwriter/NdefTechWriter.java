/**
 * NdefTechWriter.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;
import fi.siika.bttagwriter.TagWriter.TagInformation;

/**
 * Class that takes care of writing to Ndef and to NdefFormatable tags
 */
public class NdefTechWriter extends TagTechWriter {
	
	private final static String DEBUG_TAG ="NdefTechWriter";

	/* (non-Javadoc)
	 * @see fi.siika.bttagwriter.TagTechWriter#writeToTag(android.nfc.Tag, fi.siika.bttagwriter.TagWriter.TagInformation)
	 */
	public int writeToTag(Tag tag, TagInformation info) throws Exception {
		
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
	
	private int writeToNdef (Ndef tag, TagInformation info) throws Exception {
		
		int ret = TagWriter.HANDLER_MSG_SUCCESS;
		
		tag.connect();
		
		NdefMessage msg = BtTagGenerator.generateNdefMessageForBtTag(info,
			tag.getMaxSize());
		
		tag.writeNdefMessage(msg);
		if (info.readOnly) {
			tag.makeReadOnly();
		}
		
		tag.close();
		
		Log.d (DEBUG_TAG, "Ndef written");
		
		return ret;
	}
	
	private int writeToNdefFormatable (NdefFormatable tag, TagInformation info)
		throws Exception {
		
		int ret = TagWriter.HANDLER_MSG_SUCCESS;
		
		tag.connect();
		
		NdefMessage msg = BtTagGenerator.generateNdefMessageForBtTag(info, -1);
		
		if (info.readOnly) {
			tag.formatReadOnly(msg);
		} else {
			tag.format(msg);
		}
		
		tag.close();
		
		Log.d (DEBUG_TAG, "NdefFormatable written");
		
		return ret;
		
	}

}
