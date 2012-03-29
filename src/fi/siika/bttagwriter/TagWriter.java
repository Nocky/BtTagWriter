/**
 * TagWriter.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter;

import java.io.IOException;

import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Handler;
import android.util.Log;
import fi.siika.bttagwriter.exceptions.OutOfSpaceException;

/**
 * TagWriter provides thread and code that will take care of the tag write
 * process.
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class TagWriter extends Thread {
	
	final static private String TAG = "TagWriter";
	
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
	
	private Handler mHandler = null;
	private boolean mCancelled = false;
	
	private TagInformation mInfo = null;
	private Tag mTag = null;
	private TagTechWriter mTechWriter = null;
	
	// handler messages emitted
	public final static int HANDLER_MSG_SUCCESS = 0;
	public final static int HANDLER_MSG_CANCELLED = 1;
	public final static int HANDLER_MSG_CONNECTION_LOST = -1;
	public final static int HANDLER_MSG_FAILED_TO_FORMAT = -2;
	public final static int HANDLER_MSG_TOO_SMALL = -3;
	public final static int HANDLER_MSG_TAG_NOT_ACCEPTED = -4;
	public final static int HANDLER_MSG_FAILED_TO_WRITE = -5;
	public final static int HANDLER_MSG_WRITE_PROTECTED = -6;

	
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
		
		if (mTag != null) {
			return false;
		}
		
		String[] techs = tag.getTechList();
		for (int i = 0; i < techs.length; ++i) {
			Log.d (TAG, "Tag tech: " + techs[i]);
		}
		
		MifareUltralight mul = MifareUltralight.get (tag);
		Ndef ndef = Ndef.get (tag);
		NdefFormatable format = NdefFormatable.get(tag);
		
		if (mul != null) {
			mTag = tag;
			mTechWriter = new MifareUltralightTechWriter();
		} else if (ndef != null || format != null) {
			mTag = tag;
			mTechWriter = new NdefTechWriter();
		} else {
			Log.e (TAG, "Failed to identify tag given");
			return false;
		}
			
		mInfo = (TagInformation)(information.clone());
		mCancelled = false;
		run();
		return true;
	}
	
	/**
	 * Implementation of Thread run.
	 */
	@Override
	public void run() {
		int message = HANDLER_MSG_SUCCESS;
		
		try {
			message = mTechWriter.writeToTag(mTag, mInfo);
		} catch (OutOfSpaceException e) {
			message = HANDLER_MSG_TOO_SMALL;
			Log.w (TAG, "Out of space: " + e.getMessage());
		} catch (IOException e) {
			if (mCancelled) {
				message = HANDLER_MSG_CANCELLED;
			} else {
				Log.w(TAG, e.getClass().getSimpleName()
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
		
		mTag = null;
		
		if (mHandler != null) {
			mHandler.sendMessage(mHandler.obtainMessage(message));
		}
	}
	
	/**
	 * Cancel current write process if started
	 */
	public void cancel() {
		if (mTag != null && mTechWriter != null) {	
			try {
				mTechWriter.close(mTag);
			} catch (Exception e) {
				Log.e (getClass().getSimpleName(), "Failed to cancel: "
					+ e.getMessage());
			}
		}
	}
}
