/**
 * BluetoothRowAdapter.java (bttagwriter)
 *
 * Copyright 2012 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */
package fi.siika.bttagwriter.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import fi.siika.bttagwriter.R;
import fi.siika.bttagwriter.managers.BluetoothManager;

/**
 * 
 */
public class BluetoothRowAdapter extends ArrayAdapter<Object> {
		
	private final List<BluetoothRow> list = new ArrayList<BluetoothRow>();
	private final Activity activity;
	private int discoveredColor;
	private int pairedColor;
	private Drawable unknownIcon;
	private Drawable audioIcon;

	public BluetoothRowAdapter(Activity activity) {
		super(activity, R.layout.bt_device_layout, R.id.btDeviceNameTextView);
		this.activity = activity;
	}
	
	public void setDiscoveredColor (int color) {
	    discoveredColor = color;
	}
	
	public void setPairedColor (int color) {
	    pairedColor = color;
    }
	
	public void setUnknownIcon (Drawable icon) {
        unknownIcon = icon;
    }
	
	public void setAudioIcon (Drawable icon) {
	    audioIcon = icon;
	}
	
	@Override
	public int getCount() {
		return list.size();
	}
	
	@Override
	public View getView (int position, View convertView, ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			LayoutInflater inflater = activity.getLayoutInflater();
			row = inflater.inflate(R.layout.bt_device_layout, null);
		}
		
		BluetoothRow rowData = list.get(position);
		TextView nameLine =(TextView)row.findViewById (R.id.btDeviceNameTextView);
		nameLine.setText (rowData.getName());
		TextView addressLine = (TextView)row.findViewById (R.id.btDeviceAddressTextView);
		ImageView image = (ImageView)row.findViewById(R.id.deviceTypeIcon);
		
		if (rowData.isAudio()) {
		    image.setImageDrawable(audioIcon);
		} else {
		    image.setImageDrawable(unknownIcon);
		}
		
		String addressValue = rowData.getAddress();
		if (rowData.isPaired()) {
		    addressValue =
				activity.getResources().getString(R.string.btscan_paired_str) + " "
				+ addressValue;
		}
		
		addressLine.setText (addressValue);
		
		int colorUsed = rowData.isDeviceVisible() ? discoveredColor : pairedColor;
	    nameLine.setTextColor(colorUsed);
	    addressLine.setTextColor(colorUsed);
		
		return(row);                         
	}
	
	public BluetoothRow getRow (int index) {
		return list.get(index);
	}
	
	@Override
	public void clear() {
		list.clear();
		notifyDataSetChanged();
	}
	
	public void clearNonAudio() {
	    for (int i = list.size() - 1; i >= 0; --i) {
	        if (list.get(i).isAudio()) {
	            break;
	        } else {
	            list.remove(i);
	        }
	    }
	    notifyDataSetChanged();
	}
	
	public void addDeviceIfNotPresent (BluetoothDevice device, boolean visible) {
	    
	    boolean isPaired = device.getBondState() == BluetoothDevice.BOND_BONDED;
	    boolean isAudio = BluetoothManager.isSuitableBluetoothDevice(device);
	    
	    BluetoothRow row = new BluetoothRow (device.getName(),
	            device.getAddress(), isPaired, isAudio);
	    row.setDeviceVisible(visible);
	    
		if (list.contains(row)) {
		    list.set(list.indexOf(row), row);
			return;
		}
		
		int location = 0;
		for (location = 0; location < list.size(); ++location) {
		    BluetoothRow old = list.get(location);
		    if (row.isAudio() && !old.isAudio()) {
		        break;
		    }
		    if (row.isPaired() && !old.isPaired()) {
                break;
            }
		}
		list.add(location, row);
		notifyDataSetChanged();
	}
}
