/**
 * BluetoothManager.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.managers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import java.util.Iterator;
import java.util.Set;

import fi.siika.bttagwriter.R;

/**
 * 
 */
public class BluetoothManager {
	
	public interface StateListener {
		public void bluetoothStateChanged(boolean enabled);
	}
	
	public interface DiscoveryListener {
	    /**
	     * 
	     * @param device
	     * @param fromPaired true if device was from paired list (and not proper discovery)
	     */
		public void bluetoothDeviceFound (BluetoothDevice device, boolean fromPaired);
		public void bluetoothDiscoveryStateChanged (boolean active);
	};
	
	private Context mContext = null;
	private BluetoothAdapter mBtAdapter = null;
	private boolean mEnabledBt = false;
	private DiscoveryListener mDiscoveryListener = null;
	private StateListener mStateListener = null;
	private boolean mReceiverConnected = false;
	private final static String TAG = "BluetoothManager";
	
	public BluetoothManager(Context context) {
		mContext = context;
	}
	
	public void setStateListener(StateListener listener) {
		mStateListener = listener;
	}
	
	private void connectReceiver() {
		if (mReceiverConnected == false) {
	        //setup broadcaster listener
	        IntentFilter filter = new IntentFilter ();
	        filter.addAction (BluetoothDevice.ACTION_FOUND);
	        filter.addAction (BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	        filter.addAction (BluetoothAdapter.ACTION_DISCOVERY_STARTED);
	        filter.addAction (BluetoothAdapter.ACTION_STATE_CHANGED);
			mContext.registerReceiver (mBCReceiver, filter);
			mReceiverConnected = true;
		}
	}
	
	public void releaseAdapter() {
		if (mReceiverConnected) {
			mContext.unregisterReceiver (mBCReceiver);
			mReceiverConnected = false;
		}
	}
	
	/*
	 * Get Bluetooth adapter.
	 */
	public BluetoothAdapter getBluetoothAdapter() {
		if (mBtAdapter == null) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		}
		
		connectReceiver();
		
		return mBtAdapter;
	}
	
	/**
	 * Is bluetooth connectivity enabled.
	 * @return true if connectivity is enabled
	 */
	public boolean isEnabled() {
		boolean enabled = false;
		BluetoothAdapter adapter = getBluetoothAdapter();
		if (adapter != null) {
			enabled = adapter.isEnabled();
		}
		return enabled;
	}
	
	/**
	 * This will call state listener's state changed function even if bluetooth
	 * was already enabled!
	 */
	public void enable() {
		BluetoothAdapter adapter = getBluetoothAdapter();
		if (isEnabled() == false) {
			Log.d(TAG, "Enable bluetooth");
			mEnabledBt = true;
			adapter.enable();
		} else {
			if (mStateListener != null) {
				mStateListener.bluetoothStateChanged(true);
			}
		}
	}
	
	private void browsePairedDevices() {
		BluetoothAdapter adapter = getBluetoothAdapter();
		Set<BluetoothDevice> paired = adapter.getBondedDevices();
		
		Iterator<BluetoothDevice>iter = paired.iterator();
		while (iter.hasNext()) {
			BluetoothDevice device = iter.next();
			
			 if (mDiscoveryListener != null) {
				 mDiscoveryListener.bluetoothDeviceFound(device, true);
			 }
		}
	}
	
	/*
	 * Start Bluetooth discovery (if not active)
	 */
	public boolean startDiscovery(DiscoveryListener listener) {
		mDiscoveryListener = listener;
		BluetoothAdapter adapter = getBluetoothAdapter();
		boolean success = true;
		
		if (adapter == null) {
			success = false;
		} else if (adapter.isEnabled() == false) {
			enable();
			Toast toast = Toast.makeText(mContext,
				R.string.toast_bluetooth_enabled_str, Toast.LENGTH_LONG);
			toast.show();
		} else if (adapter.isDiscovering() == false) {
			adapter.startDiscovery();
		    browsePairedDevices();
		}
		
		return success;
	}
	
	/**
	 * Disable bluetooth if it was originally enabled by BluetoothManager
	 */
	public void disableIfEnabled() {
		if (mBtAdapter != null) {
			if (mEnabledBt == true) {
				Log.d(TAG, "Disable bluetooth");
				mBtAdapter.disable();
				mEnabledBt = false;
			}
		}
	}
	
	public void stopDiscovery() {
		if (mBtAdapter != null) {
			if (mBtAdapter.isDiscovering()) {
				mBtAdapter.cancelDiscovery();
			}
			disableIfEnabled();
		}
	}
	
	private final BroadcastReceiver mBCReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				 BluetoothDevice device = intent.getParcelableExtra (
						 BluetoothDevice.EXTRA_DEVICE);
				
				 if (mDiscoveryListener != null) {
					 mDiscoveryListener.bluetoothDeviceFound(device, false);
				 }
			
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(
				action)) {
				
				//Workaround, first call of paired devices does not work if
				//Bluetooth was just enabled.
				//browsePairedDevices();
				
				if (mDiscoveryListener != null) {
					mDiscoveryListener.bluetoothDiscoveryStateChanged(false);
				}
				disableIfEnabled();
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				if (mDiscoveryListener != null) {
					mDiscoveryListener.bluetoothDiscoveryStateChanged(true);
				}
			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
					BluetoothAdapter.STATE_OFF);
				if (state == BluetoothAdapter.STATE_ON) {
					if (mDiscoveryListener != null) {
						browsePairedDevices();
						getBluetoothAdapter().startDiscovery();
					}
					if (mStateListener != null) {
						mStateListener.bluetoothStateChanged(true);
					}
				} else if (state == BluetoothAdapter.STATE_OFF) {
					mEnabledBt = false;
					if (mStateListener != null) {
						mStateListener.bluetoothStateChanged(false);
					}
				}
			}
		
		}
	};

}
