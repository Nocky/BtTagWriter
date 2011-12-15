/**
 * BluetoothManager.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter;

import fi.siika.bttagwriter.MainActivity.Pages;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

/**
 * 
 */
public class BluetoothManager {
	
	public interface Listener {
		public void bluetoothDeviceFound (BluetoothDevice device);
		public void bluetoothDiscoveryStateChanged (boolean active);
	};
	
	private Context mContext = null;
	private BluetoothAdapter mBtAdapter = null;
	private boolean mEnabledBt = false;
	private Listener mListener = null;
	
	public BluetoothManager(Context context, Listener listener) {
		mContext = context;
		mListener = listener;
	}
	
	/*
	 * Get Bluetooth adapter.
	 */
	public BluetoothAdapter getBluetoothAdapter() {
		if (mBtAdapter == null) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			
	        //setup broadcaster listener
	        IntentFilter filter = new IntentFilter ();
	        filter.addAction (BluetoothDevice.ACTION_FOUND);
	        filter.addAction (BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	        filter.addAction (BluetoothAdapter.ACTION_DISCOVERY_STARTED);
	        filter.addAction (BluetoothAdapter.ACTION_STATE_CHANGED);
	        mContext.registerReceiver (mBCReceiver, filter);
		}
		
		return mBtAdapter;
	}
	
	/*
	 * Start Bluetooth discovery (if not active)
	 */
	public boolean startDiscovery() {
		BluetoothAdapter adapter = getBluetoothAdapter();
		boolean success = true;
		
		if (adapter == null) {
			success = false;
		} else if (adapter.isEnabled() == false) {
			mEnabledBt = true;
			adapter.enable();
			Toast toast = Toast.makeText(mContext,
				R.string.toast_bluetooth_enabled_str, Toast.LENGTH_LONG);
			toast.show();
		} else if (adapter.isDiscovering() == false) {
			adapter.startDiscovery();
		}
		
		return success;
	}
	
	public void stopDiscovery() {
		if (mBtAdapter != null) {
			if (mBtAdapter.isDiscovering()) {
				mBtAdapter.cancelDiscovery();
			}
			if (mEnabledBt == true) {
				mBtAdapter.disable();
			}
		}
	}
	
	private final BroadcastReceiver mBCReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				 BluetoothDevice device = intent.getParcelableExtra (
						 BluetoothDevice.EXTRA_DEVICE);
				
				 if (mListener != null) {
					 mListener.bluetoothDeviceFound(device);
				 }
			
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(
				action)) {
				
				if (mListener != null) {
					mListener.bluetoothDiscoveryStateChanged(false);
				}
				if (mEnabledBt == true) {
					mEnabledBt = false;
					mBtAdapter.disable();
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				if (mListener != null) {
					mListener.bluetoothDiscoveryStateChanged(true);
				}
			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
					BluetoothAdapter.STATE_OFF);
				if (state == BluetoothAdapter.STATE_ON) {
					if (mEnabledBt) {
						getBluetoothAdapter().startDiscovery();
					}
				} else if (state == BluetoothAdapter.STATE_OFF) {
					mEnabledBt = false;
				}
			}
		
		}
	};

}
