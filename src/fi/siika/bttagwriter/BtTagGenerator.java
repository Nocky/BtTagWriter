/**
 * BtTagCreator.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.MifareUltralight;
import android.util.Log;

/**
 * BtTagCreator is used to construct NdefMessages written to the tags
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class BtTagGenerator {
	
	private static final String DEBUG_TAG = "BtTagGenerator";
	
	/**
	 * Will construct NdefMessage for given Bluetooth name and address
	 * @param name Name of Bluetooth device
	 * @param address Address of Bluetooth device ("00:00:00:00:00:00" format)
	 * @return NdefMessage with asked information
	 */
	public static NdefMessage generateNdefMessageForBtTag (String name, 
		String address, short sizeLimit) throws IOException {
		
		NdefRecord media = null;
		
		BtSecureSimplePairing.Data content = new BtSecureSimplePairing.Data();
		content.setName(name);
		content.setAddress(address);
		
		byte[] mime = BtSecureSimplePairing.MIME_TYPE.getBytes("UTF-8");
		int minSize = 4 + mime.length 
			+ BtSecureSimplePairing.MIN_SIZE_IN_BYTES;
		
		StringBuilder sb = new StringBuilder();
		sb.append("minSize/sizeLimit ");
		sb.append(minSize);
		sb.append("/");
		sb.append(sizeLimit);
		Log.d (DEBUG_TAG, sb.toString());
		
		/* Let's see if to enable this
		if (minSize > sizeLimit) {
			minSize -= mime.length;
			mime = BtSecureSimplePairing.SHORT_MIME_TYPE.getBytes("UTF-8");
			minSize += mime.length;
		}
		*/
		
		if (minSize > sizeLimit) {
			Log.e (DEBUG_TAG, "Not enough size!");
			return null;
		}
		
		int mediaSizeLimit = sizeLimit - 2 - mime.length;
		
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
	
	private static byte[] generateAlternativeCarrierData () {
		//0x01 = active target
		//0x01 = ndef record id
		//0x30 = TODO: FIX THIS. Length of something
		//0x00 = RFU
		byte[] data = {0x01, 0x01, 0x30, 0x00};
		return data;
	}
}
