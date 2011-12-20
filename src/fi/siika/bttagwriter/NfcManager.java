/**
 * NfcManager.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.tech.MifareUltralight;
import android.os.Build;

/**
 * 
 */
public class NfcManager {
	
	private PendingIntent mPendingIntent = null;
	private Activity mActivity = null;
	private NfcAdapter mAdapter = null;
	
	public NfcManager (Activity activity) {
		mActivity = activity;
	}
	
	public NfcAdapter getAdapter() {
		//TODO: Remove this
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
			return null;
		} else if (mAdapter == null) {
			mAdapter = NfcAdapter.getDefaultAdapter(mActivity);
		}
		
		return mAdapter;
	}
	
	public void enableForegroundNdefDiscovered() {
		NfcAdapter nfcAdapter = getAdapter();
		if (nfcAdapter == null ) {
			return;
		}

		mPendingIntent = PendingIntent.getActivity(mActivity, 0,
			new Intent(mActivity, mActivity.getClass()).addFlags(
				Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			
		IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			tech.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}

		/*
		String[][] techList = new String[][] { new String[] {
			MifareUltralight.class.getName() } };
		*/

		nfcAdapter.enableForegroundDispatch(mActivity, mPendingIntent,
			new IntentFilter[] { tech }, new String[][] {});
	}
	
	public void enableTechDiscovered() {
		NfcAdapter nfcAdapter = getAdapter();
		if (nfcAdapter == null ) {
			return;
		}

		mPendingIntent = PendingIntent.getActivity(mActivity, 0,
			new Intent(mActivity, mActivity.getClass()).addFlags(
				Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			
		IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		try {
			tech.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}

		String[][] techList = new String[][] { new String[] {
			MifareUltralight.class.getName() } };

		nfcAdapter.enableForegroundDispatch(mActivity, mPendingIntent,
			new IntentFilter[] { tech }, techList);
	}
	
	public void disableForegroundDispatch() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
			return;
		} else if (mAdapter != null && mAdapter.isEnabled()) {
			mAdapter.disableForegroundDispatch (mActivity);
			mPendingIntent = null;
		}
	}

}
