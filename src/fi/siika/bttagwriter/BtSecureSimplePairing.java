/**
 * BtSecureSimplePairing.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.util.Log;

/**
 * Class providing parse and generate functions for Bluetooth Secure Simple
 * Pairing binaries
 */
public class BtSecureSimplePairing {
	
	public final static String MIME_TYPE = "application/vnd.bluetooth.ep.oob";
	
	/*
	 * Magic values are from:
	 * https://www.bluetooth.org/Technical/AssignedNumbers/generic_access_profile.htm
	 */
	private final static byte BYTE_SHORTENED_LOCAL_NAME = 0x08;
	private final static byte BYTE_COMPLETE_LOCAL_NAME = 0x09; 
	private final static byte BYTE_CLASS_OF_DEVICE = 0x0D;
	private final static byte BYTE_SIMPLE_PAIRING_HASH = 0x0E;
	
	private final static String DEBUG_TAG = "BtSecureSimplePairing";
	
	private final static short SPACE_TOTAL_LEN_BYTES = 2;
	private final static short SPACE_ADDRESS_BYTES = 6;
	private final static short SPACE_DEVICE_CLASS_BYTES = 5;
	private final static short SPACE_MIN_BYTES =
		SPACE_TOTAL_LEN_BYTES + SPACE_ADDRESS_BYTES + SPACE_DEVICE_CLASS_BYTES;
	
	/**
	 * Class containing the data we care about
	 */
	public static class Data {
		private String mName;
		private String mAddress;
		private int mDeviceClass;
		
		public Data() {
			mName = "";
			mAddress = "00:00:00:00:00:00";
			mDeviceClass = (0x14 << 16) |
				BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO;
		}
		
		public void setName(String name) {
			mName = name;
		}
		
		public String getName () {
			return mName;
		}
		
		/**
		 * Will only accept valid addresses. But both lower and upper case
		 * letters are accepted.
		 * @param address Address in string format (e.g. "00:00:00:00:00:00")
		 */
		public void setAddress (String address) {
			String modAddress = address.toUpperCase();
			if (BluetoothAdapter.checkBluetoothAddress(modAddress)) {
				mAddress = modAddress;
			}
		}
		
		public String getAddress () {
			return mAddress;
		}
		
		public void setDeviceClass (int deviceClass) {
			mDeviceClass = deviceClass;
		}
		
		public int getDeviceClass () {
			return mDeviceClass;
		}
	};
	
	/*
	 * How binary data is constructed:
	 * First two bytes = total length
	 * Next six bytes = address
	 * Repeat next until end:
	 * first byte = "data length"
	 * second byte = type
	 * "data length" bytes = data 
	 */
	
	/**
	 * Generate binary Bluetooth Secure Simple Pairing content
	 * @param input Information stored to binary output
	 * @param makeSort TODO!
	 * @return Return binary content
	 */
	public static byte[] generate(Data input, short maxLength)
		throws IOException {
		
		//TODO: Is 30k bytes enough? I assume so ;) (16th bit can't be used)
		//TODO: Can we leave class out?
		short len = SPACE_MIN_BYTES;
		
		byte[] nameBytes = input.getName().getBytes("UTF-8");
		short spaceTakenByName = (short)(2 + nameBytes.length);
		
		// No name at all if we do not have space
		// Maybe short name could be tried too? But usually total name is short
		// anyway. So cutting name does not help much.
		if (len + spaceTakenByName > maxLength) {
			nameBytes = null;
			spaceTakenByName = 0;
		}
		
		len += spaceTakenByName;
		
		//Never return too big!
		if (len > maxLength) {
			throw new IOException("Not enough space");
		}
		
		byte data[] = new byte[len];
		int index = -1;
		
		// total length (2 bytes)
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort(len);
		data[++index] = buffer.get(0);
		data[++index] = buffer.get(1);
		
		// address (6 bytes) TODO!
		String[] parts = input.getAddress().split(":");
		data[++index] = (byte)Short.parseShort(parts[5], 16);
		data[++index] = (byte)Short.parseShort(parts[4], 16);
		data[++index] = (byte)Short.parseShort(parts[3], 16);
		data[++index] = (byte)Short.parseShort(parts[2], 16);
		data[++index] = (byte)Short.parseShort(parts[1], 16);
		data[++index] = (byte)Short.parseShort(parts[0], 16);
		
		if (spaceTakenByName > 0) {
		
			// name len (index + name) (1 byte)
			data[++index] = (byte)(nameBytes.length + 0x01);
			data[++index] = BYTE_COMPLETE_LOCAL_NAME;
			// name (n bytes)
			for (int i = 0; i < nameBytes.length; ++i) {
				data[++index] = (byte)nameBytes[i];
			}
			
		}
		
		// Define class of device (TODO: change this so that it will support
		// other values)
		data[++index] = 0x04;
		data[++index] = BYTE_CLASS_OF_DEVICE;
		//TODO: Read from input data!!!
		// Class of Device (3 bytes)
		// https://www.bluetooth.org/apps/content/?doc_id=49706
		data[++index] = 0x14;
		// 0x0402 = BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
		data[++index] = 0x04;
		data[++index] = 0x20;

		return data;
	}
	
	/**
	 * Parse binary Bluetooth Secure Simple Pairing content
	 * @param binaryData Binary data
	 * @return Binary data converted to class for easy access
	 */
	public static Data parse (byte[] binaryData) throws Exception {
		Data data = new Data();
		
		//TODO: ignoring for now length bytes 0 and 1
		
		//TODO: There has to be nicer way to do this!!!
    	int[] addressBuffer = new int[6];
    	for (int i = 0; i < 6; i++) {
    		addressBuffer[i] = (int)(binaryData[7-i]) & 0xff;
    	}
    	StringBuilder sb = new StringBuilder();
    	for (int i = 0; i < 6; ++i) {
    		if (i > 0) {
    			sb.append(":");
    		}
    		if (addressBuffer[i] < 0x10) {
    			sb.append("0");
    		}

    		sb.append (Integer.toHexString(addressBuffer[i]));
    	}
    	data.setAddress(sb.toString());
    	Log.d (DEBUG_TAG, "Address: " + data.getAddress());
    	
    	//Read the rest
    	for (int i = 8; i < binaryData.length; ++i) {
    		byte dataLen = binaryData[i];
    		byte dataType = binaryData[i+1];
    		byte[] dataArray = Arrays.copyOfRange(binaryData, i+2,
    			i + 1 + dataLen);
    		
    		i = i + 1 + dataLen + 1; //Update index for next round
    		
    		switch (dataType) {
    		case BYTE_COMPLETE_LOCAL_NAME:
    			data.setName(new String(dataArray, "UTF-8"));
    			break;
    		case BYTE_SHORTENED_LOCAL_NAME:
    			//Do not override complete name if it exists
    			if (data.getName().isEmpty()) {
    				data.setName(new String(dataArray, "UTF-8"));
    			}
    			break;
    		case BYTE_CLASS_OF_DEVICE:
    			ByteBuffer bb = ByteBuffer.wrap(dataArray);
    			data.setDeviceClass(bb.asIntBuffer().get());
    			break;
    		default:
    			//There are many known elements we ignore here
    			Log.w(DEBUG_TAG, new StringBuilder().append(
    				"Unknown element: ").append(dataType).toString());
    		}
    		
    	}
    	
    	Log.d (DEBUG_TAG, "Parsed data: '" + data.getAddress() + "' '"
    		+ data.getName() + "'");
		
		return data;
	}
}
