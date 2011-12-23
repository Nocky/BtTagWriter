/**
 * MifareClassicWriter.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import java.util.Arrays;

import android.nfc.tech.MifareClassic;
import android.util.Log;

/**
 * Class that takes care of Mifare Classic actions. Like formatting and writing
 * of NDEF message.
 */
public class MifareClassicWriter {
	
	private final static byte[] KEY_MAD = new byte[] {(byte)0xA0, (byte)0xA1,
		(byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5};
	
	private final static byte[] KEY_A = MifareClassic.KEY_NFC_FORUM;
	private final static byte[] KEY_B = MifareClassic.KEY_DEFAULT;
	
	private final static String DEBUG_TAG = "MifareClassicWriter";
	
	/**
	 * Generate GPB
	 * @param majorVer
	 * @param minorVer
	 * @return
	 */
	private static byte generateGPB (int majorVer, int minorVer) {
		byte ret;
		
		majorVer &= 3;
		minorVer &= 3;
		
		//TODO: last 4 bytes are security, for no 0000 = no secure for
		//read or write of ...
		ret = (byte)((majorVer << 6) | (minorVer << 4));
		
		return ret;
	}
	
	/**
	 * Generate access bytes
	 * @param c1
	 * @param c2
	 * @param c3
	 * @return
	 */
	private static byte[] generateAccessBytes (int c1, int c2, int c3) {
		
		// clear unwanted bits
		c1 = c1 & 15;
		c2 = c2 & 15;
		c3 = c3 & 15;
		
		byte[] ret = new byte[3];
		
		// ~c2 and ~c1 (high first)
		ret[0] = (byte)((c2 << 4) | c1);
		ret[0] = (byte)~ret[0];
		
		// c1 and ~c3 (high first)
		ret[1] = (byte)((c1 << 4) | c3);
		ret[1] ^= (byte)15;
		
		// c3 and c2 (high first)
		ret[2] = (byte)((c3 << 4) | c2);
		
		return ret;
	}
	
	/**
	 * Writes sector trailer with safety checks
	 * @param tag Tag where information is written
	 * @param blockIndex Index of block written (must point to trailer)
	 * @param keyA Key A written
	 * @param keyB Key B written
	 * @throws Exception
	 */
	private static void writeSectorTrailer (MifareClassic tag, int blockIndex,
		byte[] keyA, byte[] keyB, int c1, int c2, int c3) throws Exception {
		
		//Keep this safety check!
		if ((blockIndex + 1) % 4 != 0 ||keyA.length != 6 || keyB.length != 6) {
			throw new Exception("Invalid parameters");
		}
		
		byte[] trailer = new byte[16];
		
		int trailerIndex = -1;
		
		// first 6 bytes are key A
		for (int i = 0; i < keyA.length; ++i) {
			trailer[++trailerIndex] = keyA[i];
		}
		
		// next 3 bytes are access bytes
		byte[] accessBytes = generateAccessBytes (c1, c2, c3);
		for (int i = 0; i < accessBytes.length; ++i) {
			trailer[++trailerIndex] = accessBytes[i];
		}
		
		// next 1 byte is GPB
		trailer[++trailerIndex] = generateGPB (1, 0);
		
		// last 6 bytes are key B
		for (int i = 0; i < keyB.length; ++i) {
			trailer[++trailerIndex] = keyB[i];
		}
		
		Log.d (DEBUG_TAG, "Sector trailer " + String.valueOf(trailer.length));
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < trailer.length; ++i) {
			sb.append(trailer[i]);
			sb.append(" ");
		}
		Log.d (DEBUG_TAG, sb.toString());
		
		tag.writeBlock(blockIndex, trailer);
	}
	
	/**
	 * Simplified version from writeSectorTrailer. Used for now. Will grand all
	 * rights to both keys.
	 * @param tag
	 * @param blockIndex
	 * @throws Exception
	 */
	private static void writeZeroSecuritySectorTrailer (MifareClassic tag,
		int blockIndex) throws Exception {
		
		if (blockIndex == 3) {
			writeSectorTrailer (tag, blockIndex, KEY_MAD, KEY_B, 0, 0, 0);
		} else {
			writeSectorTrailer (tag, blockIndex, KEY_A, KEY_B, 0, 0, 0);
		}
	}
	
