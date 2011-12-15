/**
 * PairActivity.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import java.util.Arrays;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 */
public class PairActivity extends Activity {
	
	private BluetoothManager mBtMgr = null;
	private NfcManager mNfcMgr = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pair);
        
        Log.d("hep", "created!");
        
        mBtMgr = new BluetoothManager (this, null);
        mNfcMgr = new NfcManager (this);
    }
    
    @Override
    public void onResume() {    
    	super.onResume();
        mNfcMgr.enableForegroundNdefDiscovered();
    }
    
    @Override
    public void onPause() {
    	mNfcMgr.disableForegroundDispatch();
    	super.onPause();
    }
    
    private void connectToBluetoothDevice (BluetoothDevice device) {
    	UUID uuid = UUID.fromString ("0000110B-0000-1000-8000-00805F9B34FB");
    	try {
    		BluetoothSocket socket =
    			device.createInsecureRfcommSocketToServiceRecord(uuid);
    		socket.connect();
    	} catch (Exception e) {
    		Toast toast = Toast.makeText(this, "FUCK!", Toast.LENGTH_LONG);
    	}
    }
    
    private void handleBluetoothNdefRecord (NdefRecord rec) {
    	//first 6 bytes = address
    	byte[] data = rec.getPayload();
    	int[] address = new int[6];
    	for (int i = 0; i < 6; i++) {
    		address[i] = (int)(data[7-i]) & 0xff;
    	}
    	
    	StringBuilder sb = new StringBuilder();
    	for (int i = 0; i < 6; ++i) {
    		if (i > 0) {
    			sb.append(":");
    		}
    		if (address[i] < 0x10) {
    			sb.append("0");
    		}

    		sb.append (Integer.toHexString(address[i]));
    	}
    	
    	String strAddress = sb.toString();
    	strAddress = strAddress.toUpperCase();
    	Log.d (getClass().getSimpleName(), strAddress);
    	
    	if (BluetoothAdapter.checkBluetoothAddress(strAddress)) {
    		connectToBluetoothDevice (
    			mBtMgr.getBluetoothAdapter().getRemoteDevice (strAddress));
    	} else {
    		sb.append(" fail!");
    		Toast toast = Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG);
    		toast.show();
    	}
    	
    }
    
    private final static String BT_EP_OOB_MIME_TYPE =
        "application/vnd.bluetooth.ep.oob";
    
    private byte[] mMimeByteArray = null;
    
    private byte[] getMimeType() {
    	if (mMimeByteArray == null) {
    		try {
    			mMimeByteArray = BT_EP_OOB_MIME_TYPE.getBytes("UTF-8");
    		} catch (Exception e) {
    		}
    	}
    				
    	return mMimeByteArray;
    }

    
    private void handleNdefMessages(NdefMessage[] msgs) {
    	for (int i = 0; i < msgs.length; ++i) {
    		NdefMessage msg = msgs[i];
    		NdefRecord[] recs = msg.getRecords();
    		for (int j = 0; j < recs.length; ++j) {
    			NdefRecord rec = recs[j];
    			if (rec.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
    				Arrays.equals(rec.getType(), getMimeType())) {
    			
    				handleBluetoothNdefRecord (rec);
    			}
    		}
    	}
    	
    }
    
    @Override
    public void onNewIntent(Intent intent) {
    	
    	Log.d("hep", "here!");
    	
    	setIntent (intent);
    	
    	if (intent.getAction().equals("android.nfc.action.NDEF_DISCOVERED")) {   
    		
    		Parcelable[] parcs = intent.getParcelableArrayExtra(
    			NfcAdapter.EXTRA_NDEF_MESSAGES);
    		NdefMessage[] msgs = new NdefMessage[parcs.length];
    		for (int i = 0; i < parcs.length; ++i) {
    			msgs[i] = (NdefMessage) parcs[i];
    		}
    		
    		if (msgs.length > 0) {
    			handleNdefMessages(msgs);
    		} else {
    			Toast toast = Toast.makeText(this, intent.getAction(), Toast.LENGTH_LONG);
        		toast.show();
    		}
    	} else {
    		Toast toast = Toast.makeText(this, intent.getAction(), Toast.LENGTH_LONG);
    		toast.show();
    	}
    }

}
