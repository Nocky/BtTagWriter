/**
 * PairActivity.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetooth;
import android.bluetooth.IBluetoothA2dp;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import fi.siika.bttagwriter.data.BtSecureSimplePairing;
import fi.siika.bttagwriter.managers.BluetoothManager;
import fi.siika.bttagwriter.managers.NfcManager;

/**
 * Activity used to handle pairing when suitable NDEF message is found from
 * tag read. This activity is only called via NDEF indents and is not visible
 * in application launcher.
 */
public class PairActivity extends Activity
	implements BluetoothProfile.ServiceListener, ConnectTimer.Listener,
	BluetoothManager.StateListener {
	
	private BluetoothManager mBtMgr = null;
	private NfcManager mNfcMgr = null;
	private BluetoothDevice mConnectedDevice = null;
	private String mDeviceName = new String();
	private Actions mAction = Actions.IDLE;
	private ConnectTimer mTimer = null; //workaround
	
	private final static String TAG = "PairActivity";
	
	/**
	 * Actions PairActivity can be performing
	 */
	private enum Actions {
		IDLE,
		BOUNDING,
		CONNECTING,
		DISCONNECTING
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pair);
        
        Log.d(getClass().getSimpleName(), "created!");
        
        mBtMgr = new BluetoothManager (this);
        mBtMgr.setStateListener(this);
        mNfcMgr = new NfcManager (this);
        mTimer = new ConnectTimer (this);
        
        onNewIntent(getIntent());
    }
    
    @Override
    public void onResume() {    
    	super.onResume();
    	mNfcMgr.enableForegroundNdefDiscovered();
    	
    	mBtMgr.getBluetoothAdapter().getProfileProxy (this, this,
    		BluetoothProfile.A2DP);
    }
    
    @Override
    public void onPause() {
    	mTimer.cancel();
    	mBtMgr.releaseAdapter();
    	mNfcMgr.disableForegroundDispatch();
    	super.onPause();
    }
    
    private String getDeviceName() {
    	String myName = mDeviceName;
    	
    	if (myName.isEmpty()) {
    		
    		myName = getResources().getString(R.string.pair_unnamed_device_str);
    	}
    	
    	return myName;
    }
    
    /**
     * Will take care of bounding (if needed) and connecting
     * @return false if action was not started successfully
     */
    private boolean boundAndConnect () {
    	
    	boolean ret = true;
    	String msg = new String();
    	mAction = Actions.CONNECTING;
    	
    	if (mBtMgr.isEnabled() == false) {
    		mBtMgr.enable();
    	
    	} else if (mConnectedDevice.getBondState() ==
    		BluetoothDevice.BOND_NONE) {
    		
    		try {
	    		IBluetooth bt = BtInterfaces.getBluetooth();
				mAction = Actions.BOUNDING;
				msg = getResources().getString(R.string.pair_bounding_str);
				msg = msg.replaceAll("%1", getDeviceName());
				mTimer.start();
				bt.createBond(mConnectedDevice.getAddress());
    		} catch (Exception e) {
    			mTimer.cancel();
    			msg = getResources().getString(R.string.pair_failed_to_bound_str);
    			msg = msg.replaceAll("%1", getDeviceName());
    			ret = false;
    		}
    		
    	} else {
    		audioConnect ();
    	}
    	
    	showMessage (msg);
    	
    	return ret;
    }
    
    /**
     * Will take care of disconnecting of bluetooth device
     * @param state Current connection state
     * @return false if failed to start disconnecting
     */
    private boolean disconnect (ConnectedState state) {
    	
    	boolean ret = true;
    	mAction = Actions.DISCONNECTING;
		String msg = getResources().getString(
				R.string.pair_disconnecting_str);
		msg = msg.replaceAll("%1", getDeviceName());
		mTimer.start();
    	
    	if (state == ConnectedState.A2DP_CONNECTED) {
			IBluetoothA2dp a2dp = BtInterfaces.getA2dp();
			try {
				a2dp.disconnect (mConnectedDevice);
			} catch (Exception e) {
				msg = getResources().getString (
						R.string.pair_failed_to_disconnect_str);
				msg = msg.replaceAll("%1", getDeviceName());
				
				mTimer.cancel();
				ret = false;
			}
    	}
    	
    	showMessage (msg);
    	return ret;
    	
    }
    
    /**
     * Grouping long list of Bluetooth device classes to simpler list
     */
    private static enum DeviceClass {
    	/**
    	 * Unknown and so ignored Bluetooth device
    	 */
    	UNKNOWN_CLASS,
    	/**
    	 * A2DP audio device, should be connected via A2DP interfaces
    	 */
    	A2DP_CLASS,
    };
    
    private static DeviceClass getDeviceClass (BluetoothDevice device) {
		int devClass = device.getBluetoothClass().getDeviceClass();
		
		// These classes we ignore
		if (devClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
			devClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
			
			return DeviceClass.UNKNOWN_CLASS;
			
		} else {
			
			//TODO: Not all other devices are A2DPs, maybe add all classes to if
			return DeviceClass.A2DP_CLASS;
		}
    }
    
    private static enum ConnectedState {
    	UNKNOWN,
    	DISCONNECTED,
    	A2DP_CONNECTED
    };
    
    private static ConnectedState isDeviceConnected (BluetoothDevice device) {
    	
    	ConnectedState ret = ConnectedState.DISCONNECTED;
    	DeviceClass devClass = getDeviceClass (device);
    	
    	try {
    	   	
	    	if (devClass == DeviceClass.A2DP_CLASS) {
	    		
	    		IBluetoothA2dp a2dp = BtInterfaces.getA2dp();
	    		if (a2dp.getConnectionState(device) ==
		    		BluetoothProfile.STATE_CONNECTED) {
	    			
	    			ret = ConnectedState.A2DP_CONNECTED;
		    			
		    	}
	    	} else {
	    		ret = ConnectedState.UNKNOWN;
	    	}
    	} catch (Exception e) {
    		Log.e (TAG, "Failed to get connected state: "
    			+ e.getMessage());
    	}
    	
    	return ret;
    }
    
    private void changeConnectedState (BluetoothDevice device,
    	String name) {
    	
    	if (device == null) {
    		Log.e (getClass().getSimpleName(),
    			"changeConnectedState called with null");
    		finish();
    		return;
    	}
    	
    	boolean finishActivity = false;
    	mConnectedDevice = device;
    	mDeviceName = name;
    	
    	ConnectedState state = isDeviceConnected (device);
    	
    	if (state == ConnectedState.DISCONNECTED) {
    		finishActivity = !boundAndConnect ();
    	} else if (state != ConnectedState.UNKNOWN) {
    		finishActivity = !disconnect (state);
    	} else {
    		Log.e (getClass().getSimpleName(),
    			"Can not change connection state");
    		finishActivity = true;
    	}
    	
    	if (finishActivity) {
    		finish();
    	}
    }
    
    private void handleBluetoothNdefRecord (NdefRecord rec) {

    	byte[] payload = rec.getPayload();
    	try {
	    	BtSecureSimplePairing.Data data = BtSecureSimplePairing.parse (
	    		payload);
	    	
	    	if (data == null) {
	    		Log.d (getClass().getSimpleName(), "Ignoring tag, invalid data");
	    		this.finish();
	    	}
	    	
	    	changeConnectedState (mBtMgr.getBluetoothAdapter().getRemoteDevice (
	    		data.getAddress()), data.getName());
	    	
    	} catch (Exception e) {
    		Log.e (getClass().getSimpleName(), "Failed to parse and connect");
    	}
    }
    
    /**
     * Check that given mime type is something this activity wants
     * @param input Mime type in byte array
     * @return true if Mime is accepted
     */
    private boolean acceptMimeType (byte[] input) {
    	
    	try {
        	String mime = new String (input, "UTF-8");
        	return BtSecureSimplePairing.validMimeType(mime);
    	} catch (Exception e) {
    		Log.e (getClass().getSimpleName(), "Failed to compare mime types");
    		return false;
    	}
    }

    /**
     * Will check all messages and handle those that are meant for this activity
     * @param msgs Array of NDEF messages
     */
    private void handleNdefMessages(NdefMessage[] msgs) {
    	for (int i = 0; i < msgs.length; ++i) {
    		NdefMessage msg = msgs[i];
    		NdefRecord[] recs = msg.getRecords();
    		for (int j = 0; j < recs.length; ++j) {
    			NdefRecord rec = recs[j];
    			if (rec.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
    				acceptMimeType(rec.getType())) {
    			
    				Log.d(getClass().getSimpleName(),
    					"Reseived BT NDEF record");
    				handleBluetoothNdefRecord (rec);
    			}
    		}
    	}
    	
    }
    
    @Override
    public void onNewIntent(Intent intent) {
    	
    	Log.d(getClass().getSimpleName(), "Intent called");
    	
    	setIntent (intent);
    	
    	if (intent.getAction().equals("android.nfc.action.NDEF_DISCOVERED")) {   
    		
    		Parcelable[] parcs = intent.getParcelableArrayExtra(
    			NfcAdapter.EXTRA_NDEF_MESSAGES);
    		NdefMessage[] msgs = new NdefMessage[parcs.length];
    		for (int i = 0; i < parcs.length; ++i) {
    			msgs[i] = (NdefMessage) parcs[i];
    		}
    		
    		if (msgs.length > 0) {
    			Log.d(getClass().getSimpleName(), "Reseived NDEF messages");
    			handleNdefMessages(msgs);
    		}
    	}
    }

	/* (non-Javadoc)
	 * @see android.bluetooth.BluetoothProfile.ServiceListener#onServiceConnected(int, android.bluetooth.BluetoothProfile)
	 */
	public void onServiceConnected(int profile, BluetoothProfile proxy) {
		
		if (profile == BluetoothProfile.A2DP ||
			profile == BluetoothProfile.HEADSET) {
			
			Log.d(getClass().getSimpleName(), "onServiceConnected");
		
		} else {
				
			Log.d(getClass().getSimpleName(), "Non A2DP BTPSC or headset: "
				+ String.valueOf(profile));
			return;
		}
		
		if (mConnectedDevice == null) {
			Log.d(getClass().getSimpleName(), "ConDev was null");
			return;
		}
		
		int state = proxy.getConnectionState(mConnectedDevice);
		
		//For unknown reason state is little random. Sometimes we receive
		//CONNECTED and nothing else when disconnecting
		if (state == BluetoothProfile.STATE_CONNECTING ||
			state == BluetoothProfile.STATE_CONNECTED) {
			
			if (mAction == Actions.CONNECTING) {
				Log.d(getClass().getSimpleName(), "Device connected");
				this.finish();
			} else {
				Log.w(getClass().getSimpleName(), "Connecting? "
					+ String.valueOf(state));
				mTimer.start();
			}
		} else if (state == BluetoothProfile.STATE_DISCONNECTING ||
			state == BluetoothProfile.STATE_DISCONNECTED) {
			
			if (mAction == Actions.DISCONNECTING) {
				Log.d(getClass().getSimpleName(), "Device disconnected");
				this.finish();
			} else {
				Log.w(getClass().getSimpleName(), "Disconnecting?"
					+ String.valueOf(state));
				mTimer.start();
			}
		} else {
			Log.d(getClass().getSimpleName(), new StringBuilder().append(
				"state: ").append (state).toString());
		}
	}

	/* (non-Javadoc)
	 * @see android.bluetooth.BluetoothProfile.ServiceListener#onServiceDisconnected(int)
	 */
	public void onServiceDisconnected(int profile) {
		
		if (profile == BluetoothProfile.A2DP ||
			profile == BluetoothProfile.HEADSET) {
			
			Log.d(getClass().getSimpleName(), "onServiceDisconnected");
		} else {
			Log.d(getClass().getSimpleName(), "Non A2DP BTPSDC");
			return;
		}
		
		Log.d(getClass().getSimpleName(), "A2DP Service disconnected");
		
	}
	
	/**
	 * Does either A2DP or headset connect based on device
	 */
	private void audioConnect() {
		
		int devClass = mConnectedDevice.getBluetoothClass().getDeviceClass();
		
		if (devClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
			devClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
			
			Log.d (getClass().getSimpleName(), "Connecting headset");
			
			//headsetConnect();
			a2dpConnect();
		} else {
			
			Log.d (getClass().getSimpleName(), "Connecting A2DP class "
				+ String.valueOf(devClass));
			
			a2dpConnect();
		}
	}
	
	/**
	 * Handles the A2DP connecting
	 */
	private void a2dpConnect () {
		
		String msg = new String();
		
		try {
			mTimer.cancel();
			IBluetoothA2dp a2dp = BtInterfaces.getA2dp();
			if (a2dp.getConnectionState(mConnectedDevice) ==
				BluetoothProfile.STATE_DISCONNECTED) {
				
				msg = getResources().getString(R.string.pair_connecting_str);
    			msg = msg.replaceAll("%1", getDeviceName());
    			mTimer.start();
    			a2dp.connect(mConnectedDevice);	
			} else {
				Log.d (getClass().getSimpleName(), "Already connected");
				finish();
			}
		} catch (Exception e) {
			Log.e (getClass().getSimpleName(), "Failed to A2DP connect:"
				+ e.getMessage());
		}
		
		showMessage (msg);
	}

	/* (non-Javadoc)
	 * @see fi.siika.bttagwriter.ConnectTimer.Listener#timerTick(int)
	 */
	public void timerTick(int secondsLeft) {
		int state = mBtMgr.getBluetoothAdapter().getProfileConnectionState(
			BluetoothProfile.A2DP);
		
		if (mAction == Actions.BOUNDING) {
			if (mConnectedDevice.getBondState() ==
				BluetoothDevice.BOND_BONDED) {
				
				Log.d (getClass().getSimpleName(), "Paired, now connect");
				mTimer.cancel();
				boundAndConnect();
			}
		
		} else if (mAction == Actions.CONNECTING &&
			state == BluetoothProfile.STATE_CONNECTED) {
			mTimer.cancel();
			finish();
		} else if (mAction == Actions.DISCONNECTING &&
			state == BluetoothProfile.STATE_DISCONNECTED) {
			mTimer.cancel();
			finish();
		} else if (secondsLeft == 0) {
			Log.e (getClass().getSimpleName(), "Failed in bluetooth action");
			String msg = new String();
			if (mAction == Actions.DISCONNECTING) {
				msg = getResources().getString(
					R.string.pair_failed_to_disconnect_str);
				msg = msg.replace("%1", getDeviceName());
			} else {
				msg = getResources().getString(
					R.string.pair_failed_to_connect_str);
				msg = msg.replace("%1", getDeviceName());
			}
			showMessage (msg);
			mTimer.cancel();
			finish();
		}
		
	}

	/* (non-Javadoc)
	 * @see fi.siika.bttagwriter.BluetoothManager.StateListener#bluetoothStateChanged(boolean)
	 */
	public void bluetoothStateChanged(boolean enabled) {
		if (enabled == true &&
			(mAction == Actions.CONNECTING || mAction == Actions.BOUNDING)) {
			
			boundAndConnect();
		}
	}
	
	/**
	 * Will show given message to user in UI
	 * @param msg Message shown. Ignored and not shown if empty.
	 */
	private void showMessage (String msg) {
		
		if (msg == null || msg.isEmpty()) {
			return;
		}
		
    	Log.d (getClass().getSimpleName(), msg);
    		
		TextView view = (TextView)findViewById(R.id.pairInfoTextView);
		view.setText(msg);
		
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.pair_toast,
			(ViewGroup) findViewById(R.id.pairToastLayout));
		TextView text = (TextView) layout.findViewById(R.id.pairToastText);
		text.setText(msg);
		
		Toast toast = new Toast(getApplicationContext());
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);

		toast.setView(layout);
		
		toast.show();
	}

}
