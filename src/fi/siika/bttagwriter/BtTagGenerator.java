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
			generateAlternativeCarrierData (name, address, false));
				
		NdefMessage ret = new NdefMessage(new NdefRecord[] {hs, ac});
		return ret;
	}
	
	//https://www.bluetooth.org/Technical/AssignedNumbers/generic_access_profile.htm
	private final static byte BTAN_SHORTENED_LOCAL_NAME = 0x08;
	private final static byte BTAN_COMPLETE_LOCAL_NAME = 0x09; 
	private final static byte BTAN_CLASS_OF_DEVICE = 0x0D;
	
	private static byte[] generateAlternativeCarrierData (String name,
		String address, boolean makeSort) {
		
		//Magic values from...
		//https://www.bluetooth.org/Technical/AssignedNumbers/generic_access_profile.htm
		
		short spaceTakenByName = 0;
		byte[] nameBytes = new byte[0];
		if (makeSort == false) {
			try {
				nameBytes = name.getBytes("UTF-8");
				spaceTakenByName = (short)(2 + nameBytes.length);
			} catch (Exception e) {
			}
		}
		
		short len = (short)(2 + 6 + spaceTakenByName + 1 + 1 + 3);
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
		
		if (spaceTakenByName > 0) {
		
			// name len (index + name) (1 byte)
			data[++index] = (byte)(nameBytes.length + 0x01);
			data[++index] = BTAN_COMPLETE_LOCAL_NAME;
			// name (n bytes)
			for (int i = 0; i < nameBytes.length; ++i) {
				data[++index] = (byte)nameBytes[i];
			}
			
		}
		
		// Define class of device (TODO: change this so that it will support
		// other values)
		data[++index] = 0x04;
		data[++index] = BTAN_CLASS_OF_DEVICE;
		// Class of Device (3 bytes)
		// https://www.bluetooth.org/apps/content/?doc_id=49706
		data[++index] = 0x14;
		// 0x0402 = BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
		data[++index] = 0x04;
		data[++index] = 0x20;

		return data;
	}
}
