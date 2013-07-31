/*
 * NdefTechWriter.java (BT Tag Writer)
 *
 * https://github.com/alump/BtTagWriter
 *
 * Copyright 2011-2013 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.writers;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import fi.siika.bttagwriter.data.BtTagGenerator;
import fi.siika.bttagwriter.data.TagInformation;
import fi.siika.bttagwriter.data.TagType;
import fi.siika.bttagwriter.exceptions.IOFailureException;
import fi.siika.bttagwriter.exceptions.WriteException;

/**
 * Class that takes care of writing to Ndef and to NdefFormatable tags
 */
public class NdefTechWriter extends TagTechWriter {

    private final static String TAG = "NdefTechWriter";


    /* (non-Javadoc)
     * @see fi.siika.bttagwriter.TagTechWriter#writeToTag(android.nfc.Tag, fi.siika.bttagwriter.TagWriter.TagInformation)
     */
    @Override
    public void writeToTag(Tag tag, TagInformation info) throws WriteException {

        Ndef ndef = Ndef.get(tag);

        if (ndef != null) {
            writeToNdef(ndef, info, info.getType());
            return;
        } else {
            NdefFormatable form = NdefFormatable.get(tag);
            if (form != null) {
                writeToNdefFormatable(form, info);
                return;
            }
        }

        throw new WriteException(WriteError.FAILED_TO_WRITE);
    }

    @Override
    public void close(Tag tag) throws IOFailureException {
        Ndef ndef = Ndef.get(tag);

        try {
            if (ndef != null) {
                ndef.close();
            } else {
                NdefFormatable form = NdefFormatable.get(tag);
                form.close();
            }
        } catch (IOException e) {
            throw new IOFailureException("Failed to close Ndef(Formatable)", e);
        }
    }

    private void writeToNdef(Ndef tag, TagInformation info, TagType type) throws
            WriteException {

        Log.d(TAG, "Ndef writing...");

        try {
            tag.connect();
        } catch (IOException e) {
            throw new IOFailureException(WriteError.CONNECTION_LOST, e, "Failed to connect with Ndef");
        }

        try {
            NdefMessage msg = BtTagGenerator.generateNdefMessageForBtTag(info,
                    tag.getMaxSize());
            tag.writeNdefMessage(msg);
        } catch (UnsupportedEncodingException e) {
            throw new WriteException(WriteError.SYSTEM_ERROR, e, "Encoding exception");
        } catch (FormatException e) {
            throw new WriteException(WriteError.FAILED_TO_FORMAT, e, "Failed to format");
        } catch (IOException e) {
            throw new WriteException(WriteError.FAILED_TO_WRITE, e, "Failed to write NDEF");
        }


        if (info.isReadOnly()) {
            try {
                tag.makeReadOnly();
            } catch (IOException e) {
                throw new WriteException(WriteError.FAILED_TO_FORMAT, e, "Failed to set read only");
            }
        }

        try {
            tag.close();
        } catch (IOException e) {
            throw new WriteException(WriteError.FAILED_TO_WRITE, e, "Failed to close NDEF");
        }

        Log.d(TAG, "Ndef written");
    }

    private void writeToNdefFormatable(NdefFormatable tag, TagInformation info)
            throws WriteException {

        Log.d(TAG, "NdefFormatable writing...");

        try {
            if (tag.isConnected() == false) {
                tag.connect();
            }
        } catch (IOException e) {
            throw new IOFailureException(WriteError.CONNECTION_LOST, e, "Failed to connect NdefFormatable");
        }


        NdefMessage msg;
        try {
            msg = BtTagGenerator.generateNdefMessageForBtTag(info, -1);
        } catch (UnsupportedEncodingException e) {
            throw new WriteException(WriteError.SYSTEM_ERROR, e, "Encoding exception");
        }

        if (info.isReadOnly()) {
            try {
                tag.formatReadOnly(msg);
            } catch (FormatException e) {
                throw new WriteException(WriteError.FAILED_TO_FORMAT, e, "Failed to RO format");
            } catch (IOException e) {
                throw new IOFailureException(WriteError.FAILED_TO_FORMAT, e, "Failed to RO format NdefFormatable");
            }
        } else {
            try {
                tag.format(msg);
            } catch (FormatException e) {
                throw new WriteException(WriteError.FAILED_TO_FORMAT, e, "Failed to format");
            } catch (IOException e) {
                throw new IOFailureException(WriteError.FAILED_TO_FORMAT, e, "Failed to format NdefFormatable");
            }
        }

        try {
            tag.close();
        } catch (IOException e) {
            throw new WriteException(WriteError.FAILED_TO_WRITE, e, "Failed to close NDEF");
        }

        Log.d(TAG, "NdefFormatable written");

    }

}
