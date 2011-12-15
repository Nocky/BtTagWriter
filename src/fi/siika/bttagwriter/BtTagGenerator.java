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
	
	/**
	 * Will construct NdefMessage for given Bluetooth name and address
	 * @param name Name of Bluetooth device
	 * @param address Address of Bluetooth device ("00:00:00:00:00:00" format)
	 * @return NdefMessage with asked information
	 */
	public static NdefMessage generateNdefMessageForBtTag (String name, 
		String address) {
		
		NdefRecord media = null;
		try {
			media = new NdefRecord (NdefRecord.TNF_MIME_MEDIA,
				BT_EP_OOB_MIME_TYPE.getBytes("UTF-8"), new byte[] {(byte)0x01},
				generateBluetoothMedia (name, address, false));
		} catch (Exception e) {
			Log.d("BtTagGenerator", e.getMessage());
		}
				
		NdefMessage ret = new NdefMessage(new NdefRecord[] {
			generateHandoverSelectRecord(), media});
		return ret;
	}
	
	//https://www.bluetooth.org/Technical/AssignedNumbers/generic_access_profile.htm
	private final static byte BTAN_SHORTENED_LOCAL_NAME = 0x08;
	private final static byte BTAN_COMPLETE_LOCAL_NAME = 0x09; 
	private final static byte BTAN_CLASS_OF_DEVICE = 0x0D;
	
	private final static String BT_EP_OOB_MIME_TYPE =
		"application/vnd.bluetooth.ep.oob";
	
	
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
		byte[] data = {0x01, 0x01, 0x30, 0x00};
		return data;
	}
		
	private static byte[] generateBluetoothMedia (String name,
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
		
		// total length (2 bytes)
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort(len);
		data[++index] = buffer.get(0);
		data[++index] = buffer.get(1);
		
		// address (6 bytes) TODO!
		Log.d("BtTagGenerator", address);
		String[] parts = address.split(":");
		Log.d("BtTagGenerator", String.valueOf(parts.length));
		Log.d("BtTagGenerator", parts[5]);
		data[++index] = (byte)Short.parseShort(parts[5], 16);
		Log.d("BtTagGenerator", new StringBuilder().append(data[index]).toString());
		data[++index] = (byte)Short.parseShort(parts[4], 16);
		data[++index] = (byte)Short.parseShort(parts[3], 16);
		data[++index] = (byte)Short.parseShort(parts[2], 16);
		data[++index] = (byte)Short.parseShort(parts[1], 16);
		data[++index] = (byte)Short.parseShort(parts[0], 16);
		
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
