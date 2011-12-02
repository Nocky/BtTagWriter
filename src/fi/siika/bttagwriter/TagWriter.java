package fi.siika.bttagwriter;

import java.io.IOException;
import java.util.Arrays;

import android.nfc.FormatException;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.TagTechnology;
import android.os.Handler;
import android.os.Message;

public class TagWriter extends Thread {
	
	public class TagInformation implements Cloneable {
		public String address;
		public String name;
		
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
	private TagInformation mInfo = null;
	private NdefFormatable mTag = null;
	private boolean mCancelled = false;
	
	public final static int HANDLER_MSG_SUCCESS = 0;
	public final static int HANDLER_MSG_CANCELLED = 1;
	public final static int HANDLER_MSG_CONNECTION_LOST = 2;
	public final static int HANDLER_MSG_FAILED_TO_FORMAT = 3;
	public final static int HANDLER_MSG_TOO_SMALL = 4;
	
	/*
	private final static int START_CC_MIFARE_UL_PAGE = 3;
	private final static int START_NDEF_MIFARE_UL_PAGE = 4;
	private final static byte CC_NDEF_BYTE = (byte)0xE1;
	private final static byte CC_NDEF_VERSION_1_0_BYTE = (byte)0x01;
	private final static byte CC_SIZE_48_BYTES_BYTE = (byte)0x06;
	private final static byte CC_NO_SECURITY_BYTE = (byte)0x00;
	*/
	
	
	public TagWriter (Handler handler) {
		mHandler = handler;
	}
	
	public boolean writeToTag (Tag tag, TagInformation information) {

		mInfo = (TagInformation)(information.clone());		
		mTag = NdefFormatable.get(tag);
		
		if (mTag != null) {
			if (mTag.isConnected() == false) {
				return false;
			} else {
				mCancelled = false;
				run();
				return true;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public void run() {
		int message = HANDLER_MSG_SUCCESS;
		
		try {
			mTag.format(BtTagGenerator.generateNdefMessageForBtTag(mInfo.name,
				mInfo.address));
		} catch (IOException e) {
			if (mCancelled) {
				message = HANDLER_MSG_CANCELLED;
			} else {
				message = HANDLER_MSG_CONNECTION_LOST;
			}
		} catch (FormatException e) {
			message = HANDLER_MSG_FAILED_TO_FORMAT;
		}
		
		if (mHandler != null) {
			mHandler.obtainMessage(message);
		}
	}
	
	/*
	private void writeToMifareUltraLight(MifareUltralight tag)
		throws IOException {
		
		int ndefSizeLimitPages = 36;
		if (tag.getType() == MifareUltralight.TYPE_ULTRALIGHT) {
			ndefSizeLimitPages = 12;
		}
		
		byte[] ndefMessage = 
		
		int pages = ndefMessage.length / tag.PAGE_SIZE;
		if (ndefMessage.length % tag.PAGE_SIZE != 0) {
			pages += 1;
		}
		
		if (ndefSizeLimitPages < pages) {
			throw new IOException("Too small tag");
		}
		
		for (int i = 0; i < pages; ++i) {
			int index = 4*i;
			//This will auto fill with 0x00s if we index out
			byte[] page = Arrays.copyOfRange(ndefMessage, index, index + 3);
			tag.writePage (START_NDEF_MIFARE_UL_PAGE + i, page);
		}
		
		byte ccSizeByte = CC_SIZE_48_BYTES_BYTE;
		
		//And finally write header
		tag.writePage(START_CC_MIFARE_UL_PAGE, new byte[] {
			CC_NDEF_BYTE, CC_NDEF_VERSION_1_0_BYTE, ccSizeByte,
			CC_NO_SECURITY_BYTE});
	}
	*/
	
	public void cancel() {
		if (mTag != null) {
			try {
				mTag.close();
			} catch (IOException e) {
			}
		}
	}
}
