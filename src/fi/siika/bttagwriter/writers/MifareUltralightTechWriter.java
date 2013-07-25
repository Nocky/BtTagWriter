/**
 * MifareUltralightWriter.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.writers;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import fi.siika.bttagwriter.data.TagInformation;
import fi.siika.bttagwriter.exceptions.IOFailureException;
import fi.siika.bttagwriter.exceptions.OutOfSpaceException;
import fi.siika.bttagwriter.exceptions.WriteException;

/**
 * Special write functionality for Mifare Ultralights. This class can be
 * removed if Ndef(Formatable) can handle ultralights well enough. But that
 * can not be tested before 4.0.3 version of Android is released.
 */
public class MifareUltralightTechWriter extends TagTechWriter {
	
	private final static String TAG = "MUWriter";
	
	private final static int START_INTLOCK_MIFARE_UL_PAGE = 2;
	private final static int START_CC_MIFARE_UL_PAGE = 3;
	private final static int START_NDEF_MIFARE_UL_PAGE = 4;
	private final static byte CC_NDEF_BYTE = (byte)0xE1;
	private final static byte CC_NDEF_VERSION_1_1_BYTE = (byte)0x11;
	private final static byte CC_NO_SECURITY_BYTE = (byte)0x00;
	private final static byte CC_READ_ONLY_SECURITY_BYTE = (byte)0x0F;
	private final static byte MUL_CMD_REQA = 0x26;
	private final static byte MUL_CMD_WUPA = 0x52;
	
	/**
	 * Writes given information to tag given
	 * @param tag Tag where information is written
	 * @param info Information written
	 * @return TagWriter handler result
	 * @throws UnsupportedEncodingException 
	 * @throws Exception Throws exception if error
	 */
	@Override
	public void writeToTag (Tag tag, TagInformation info)
	        throws WriteException {
		
		MifareUltralight mul = MifareUltralight.get(tag);
		
		try {
			mul.connect();
		} catch (IOException e) {
			throw new IOFailureException ("Failed to connect to MUL", e);
		}
		
		int ndefSizeLimitPages = 36;
		if (mul.getType() == MifareUltralight.TYPE_ULTRALIGHT) {
			ndefSizeLimitPages = 12;
		}
		
		Log.d (TAG, new StringBuilder().append (
			"Assume MUL size to be ").append(ndefSizeLimitPages).toString());
		
		int sizeAvailableBytes = ndefSizeLimitPages * 
			MifareUltralight.PAGE_SIZE;
		
		byte[] payload;
        try {
		    payload = generatePayload (info, sizeAvailableBytes);
        } catch (UnsupportedEncodingException e) {
            throw new WriteException(WriteError.SYSTEM_ERROR, e, "Encoding exception");
        }
			
		// Check the size of payload
		int pages = payload.length / MifareUltralight.PAGE_SIZE;
		if (payload.length % MifareUltralight.PAGE_SIZE != 0) {
			pages += 1;
		}
		if (ndefSizeLimitPages < pages) {
			Log.e (TAG, new StringBuilder().append (
				"Too many pages!").append(pages).toString());
		    throw new OutOfSpaceException("Too many pages "
				+ String.valueOf(pages) + " for UL");
		}
		
		// Construct CC
		byte secByte = CC_NO_SECURITY_BYTE;
		if (info.isReadOnly()) {
			secByte = CC_READ_ONLY_SECURITY_BYTE;
		}
		byte[] cc = new byte[] { CC_NDEF_BYTE, CC_NDEF_VERSION_1_1_BYTE,
			(byte)(ndefSizeLimitPages * MifareUltralight.PAGE_SIZE / 8),
			secByte};
		
		// Construct Lock bytes
		byte[] intLock = null;
		if (info.isReadOnly()) {
			Log.d (TAG, "Turning on lock bits");
			intLock = new byte[] { 0x00, 0x00, 0x00, 0x00 };
		    intLock[2] = -1;
			intLock[3] = -1;
		}
		
		// Try to write data
		try {
			writeData (mul, intLock, cc, payload);
		} catch (IOException e) {
			throw new IOFailureException("Failed to write to MUL", e);
		}
		
		//Finally activate locking if needed
		//TODO: what 0x26 is? find documentation and proper name for it
		if (info.isReadOnly()) {
			try {
				mul.transceive(new byte[] {MUL_CMD_REQA});
			} catch (Exception e) {
				//TODO: Don't know why this throws exception but it does
				//ignoring it for now.
			}
		}
		
		try {
			mul.close();
		} catch (IOException e) {
			throw new IOFailureException ("Failed to close MUL", e);
		}
		
		Log.d (TAG, "Mifare Ultralight written");
	}
	
	@Override
	public void close (Tag tag) throws Exception {
		MifareUltralight mul = MifareUltralight.get(tag);
		mul.close();
	}
	
	private static void writeData (MifareUltralight tag,
		byte[] intLock, byte[] cc, byte[] payload) throws IOException {
			
		//Write payload
		int pageNum = START_NDEF_MIFARE_UL_PAGE;
		for (int i = 0; i < payload.length; i = i + 4) {
			byte[] page = Arrays.copyOfRange(payload, i, i + 4);
			tag.writePage (pageNum, page);
			pageNum += 1;
		}
		
		//Write CC
		tag.writePage(START_CC_MIFARE_UL_PAGE, cc);
		
		//Write IntLock if given
		if (intLock != null) {
			tag.writePage(START_INTLOCK_MIFARE_UL_PAGE, intLock);
		}
	}

}
