/**
 * MainActivity.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter;

import java.util.Iterator;
import java.util.Vector;

import fi.siika.bttagwriter.TagWriter.TagInformation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Build;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

/**
 * Main activity of BtTagWriter application
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class MainActivity extends Activity {
	
	private BluetoothAdapter mBtAdapter = null;
	private boolean mBtEnabled = false;
	
	public enum Pages {
		START(0), ABOUT(1), BT_SELECT(2), EXTRA_OPTIONS(3), TAG(4), SUCCESS(5);
		
		private int mValue;
		
		private Pages(int value) {
			mValue = value;
		}
		
		public int toInt() {
			return mValue;
		}
		
		public boolean equal(int value) {
			return toInt() == value;
		}
	};
	
	private void changeToPage (Pages page) {
		if (showFlipChild (page.toInt())) {
			if (page == Pages.BT_SELECT) {
				startBluetoothDiscovery();
			}
		}
	}
	
	private int currentPage() {
		ViewFlipper flip = (ViewFlipper)findViewById (R.id.mainFlipper);
		return flip.getDisplayedChild();
	}
	
	/*
	 * Get Bluetooth adapter.
	 */
	private BluetoothAdapter getBluetoothAdapter() {
		if (mBtAdapter == null) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		}
		
		return mBtAdapter;
	}
	
	/*
	 * Start Bluetooth discovery (if not active)
	 */
	private void startBluetoothDiscovery() {
		BluetoothAdapter adapter = getBluetoothAdapter();
		
		if (adapter == null) {
			showActionDialog(R.string.action_dialog_bluetooth_failed_str,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						changeToPage(Pages.START);
					}
				
				}, false, null);
			//mBtListAdapter.addDevice("Fake device", "00:00:00:00:00:00");
			return;
		} else if (adapter.isEnabled() == false) {
			mBtEnabled = true;
			adapter.enable();
		} else if (adapter.isDiscovering() == false) {
			adapter.startDiscovery();
		}
	}
	
	/*
	 * Stop Bluetooth discovery (if active)
	 */
	private void stopBluetoothDiscovery() {
		if (mBtAdapter != null) {
			if (mBtAdapter.isDiscovering()) {
				mBtAdapter.cancelDiscovery();
			}
		}
	}
	
	private NfcAdapter mNfcReader = null;
	
	private NfcAdapter getNfcReader() {
		
		//TODO: Remove this
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
			return null;
		} else if (mNfcReader == null) {
			mNfcReader = NfcAdapter.getDefaultAdapter(this);
		}
		
		return mNfcReader;
	}
	
	/*
	 * Enable NFC reader parts of software
	 */
	private void enableNfcReader() {
		NfcAdapter nfcAdapter = getNfcReader();
		if (nfcAdapter == null) {
			return;
		}
		
		if (nfcAdapter.isEnabled() == false) {
			showActionDialog(R.string.action_dialog_nfc_not_enabled_str,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						startActivityForResult(new Intent (
							android.provider.Settings.ACTION_WIRELESS_SETTINGS),
							0);
					}
				
				}, false, null);
			return;
		}
		
		/*
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
			new Intent(this,getClass()).addFlags(
			Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		
		IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
	    try {
	        tech.addDataType("* / *");
	    } catch (MalformedMimeTypeException e) {
	        throw new RuntimeException("fail", e);
	    }

		String[][] techList = new String[][] { new String[] {
			MifareUltralight.class.getName() } };

		nfcAdapter.enableForegroundDispatch(this, pendingIntent,
			new IntentFilter[] { tech }, techList);
		*/

	}
	
	/*
	 * Disable NFC reader parts of software
	 */
	private void disableNfcReader() {
		
		//TODO: Remove this
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1) {
			return;
		} else if (mNfcReader != null && mNfcReader.isEnabled()) {
			//mNfcReader.disableForegroundDispatch (this);
		}
	}
	
	/*
	 * Catch broadcasted messages here
	 */
	private final BroadcastReceiver mBCReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra (
					BluetoothDevice.EXTRA_DEVICE);
				mBtListAdapter.addDeviceIfNotPresent (device);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				ProgressBar pb = (ProgressBar)findViewById (R.id.btScanProgressBar);
				pb.setIndeterminate (false);
				pb.setVisibility(View.INVISIBLE);
				getBluetoothAdapter().disable();
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				ProgressBar pb = (ProgressBar)findViewById (R.id.btScanProgressBar);
				pb.setIndeterminate (true);
				pb.setVisibility(View.VISIBLE);
			} else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
				CharSequence text = "Hello Tech!";
				Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
				toast.show();
			} else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
				CharSequence text = "Hello Tag!";
				Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
				toast.show();
			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
					BluetoothAdapter.STATE_OFF);
				//TODO check if our ui is in state where it should be
				if (state == BluetoothAdapter.STATE_ON) {
					if (mBtEnabled) {
						getBluetoothAdapter().startDiscovery();
					}
				} else if (state == BluetoothAdapter.STATE_OFF) {
					mBtEnabled = false;
				}
			}
		}        
	};
	
	/**
	 * Function that will handle things that should be done after user moves
	 * away from different child views of flip. Called by showFlipChild.
	 * @param index Index of child that was replaced with something else
	 */
	private void flipChildHidden (int index) {
		if (Pages.BT_SELECT.equal(index)) {
			stopBluetoothDiscovery();
		}
	}
	
	/***
	 * Call this when changing the shown child of flip page. Will selected
	 * the correct animation depending on current and selected child.
	 * @param index Index of child shown next
	 * @return true if shown child was changed
	 */
	private boolean showFlipChild (int index) {
		ViewFlipper flip = (ViewFlipper)findViewById (R.id.mainFlipper);
		int current = flip.getDisplayedChild();
		
		if (current == index) {
			return false;
		} else if (current < index) {
			flip.setInAnimation (this, R.animator.in_left_anim);
			flip.setOutAnimation (this, R.animator.out_right_anim);
		} else {
			flip.setInAnimation (this, R.animator.in_right_anim);
			flip.setOutAnimation (this, R.animator.out_left_anim);
		}
		
		flipChildHidden (current);
		flip.setDisplayedChild (index);
		return true;
	}
	
	private OnClickListener mStartButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			mBtListAdapter.clear();
			changeToPage (Pages.BT_SELECT);
		}
	};
	
	private OnClickListener mAboutButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			changeToPage (Pages.ABOUT);
		}
	};
	
	private OnClickListener mExtraoptsReadyButtonListener =
		new OnClickListener() {
		
		public void onClick(View v) {
			
			changeToPage (Pages.TAG);
		}
	};
	
	private OnClickListener mExitButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			finish();
		}
	};
	
	//row adapter for bluetooth list
	public class BluetoothRowAdapter extends ArrayAdapter<Object> {
		
		public class Row {
			public String name;
			public String address;
		};
		
		private Vector<Row> mList;

		public BluetoothRowAdapter(Context context) {
			super(context, R.layout.bt_device_layout, R.id.btDeviceNameTextView);
			
			mList = new Vector<Row> ();
		}
		
		@Override
		public int getCount() {
			return mList.size();
		}
		
		@Override
		public View getView (int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.bt_device_layout, null);
			}
			
			Row rowData = mList.elementAt(position);
			TextView line =(TextView)row.findViewById (R.id.btDeviceNameTextView);
			line.setText (rowData.name);
			line =(TextView)row.findViewById (R.id.btDeviceAddressTextView);
			line.setText (rowData.address);
			return(row);                         
		}
		
		public void addDevice(String name, String address) {
			Row row = new Row();
			row.name = name;
			row.address = address;
			mList.add(row);
			notifyDataSetChanged();
		}
		
		public void addDevice(BluetoothDevice device) {
			addDevice (device.getName(), device.getAddress());
		}
		
		public Row getRow (int index) {
			return mList.elementAt(index);
		}
		
		public void clear() {
			mList.clear();
			notifyDataSetChanged();
		}
		
		public void addDeviceIfNotPresent (BluetoothDevice device) {
			Iterator<Row> iter = mList.iterator();
			while (iter.hasNext()) {
				Row row = iter.next();
				if (row.address.equals(device.getAddress())) {
					return;
				}
			}
			addDevice(device);
		}
	}
	private BluetoothRowAdapter mBtListAdapter = null;
   
	protected void connectSignals() {
		Button button = (Button)findViewById (R.id.startButton);
		button.setOnClickListener (mStartButtonListener);
		
		button = (Button)findViewById (R.id.restartButton);
		button.setOnClickListener (mStartButtonListener);
		
		button = (Button)findViewById (R.id.extraoptsReadyButton);
		button.setOnClickListener (mExtraoptsReadyButtonListener);
		
		button = (Button)findViewById (R.id.exitButton);
		button.setOnClickListener(mExitButtonListener);
		
		button = (Button)findViewById (R.id.aboutButton);
		button.setOnClickListener(mAboutButtonListener);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if(mBtEnabled) {
			getBluetoothAdapter().disable();
			mBtEnabled = false;
		}
		
		disableNfcReader();
		stopBluetoothDiscovery();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mBtEnabled = false;
        
        if (mBtListAdapter == null) {
        	mBtListAdapter = new BluetoothRowAdapter(this);
        }
        ListView list = (ListView)findViewById (R.id.btDevicesList);
        list.setAdapter (mBtListAdapter);
        
        list.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        		int position, long id) {
        		
        		StringBuilder sbuilder = new StringBuilder();
        		sbuilder.append(mBtListAdapter.getRow(position).name);
        		sbuilder.append(" (");
        		sbuilder.append(mBtListAdapter.getRow(position).address);
        		sbuilder.append(")");
        		
        		TextView tview = (TextView)findViewById (R.id.extraoptsSelectedDeviceValue);
        		tview.setText(sbuilder.toString());
        		
        		changeToPage (Pages.EXTRA_OPTIONS);
        	}
        });
        
        connectSignals();
        
        //setup broadcaster listener
        IntentFilter filter = new IntentFilter ();
        filter.addAction (BluetoothDevice.ACTION_FOUND);
        filter.addAction (BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction (BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction (NfcAdapter.ACTION_TECH_DISCOVERED);
        filter.addAction (NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction (BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver (mBCReceiver, filter);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	
    	boolean ret = false;
    	
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
        	int curPage = currentPage();
        	
        	if (Pages.ABOUT.equal(curPage)) {
        		changeToPage(Pages.START);
        	} else if (Pages.BT_SELECT.equal(curPage)) {
        		changeToPage(Pages.START);
        	} else if (Pages.EXTRA_OPTIONS.equal(curPage)) {
        		changeToPage(Pages.BT_SELECT);
        	} else if (Pages.TAG.equal(curPage)) {
        		changeToPage(Pages.EXTRA_OPTIONS);
        	} else {
        		ret = super.onKeyDown(keyCode, event);
        	}
        } else {
        	ret = super.onKeyDown(keyCode, event);
        }
        return ret;
    }
    
    private void showActionDialog (int textResId, 
    	DialogInterface.OnClickListener clickListener,
    	boolean cancelable,
    	DialogInterface.OnCancelListener cancelListener) {
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(textResId);
    	
    	AlertDialog dialog = builder.create();
    	
    	dialog.setCancelable(cancelable);
    	
    	if (clickListener != null) {
    		dialog.setButton(getResources().getText(R.string.action_dialog_ok),
    			clickListener);
    	}
    	
    	dialog.show();
    	
    }


}