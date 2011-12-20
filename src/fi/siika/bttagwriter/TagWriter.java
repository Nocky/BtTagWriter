/**
 * TagWriter.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter;

import java.io.IOException;
import java.util.Arrays;

import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NdefFormatable;
import android.os.Handler;
import android.util.Log;

/**
 * TagWriter provides thread and code that will take care of the tag write
 * process.
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class TagWriter extends Thread {
	
	/**
	 * Class containing information written to NFC tags
	 */
	public static class TagInformation implements Cloneable {
		/**
		 * Bluetooth address of device ("00:00:00:00:00:00" format)
		 */
		public String address;
		
		/**
		 * Bluetooth name of device
		 */
		public String name;
		
		/**
		 * If true writer will try to write protected the tag
		 */
		public boolean readOnly = false;
		
		/**
		 * Pin code or if empty no pin code
		 */
		public String pin;
		
		@Override
		public Object clone () {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}
	}
	
	/**
	 * Exception used when write fails because of lack of space
	 */
	private class OutOfSpaceException extends Exception {
		
		public OutOfSpaceException (String msg) {
			super (msg);
		}
		
		private static final long serialVersionUID = 1L;
		
	}
	
	private Handler mHandler = null;
	private TagInformation mInfo = null;
	private Tag mTag = null;
	private String mTagClass;
	private boolean mCancelled = false;
	
	public final static int HANDLER_MSG_SUCCESS = 0;
	public final static int HANDLER_MSG_CANCELLED = 1;
	public final static int HANDLER_MSG_CONNECTION_LOST = -1;
	public final static int HANDLER_MSG_FAILED_TO_FORMAT = -2;
	public final static int HANDLER_MSG_TOO_SMALL = -3;
	public final static int HANDLER_MSG_TAG_NOT_ACCEPTED = -4;
	public final static int HANDLER_MSG_FAILED_TO_WRITE = -5;
	public final static int HANDLER_MSG_WRITE_PROTECTED = -6;
	
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
	 * Construct new TagWriter
	 * @param handler Handler used to send messages
	 */
	public TagWriter (Handler handler) {
		mHandler = handler;
	}
	
	/**
	 * Start write process to given tag. Will call run if initialization was
	 * success.
	 * @param tag Tag now connected with device
	 * @param information Information written to tag
	 * @return true if write process and thread was started
	 */
	public boolean writeToTag (Tag tag, TagInformation information) {
		
		MifareUltralight mul = MifareUltralight.get(tag);
		if (mul != null) {
			mTag = tag;
			mTagClass = MifareUltralight.class.getName();
		} else {
			mTag = null;
		}
		
		if (mTag != null) {
			mInfo = (TagInformation)(information.clone());
			mCancelled = false;
			run();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Implementation of Thread run.
	 */
	@Override
	public void run() {
		int message = HANDLER_MSG_SUCCESS;
		
		try {
			if (mTagClass.equals(MifareUltralight.class.getName())) {
				MifareUltralight mul = MifareUltralight.get(mTag);
				message = writeToMifareUltraLight(mul);
			} else if (mTagClass.equals(NdefFormatable.class.getName())) {
				NdefFormatable.get(mTag).format (
					BtTagGenerator.generateNdefMessageForBtTag(mInfo,
						(short)128));
			} else {
				message = HANDLER_MSG_TAG_NOT_ACCEPTED;
			}
		} catch (OutOfSpaceException e) {
			message = HANDLER_MSG_TOO_SMALL;
			Log.w (getClass().getSimpleName(), "Not enough space");
		} catch (IOException e) {
			if (mCancelled) {
				message = HANDLER_MSG_CANCELLED;
			} else {
				Log.w(getClass().getName(), e.getClass().getSimpleName()
					+ " " + e.getMessage());
				message = HANDLER_MSG_CONNECTION_LOST;
			}
		} catch (FormatException e) {
			Log.e(getClass().getName(),"Failed to format");
			message = HANDLER_MSG_FAILED_TO_FORMAT;
		} catch (Exception e) {
			Log.w(getClass().getName(), "Exception: " + e.getMessage());
			message = HANDLER_MSG_CONNECTION_LOST;
		}
		
		if (mHandler != null) {
			mHandler.sendMessage(mHandler.obtainMessage(message));
		}
	}
	
	private void writeDataToMifareUltraLight(MifareUltralight tag,
		byte[] intLock, byte[] cc, byte[] payload) throws Exception {
		
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
	
	private int writeToMifareUltraLight(MifareUltralight tag) throws Exception {
		
		tag.connect();
		
		int ndefSizeLimitPages = 36;
		if (tag.getType() == MifareUltralight.TYPE_ULTRALIGHT) {
			ndefSizeLimitPages = 12;
		}
		
		Log.d (getClass().getSimpleName(), new StringBuilder().append (
			"Assume MUL size to be ").append(ndefSizeLimitPages).toString());
		
		int sizeAvailableBytes = ndefSizeLimitPages * 
			MifareUltralight.PAGE_SIZE - 2;
		
		byte[] ndefMessage = null;
		ndefMessage = BtTagGenerator.generateNdefMessageForBtTag(
			mInfo, (short)sizeAvailableBytes).toByteArray();
		
		// Construct the payload
		byte[] payload = new byte[ndefMessage.length + 2];
		payload[0] = 0x03;
		payload[1] = (byte)(ndefMessage.length);
		for (int i = 0; i < ndefMessage.length; ++i) {
			payload[2+i] = ndefMessage[i];
		}
		
		// Check the size of payload
		int pages = payload.length / MifareUltralight.PAGE_SIZE;
		if (payload.length % MifareUltralight.PAGE_SIZE != 0) {
			pages += 1;
		}
		if (ndefSizeLimitPages < pages) {
			Log.e (getClass().getSimpleName(), new StringBuilder().append(
				"Too many pages!").append(pages).toString());
		    throw new OutOfSpaceException("Too many pages " + String.valueOf(pages));
		}
		
		// Construct CC
		byte secByte = CC_NO_SECURITY_BYTE;
		if (mInfo.readOnly) {
			secByte = CC_READ_ONLY_SECURITY_BYTE;
		}
		byte[] cc = new byte[] { CC_NDEF_BYTE, CC_NDEF_VERSION_1_1_BYTE,
			(byte)(ndefSizeLimitPages * MifareUltralight.PAGE_SIZE / 8),
			secByte};
		
		// Construct Lock bytes
		byte[] intLock = null;
		if (mInfo.readOnly) {
			Log.d (getClass().getSimpleName(), "Turning on lock bits");
			intLock = new byte[] { 0x00, 0x00, 0x00, 0x00 };
		    intLock[2] = -1;
			intLock[3] = -1;
		}
		
		// Try to write data
		writeDataToMifareUltraLight (tag, intLock, cc, payload);
		
		//Finally activate locking if needed
		//TODO: what 0x26 is? find documentation and proper name for it
		if (mInfo.readOnly) {
			try {
				byte[] res = tag.transceive(new byte[] {MUL_CMD_REQA});
			} catch (Exception e) {
				//TODO: Don't know why this throws exception but it does
				//ignoring it for now.
			}
		}
		
		tag.close();
		
		Log.d(getClass().getName(), "Mifare Ultralight written");
		return HANDLER_MSG_SUCCESS;
	}
	
	/**
	 * Cancel current write process if started
	 */
	public void cancel() {
		if (mTag != null) {
			try {
				if (mTagClass.equals(MifareUltralight.class.getName())) {
					MifareUltralight.get(mTag).close();
				} else if (mTagClass.equals(NdefFormatable.class.getName())) {
					NdefFormatable.get(mTag).close();
				}
			} catch (IOException e) {
			}
		}
	}
}