	/**
	 * Adds safety checks to write operation preventing writing to trailer
	 * @param tag Tag where information is written
	 * @param blockIndex Index of block written (must not point to trailer)
	 * @param data Data written
	 * @throws Exception
	 */
	private static void writeBlockData (MifareClassic tag, int blockIndex,
		byte[] data) throws Exception {
		
		//Keep this safety check
		if (blockIndex == 0 || (blockIndex + 1) % 4 == 0 || data.length != 16) {
			throw new Exception("Invalid parameters");
		}
		
		tag.writeBlock(blockIndex, data);
	}
	
	public static void writeData (MifareClassic tag, byte[] data)
		throws Exception {
		
		boolean connected = false;
		
		if (tag.isConnected() == false) {
			tag.connect();
			connected = true;
		}
		
		int blockIndex = 4;
		
		for (int dataIndex = 0; dataIndex < data.length; ) {
			
			int sectorIndex = blockIndex / 4;
			
			if (tag.authenticateSectorWithKeyA(sectorIndex, KEY_A)
				== false) {
				
				throw new Exception ("Key fail with sector "
					+ String.valueOf(sectorIndex));
			}
			
			byte[] block = null;
			
			if (blockIndex != 0) {
				block = Arrays.copyOfRange(data, dataIndex, dataIndex + 16);
			    writeBlockData (tag, blockIndex, block);
			    dataIndex += 16;
			}
			
			block = Arrays.copyOfRange(data, dataIndex, dataIndex + 16);
			writeBlockData (tag, blockIndex + 1, block);
			dataIndex += 16;
			
			block = Arrays.copyOfRange(data, dataIndex, dataIndex + 16);
			writeBlockData (tag, blockIndex + 2, block);
			dataIndex += 16;
			
			blockIndex += 4;
		}
		
		if (connected) {
			tag.close();
		}
	}
	
	/**
	 * Format Mifare classic to NDEF. Will only format if needed.
	 * @param tag Tag formatted
	 * @exception Throws exception if action failed
	 */
	public static void ndefFormat (MifareClassic tag) throws Exception {
		boolean connected = false;
		
		if (tag.isConnected() == false) {
			tag.connect();
			connected = true;
		}
		
		// Initialize all to NFC forum key with all access rights
		int sectorsInitialized = 0;
		int sectors = tag.getSectorCount();
		
		for (int i = 0; i < sectors; ++i) {
			if (i == 0 && tag.authenticateSectorWithKeyA (i, KEY_MAD)) {
				// LÖKDÖLKDÖLDÖLDKÖLKDÖLKDÖLKLÖKDKLDLÖ!!!
				// DLKÖLDKÖLDKÖLDKÖLKDÖ!!!! UNCOMMENT!
				//continue;
			
			} else if (i > 0 && tag.authenticateSectorWithKeyA (i, KEY_A)) {
				
				Log.d (DEBUG_TAG, "Skip sector " + String.valueOf(i));
				continue;
				
			
			} else if (tag.authenticateSectorWithKeyA (i,
				MifareClassic.KEY_DEFAULT) == false &&
				tag.authenticateSectorWithKeyA(i, MifareClassic.KEY_NFC_FORUM)
				== false &&	tag.authenticateSectorWithKeyA(i, KEY_MAD)
				== false) {
				
				Log.e (DEBUG_TAG, "Invalid sector " + String.valueOf(i));
				throw new Exception ("Failed to initialize");
			}
			
		    int blockIndex = 3 + (i * 4);
			writeZeroSecuritySectorTrailer (tag, blockIndex);
			
			if (i == 0) {
				byte[] block = new byte[16];
				Arrays.fill(block, (byte)0);
				
				writeBlockData (tag, 2, block);
				
				block[0] = (byte)0xF3;
				block[1] = (byte)0x01;
				block[2] = (byte)0x03;
				block[3] = (byte)0xE1;
				block[4] = (byte)0x03;
				block[5] = (byte)0xE1;
				writeBlockData (tag, 1, block);
			}
			
			++sectorsInitialized;
		}
		
		Log.d (DEBUG_TAG, String.valueOf(sectorsInitialized)
			+ " sectors initialized");
		
		if (connected) {
			tag.close();
		}
		
		return;
	}

}
