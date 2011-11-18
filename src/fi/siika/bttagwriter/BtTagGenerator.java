package fi.siika.bttagwriter;

import android.nfc.NdefRecord;

public class BtTagGenerator {
	static byte[] generateData (String name, String address) {
		
		short len = (short)(2 + 6 + 1 + 1 + name.length() + 1 + 1 + 3);
		byte data[] = new byte[len];
		
		int index = -1;
		
		// total len (2 bytes)
		data[++index] = (byte)((len >> 8) & 0xFF);
		data[++index] = (byte)(len & 0xFF);
		
		// address (6 bytes) TODO!
		data[++index] = 0x00;
		data[++index] = 0x00;
		data[++index] = 0x00;
		data[++index] = 0x00;
		data[++index] = 0x00;
		data[++index] = 0x00;
		
		// name len (index + name) (1 byte)
		data[++index] = (byte)(name.length() + 1);
		
		// name index 0x09 (1 byte)
		data[++index] = 0x09;
		
		// name (n bytes)
		for (int i = 0; i < name.length(); ++i) {
			data[++index] = (byte)name.charAt(i);
		}
		
		// class len (1 byte)
		data[++index] = 0x04;
		
		// class index 0x0D (1 byte)
		data[++index] = 0x0D;
		
		// class eg. 0x140420 (3 bytes)
		data[++index] = 0x14;
		data[++index] = 0x04;
		data[++index] = 0x20;

		return data;
	}
}
