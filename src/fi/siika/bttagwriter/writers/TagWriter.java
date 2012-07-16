/**
 * TagWriter.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter.writers;

import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Handler;
import android.util.Log;
import fi.siika.bttagwriter.data.TagInformation;
import fi.siika.bttagwriter.exceptions.IOFailureException;
import fi.siika.bttagwriter.exceptions.IOFailureException.Step;
import fi.siika.bttagwriter.exceptions.OutOfSpaceException;

/**
 * TagWriter provides thread and code that will take care of the tag write
 * process.
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class TagWriter implements Runnable {
	
	final static private String TAG = "TagWriter";
	
	private final Handler mHandler;
	private boolean mCancelled = false;
	
	private TagInformation mInfo;
	private Tag mTag;
	private TagTechWriter mTechWriter;
	
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
	
	private TagTechWriter resolveTechWriter (Tag tag) {		
		if (Ndef.get (tag) != null || NdefFormatable.get (tag) != null) {
			return new NdefTechWriter();
		} else if (MifareUltralight.get (tag) != null) {
			return new MifareUltralightTechWriter();
		} else {
			Log.w(TAG, "Tag not supported!");
			return null;
		}
	}
	
	/**
	 * Start write process to given tag. Will call run if initialization was
	 * success.
	 * @param tag Tag now connected with device
	 * @param information Information written to tag
	 * @param type Type of tag written
	 * @return true if write process and thread was started. false if given
	 * tag is not supported
	 */
	public boolean writeToTag (Tag tag, TagInformation information) {
		
		if (mTag != null) {
			return false;
		}
		
		mTechWriter = resolveTechWriter (tag);
		if (mTechWriter == null) {
			String[] techs = tag.getTechList();
			StringBuilder sb = new StringBuilder();
			for (String tech : techs) {
				sb.append(tech);
				sb.append(" ");
			}
			Log.w (TAG, "Supported Tech not found: " + sb.toString());
			return false;
		} else {
			Log.d (TAG, "Tech writer " + mTechWriter.toString());
		}
			
		mInfo = (TagInformation)(information.clone());
		mCancelled = false;
		mTag = tag;
		
		Thread thread = new Thread(this);
		thread.start();
		
		return true;
	}
	
	/**
	 * Implementation of Runnable run.
	 */
	public void run() {
		int message = HANDLER_MSG_SUCCESS;
		
		try {
			message = mTechWriter.writeToTag(mTag, mInfo);
		} catch (OutOfSpaceException e) {
			message = HANDLER_MSG_TOO_SMALL;
			Log.w (TAG, "Out of space: " + e.getMessage());
		} catch (IOFailureException e) {
			if (mCancelled) {
				message = HANDLER_MSG_CANCELLED;
			} else {
				if (Step.FORMAT.equals(e.getStep())) {
					message = HANDLER_MSG_FAILED_TO_FORMAT;
				} else {
					message = HANDLER_MSG_CONNECTION_LOST;
				}
				Log.e(TAG, "IO failure: " + e.getLongMessage());
			}
		} catch (FormatException e) {
			Log.e(getClass().getName(),"Failed to format: " + e.getMessage());
			message = HANDLER_MSG_FAILED_TO_FORMAT;
		} catch (NullPointerException e) {
			e.printStackTrace();
			Log.e (TAG, "Null pointer exception!");
		} catch (Exception e) {
			Log.w(TAG, "Exception: " + e.getClass().getSimpleName() + " " + e.getMessage());
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
				mCancelled = true;
				mTechWriter.close(mTag);
			} catch (Exception e) {
				Log.e (TAG, "Failed to cancel: " + e.getMessage());
			}
		}
	}
}
