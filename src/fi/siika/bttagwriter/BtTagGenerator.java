/**
 * BtTagCreator.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter;

import java.io.IOException;
import java.util.Arrays;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.MifareUltralight;

/**
 * BtTagCreator is used to construct NdefMessages written to the tags
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class BtTagGenerator {
	
	/**
	 * Will construct NdefMessage for given Bluetooth name and address
	 * @param name Name of Bluetooth device
	 * @param address Address of Bluetooth device ("00:00:00:00:00:00" format)
	 * @return NdefMessage with asked information
	 */
	public static NdefMessage generateNdefMessageForBtTag (String name, 
		String address) {
		
		byte[] emptyByteArray = new byte[0];
		
		byte[] hsData = new byte[] {0x12}; // HS version 1.2 
		NdefRecord hs = new NdefRecord (NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_HANDOVER_SELECT, emptyByteArray, hsData);
		
		NdefRecord ac = new NdefRecord (NdefRecord.TNF_WELL_KNOWN,
			NdefRecord.RTD_ALTERNATIVE_CARRIER, emptyByteArray,
			generateAlternativeCarrierData (name, address));
				
		NdefMessage ret = new NdefMessage(new NdefRecord[] {hs, ac});
		return ret;
	}
	
	private static byte[] generateAlternativeCarrierData (String name,
		String address) {
		
		short len = (short)(2 + 6 + 1 + 1 + name.length() + 1 + 1 + 3);
		byte data[] = new byte[len];
		
		int index = -1;
		
		// total len (2 bytes)
		data[++index] = (byte)((len >> 8) & 0xFF);
		data[++index] = (byte)(len & 0xFF);
		
		// address (6 bytes) TODO!
		String[] parts = address.split(":");
		data[++index] = Byte.parseByte(parts[5], 16);
		data[++index] = Byte.parseByte(parts[4], 16);
		data[++index] = Byte.parseByte(parts[3], 16);
		data[++index] = Byte.parseByte(parts[2], 16);
		data[++index] = Byte.parseByte(parts[1], 16);
		data[++index] = Byte.parseByte(parts[0], 16);
		
		// name len (index + name) (1 byte)
		byte[] nameBytes = new byte[0];
		try {
			nameBytes = name.getBytes("UTF-8");
			data[++index] = (byte)(nameBytes.length + 0x01);
		} catch (Exception e) {
			data[++index] = 0x01;
		}
		
		// name index 0x09 (1 byte)
		data[++index] = 0x09;
		
		// name (n bytes)
		for (int i = 0; i < nameBytes.length; ++i) {
			data[++index] = (byte)nameBytes[i];
		}
		
		// class len (1 byte)
		data[++index] = 0x04;
		
		// class index 0x0D (1 byte)
		data[++index] = 0x0D;
		
		//TODO: Support something else too
		//BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO = 0x0402
		// class eg. 0x140420 (3 bytes)
		data[++index] = 0x14;
		data[++index] = 0x04;
		data[++index] = 0x20;

		return data;
	}
}
