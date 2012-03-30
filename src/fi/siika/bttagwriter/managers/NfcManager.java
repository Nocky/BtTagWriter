/**
 * NfcManager.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.managers;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.util.Log;

/**
 * NFC manager takes care of setup of NFC
 */
public class NfcManager {
	
	private PendingIntent mPendingIntent = null;
	private Activity mActivity = null;
	private NfcAdapter mAdapter = null;
	private final static String TAG = "NfcManager";
	
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
		
		if (mPendingIntent != null) {
			return;
		}
		
		Log.d (TAG, "Enable ndef dispatch");

		mPendingIntent = PendingIntent.getActivity(mActivity, 0,
			new Intent(mActivity, mActivity.getClass()).addFlags(
				Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			
		IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			tech.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}

		nfcAdapter.enableForegroundDispatch(mActivity, mPendingIntent,
			new IntentFilter[] { tech }, new String[][] {});
	}
	
	public void enableTechDiscovered() {
		NfcAdapter nfcAdapter = getAdapter();
		if (nfcAdapter == null ) {
			return;
		}
		
		if (mPendingIntent != null) {
			return;
		}
		
		Log.d (TAG, "Enable tech dispatch");

		mPendingIntent = PendingIntent.getActivity(mActivity, 0,
			new Intent(mActivity, mActivity.getClass()).addFlags(
				Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			
		IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		try {
			tech.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}

		//TODO: Use xml instead?
		String[][] techList = new String[][] { new String[] {
			MifareUltralight.class.getName() }, new String[] {
			Ndef.class.getName() }, new String[] { 
			NdefFormatable.class.getName() } };

		nfcAdapter.enableForegroundDispatch(mActivity, mPendingIntent,
			new IntentFilter[] { tech }, techList);
	}
	
	public void disableForegroundDispatch() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
			return;
		} else if (mAdapter != null && mAdapter.isEnabled()) {
			Log.d (TAG, "Disable dispatch");
			mAdapter.disableForegroundDispatch (mActivity);
			mPendingIntent = null;
		}
	}

}
