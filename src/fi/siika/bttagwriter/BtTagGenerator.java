/**
 * BtTagCreator.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter;

import java.io.IOException;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;

/**
 * BtTagCreator is used to construct NdefMessages written to the tags
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class BtTagGenerator {
	
	private static final String DEBUG_TAG = "BtTagGenerator";
	

	/**
	 * 
	 * @param info
	 * @param sizeLimit Will try to keep size of message lower than this limit.
	 * If -1 will not do any size check and will generate full size message.
	 * @return
	 * @throws IOException
	 */
	public static NdefMessage generateNdefMessageForBtTag (
		TagWriter.TagInformation info, int sizeLimit) throws Exception {
		
		NdefRecord media = null;
		
		BtSecureSimplePairing.Data content = new BtSecureSimplePairing.Data();
		content.setName(info.name);
		content.setAddress(info.address);
		if (info.pin != null && info.pin.isEmpty() == false) {
			content.setTempPin(info.pin);
		}
		
		byte[] mime = BtSecureSimplePairing.MIME_TYPE.getBytes("UTF-8");
		int minSize = 4 + mime.length 
			+ BtSecureSimplePairing.MIN_SIZE_IN_BYTES;
		
		StringBuilder sb = new StringBuilder();
		sb.append("minSize/sizeLimit ");
		sb.append(minSize);
		sb.append("/");
		sb.append(sizeLimit);
		Log.d (DEBUG_TAG, sb.toString());
		
		int mediaSizeLimit = 1024; // Safe max value
		
		if (sizeLimit > 0) {
			if (minSize > sizeLimit) {
				Log.e (DEBUG_TAG, "Not enough space!");
				throw new OutOfSpaceException ("Too small tag");
			}
			
			mediaSizeLimit = sizeLimit - 2 - mime.length;
		}
		
		media = new NdefRecord (NdefRecord.TNF_MIME_MEDIA,
			BtSecureSimplePairing.MIME_TYPE.getBytes("UTF-8"),
			new byte[] {(byte)0x01},
			BtSecureSimplePairing.generate(content,
			(short)(mediaSizeLimit-4)));
		
		Log.d (DEBUG_TAG, new StringBuilder().append(
			"media size: ").append(media.toByteArray().length).toString());
		
		return new NdefMessage(new NdefRecord[] {
			//HS is disabled as it does not seam to work with Android
			//generateHandoverSelectRecord(),
			media});
	}
	
	/* Not used currently
	private static NdefRecord generateHandoverSelectRecord() {
		byte[] ac = new NdefRecord (NdefRecord.TNF_WELL_KNOWN,
			NdefRecord.RTD_ALTERNATIVE_CARRIER, new byte[0],
			generateAlternativeCarrierData ()).toByteArray();
		
		
		byte[] data = new byte[1 + ac.length];
		data[0] = 0x12;
		for (int i = 0; i < ac.length; ++i) {
			data[1+i] = ac[i];
		}
		
		return new NdefRecord (NdefRecord.TNF_WELL_KNOWN,
			NdefRecord.RTD_HANDOVER_SELECT, new byte[0], data);
	}
	*/
	
	/* Not used currently
	private static byte[] generateAlternativeCarrierData () {
		//0x01 = active target
		//0x01 = ndef record id
		//0x30 = TODO: FIX THIS. Length of something
		//0x00 = RFU
		byte[] data = {0x01, 0x01, 0x30, 0x00};
		return data;
	}
	*/
}
