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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import fi.siika.bttagwriter.R;

/**
 * 
 */
public class BluetoothRowAdapter extends ArrayAdapter<Object> {
	
	public class Row {
		public String name;
		public String address;
		public boolean paired = false;
		
		public Row (String name, String address, boolean paired) {
			this.name = name;
			this.address = address;
			this.paired = paired;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((address == null) ? 0 : address.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Row other = (Row) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (address == null) {
				if (other.address != null) {
					return false;
				}
			} else if (!address.equals(other.address)) {
				return false;
			}
			return true;
		}

		private BluetoothRowAdapter getOuterType() {
			return BluetoothRowAdapter.this;
		}
		
		@Override
        public String toString() {
		    return address;
		}
		
	};
	
	private final List<Row> list;
	private final Activity activity;

	public BluetoothRowAdapter(Activity activity) {
		super(activity, R.layout.bt_device_layout, R.id.btDeviceNameTextView);
		this.activity = activity;
		list = new ArrayList<Row> ();
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
		
		Row rowData = list.get(position);
		TextView line =(TextView)row.findViewById (R.id.btDeviceNameTextView);
		line.setText (rowData.name);
		line =(TextView)row.findViewById (R.id.btDeviceAddressTextView);
		
		String addressLine = rowData.address;
		if (rowData.paired) {
			addressLine =
				activity.getResources().getString(R.string.btscan_paired_str) + " "
				+ addressLine;
		}
		
		line.setText (addressLine);
		return(row);                         
	}
	
	public Row getRow (int index) {
		return list.get(index);
	}
	
	@Override
	public void clear() {
		list.clear();
		notifyDataSetChanged();
	}
	
	public void addDeviceIfNotPresent (BluetoothDevice device) {
	    
	    Row row = new Row (device.getName(), device.getAddress(), device.getBondState() == BluetoothDevice.BOND_BONDED);
	    
		if (list.contains(row)) {
			return;
		}
		
		list.add(row);
		notifyDataSetChanged();
	}
}
