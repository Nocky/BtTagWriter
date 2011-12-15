/**
 * PairActivity.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;
import android.bluetooth.IBluetoothA2dp;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothHeadset;

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
    
    private UUID[] uuids = new UUID[] {
    	UUID.fromString ("00001112-0000-1000-8000-00805F9B34FB"),
    	UUID.fromString ("00001203-0000-1000-8000-00805F9B34FB"),
    	UUID.fromString ("00000003-0000-1000-8000-00805F9B34FB"),
    	UUID.fromString ("00001108-0000-1000-8000-00805F9B34FB"),
    	UUID.fromString ("0000111F-0000-1000-8000-00805F9B34FB"),
    	UUID.fromString ("0000111E-0000-1000-8000-00805F9B34FB"),
    	UUID.fromString ("0000110B-0000-1000-8000-00805F9B34FB")
    };
    
    private IBluetoothA2dp getIBluetoothA2dp() {

    	IBluetoothA2dp ibta = null;

    	try {

    	    Class c2 = Class.forName("android.os.ServiceManager");

    	    Method m2 = c2.getDeclaredMethod("getService", String.class);
    	    IBinder b = (IBinder) m2.invoke(null, "bluetooth_a2dp");

    	    Log.d(getClass().getSimpleName(), b.getInterfaceDescriptor());

    	    Class c3 = Class.forName("android.bluetooth.IBluetoothA2dp");

    	    Class[] s2 = c3.getDeclaredClasses();

    	    Class c = s2[0];
    	    // printMethods(c);
    	    Method m = c.getDeclaredMethod("asInterface", IBinder.class);

    	    m.setAccessible(true);
    	    ibta = (IBluetoothA2dp) m.invoke(null, b);

    	} catch (Exception e) {
    	    Log.e(getClass().getSimpleName(), "Shit " + e.getMessage());
    	}
    	
    	return ibta;
    }
    
    private IBluetoothHeadset getIBluetoothHeadset() {

    	IBluetoothHeadset hset = null;

    	try {

    	    Class c2 = Class.forName("android.os.ServiceManager");

    	    Method m2 = c2.getDeclaredMethod("getService", String.class);
    	    IBinder b = (IBinder) m2.invoke(null, "bluetooth_headset");

    	    Log.d(getClass().getSimpleName(), b.getInterfaceDescriptor());

    	    Class c3 = Class.forName("android.bluetooth.IBluetoothHeadset");

    	    Class[] s2 = c3.getDeclaredClasses();

    	    Class c = s2[0];
    	    // printMethods(c);
    	    Method m = c.getDeclaredMethod("asInterface", IBinder.class);

    	    m.setAccessible(true);
    	    hset = (IBluetoothHeadset) m.invoke(null, b);

    	} catch (Exception e) {
    	    Log.e(getClass().getSimpleName(), "Shit " + e.getMessage());
    	}
    	
    	return hset;
    }
    
    private IBluetooth getIBluetooth() {

    	IBluetooth ibt = null;

    	try {

    	    Class c2 = Class.forName("android.os.ServiceManager");

    	    Method m2 = c2.getDeclaredMethod("getService", String.class);
    	    IBinder b = (IBinder) m2.invoke(null, "bluetooth");

    	    Log.d(getClass().getSimpleName(), b.getInterfaceDescriptor());

    	    Class c3 = Class.forName("android.bluetooth.IBluetooth");

    	    Class[] s2 = c3.getDeclaredClasses();

    	    Class c = s2[0];
    	    // printMethods(c);
    	    Method m = c.getDeclaredMethod("asInterface", IBinder.class);

    	    m.setAccessible(true);
    	    ibt = (IBluetooth) m.invoke(null, b);

    	} catch (Exception e) {
    	    Log.e(getClass().getSimpleName(), "Shit " + e.getMessage());
    	}
    	
    	return ibt;
    }


    
    private void connectToBluetoothDevice (BluetoothDevice device) {
    	
    	mBtMgr.getBluetoothAdapter().getProfileProxy(this, null,
    		BluetoothProfile.A2DP);
    	
    	IBluetooth bt = getIBluetooth();
    	IBluetoothA2dp a2dp = getIBluetoothA2dp();
    	//IBluetoothHeadset hset = getIBluetoothHeadset();
    	
    	try {
    		bt.createBond(device.getAddress());
    		Log.d(getClass().getSimpleName(),
    			new StringBuilder().append(a2dp.getPriority(device)).toString());
    		a2dp.connect(device);
    		Log.d (getClass().getSimpleName(), "Hep!");
    	} catch (Exception e) {
    		Toast toast = Toast.makeText(this, "iFail", Toast.LENGTH_LONG);
    		toast.show();
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
