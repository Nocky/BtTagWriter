/*
 * TagWriter.java (BT Tag Writer)
 *
 * https://github.com/alump/BtTagWriter
 *
 * Copyright 2011-2013 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter.writers;

import android.app.Activity;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;

import fi.siika.bttagwriter.data.TagInformation;
import fi.siika.bttagwriter.exceptions.WriteException;

/**
 * TagWriter provides thread and code that will take care of the tag write
 * process.
 *
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class TagWriter implements Runnable {

    final static private String TAG = "TagWriter";

    //private final Handler mHandler;
    private boolean mCancelled = false;
    private Activity mActivity;
    private TagWriterListener mListener;
    private TagInformation mInfo;
    private Tag mTag;
    private TagTechWriter mTechWriter;

    /**
     * Interface for write result listener
     */
    public interface TagWriterListener {
        void onSuccess();

        void onFailure(WriteError error);
    }

    /**
     * Construct new TagWriter
     */
    public TagWriter(Activity activity, TagWriterListener listener) {
        mActivity = activity;
        mListener = listener;
    }

    private TagTechWriter resolveTechWriter(Tag tag) {
        if (Ndef.get(tag) != null || NdefFormatable.get(tag) != null) {
            return new NdefTechWriter();
        } else if (MifareUltralight.get(tag) != null) {
            return new MifareUltralightTechWriter();
        } else {
            Log.w(TAG, "Tag not supported!");
            return null;
        }
    }

    /**
     * Start write process to given tag. Will call run if initialization was
     * success.
     *
     * @param tag         Tag now connected with device
     * @param information Information written to tag
     * @return true if write process and thread was started. false if given
     * tag is not supported
     */
    public boolean writeToTag(Tag tag, TagInformation information) {

        if (mTag != null) {
            return false;
        }

        mTechWriter = resolveTechWriter(tag);
        if (mTechWriter == null) {
            String[] techs = tag.getTechList();
            StringBuilder sb = new StringBuilder();
            for (String tech : techs) {
                sb.append(tech);
                sb.append(" ");
            }
            Log.w(TAG, "Supported Tech not found: " + sb.toString());
            return false;
        } else {
            Log.d(TAG, "Tech writer " + mTechWriter.toString());
        }

        mInfo = (TagInformation) (information.clone());
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
        WriteError error = null;

        try {
            mTechWriter.writeToTag(mTag, mInfo);
        } catch (WriteException e) {
            error = e.getErrorCode();
            Log.w(TAG, "Write exception: " + e.getMessage());
        } catch (NullPointerException e) {
            error = WriteError.SYSTEM_ERROR;
            e.printStackTrace();
            Log.e(TAG, "Null pointer exception!");
        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e.getClass().getSimpleName() + " " + e.getMessage());
            error = WriteError.CONNECTION_LOST;
        }

        mTag = null;

        if (error != null) {
            final WriteError errorArg = error;
            //mHandler.sendMessage(mHandler.obtainMessage(message));
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListener.onFailure(errorArg);
                }
            });
        } else {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListener.onSuccess();
                }
            });
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
                Log.e(TAG, "Failed to cancel: " + e.getMessage());
            }
        }
    }
}
