/**
 * MainActivity.java (bttagwriter)
 *
 * Copyright 2011 Sami Viitanen <sami.viitanen@gmail.com>
 * All rights reserved.
 */

package fi.siika.bttagwriter;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import fi.siika.bttagwriter.data.TagInformation;
import fi.siika.bttagwriter.managers.BluetoothManager;
import fi.siika.bttagwriter.managers.NfcManager;
import fi.siika.bttagwriter.ui.BluetoothRowAdapter;
import fi.siika.bttagwriter.ui.Pages;
import fi.siika.bttagwriter.writers.TagWriter;

/**
 * Main activity of BtTagWriter application
 * @author Sami Viitanen <sami.viitanen@gmail.com>
 */
public class WriterActivity extends Activity implements
	BluetoothManager.DiscoveryListener {
	
	private final static String TAG = "WriterActivity";

	private TagWriter mTagWriter;
	private Handler mTagWriterHandler; 
	private final TagInformation mTagInfo = new TagInformation();
	private BluetoothManager mBtMgr;
	private NfcManager mNfcMgr;
	private int lastPage;
	
	private void setCurrentPage (Pages page) {
	    setCurrentPage(page.toInt());
	}
	
	private void setCurrentPage(int page) {
        int pageWas = getCurrentPage();
        if (showFlipChild (page)) {
            lastPage = pageWas;
            if (Pages.BT_SELECT.equal(page)) {
                startBluetoothDiscovery();
            } else if (Pages.TAG.equal(page)) {
                mNfcMgr.enableTechDiscovered();
            }
            invalidateOptionsMenu();
        }	    
	}
	
	private int getCurrentPage() {
		ViewFlipper flip = (ViewFlipper)findViewById (R.id.mainFlipper);
		return flip.getDisplayedChild();
	}
	
	/*
	 * Start Bluetooth discovery (if not active)
	 */
	private void startBluetoothDiscovery() {
		
		if (mBtMgr.startDiscovery(this) == false) {
			showActionDialog(R.string.action_dialog_bluetooth_failed_str,
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						setCurrentPage(Pages.START);
					}
				
				}, false, null);
		}
	}
	
	/**
	 * Function that will handle things that should be done after user moves
	 * away from different child views of flip. Called by showFlipChild.
	 * @param index Index of child that was replaced with something else
	 */
	private void flipChildHidden (int index) {
		if (Pages.BT_SELECT.equal(index)) {
			mBtMgr.stopDiscovery();
		} else if (Pages.TAG.equal(index)) {
			mNfcMgr.disableForegroundDispatch();
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
		} else if (Pages.ABOUT.equal(current)) {
            flip.setInAnimation(this, R.animator.fade_in_anim);
            flip.setOutAnimation (this, R.animator.out_jump_anim);
		} else if (Pages.ABOUT.equal(index)) {
            flip.setInAnimation (this, R.animator.in_jump_anim);
            flip.setOutAnimation(this, R.animator.fade_out_anim);
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
	
	private final OnClickListener mStartButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			mBtListAdapter.clear();
			setCurrentPage (Pages.BT_SELECT);
		}
	};
	
	private final OnClickListener mRescanButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			mBtMgr.startDiscovery(WriterActivity.this);
		}
	};
	
	private final OnClickListener mExtraoptsReadyButtonListener =
		new OnClickListener() {
		
		public void onClick(View v) {
			
			//Read extra options
			
			CheckBox cbox = (CheckBox)findViewById(R.id.readOnlycheckBox);
			if (cbox != null) {
				mTagInfo.readOnly = cbox.isChecked();
			} else {
				mTagInfo.readOnly = false;
			}
			
			EditText editText = (EditText)findViewById(R.id.extraoptsPinEdit);
			if (editText != null) {
				mTagInfo.pin = editText.getText().toString();
			} else {
				mTagInfo.pin = "";
			}

			setCurrentPage (Pages.TAG);
		}
	};
	
	private final OnClickListener mExitButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			finish();
		}
	};
	
	private BluetoothRowAdapter mBtListAdapter = null;
   
	protected void connectSignals() {
		Button button = (Button)findViewById (R.id.startButton);
		button.setOnClickListener (mStartButtonListener);
		
		button = (Button)findViewById (R.id.restartButton);
		button.setOnClickListener (mExtraoptsReadyButtonListener);
		
		button = (Button)findViewById (R.id.extraoptsReadyButton);
		button.setOnClickListener (mExtraoptsReadyButtonListener);
		
		button = (Button)findViewById (R.id.exitButton);
		button.setOnClickListener(mExitButtonListener);
		
		ImageButton ib = (ImageButton)findViewById (R.id.btRescanButton);
		ib.setOnClickListener(mRescanButtonListener);
	}
	
	private final DialogInterface.OnClickListener mWriteFailedDialogListener =
		new DialogInterface.OnClickListener() {

		public void onClick(DialogInterface dialog, int which) {
		}
		
	};
	
	@Override
	public void onResume() {
		super.onResume();
		
		mTagWriterHandler = new Handler() {
			@Override
			public void handleMessage (Message msg) {
				switch (msg.what) {
				case TagWriter.HANDLER_MSG_SUCCESS:
					setCurrentPage(Pages.SUCCESS);
					break;
				case TagWriter.HANDLER_MSG_CANCELLED:
					break;
				case TagWriter.HANDLER_MSG_TOO_SMALL:
					showActionDialog(R.string.tag_is_too_small_str,
						mWriteFailedDialogListener, false, null);
					break;
				default:
					showActionDialog(R.string.tag_write_failed_str,
						mWriteFailedDialogListener, false, null);
				}
			}
		};
		
		mTagWriter = new TagWriter (mTagWriterHandler);
	}
	
	@Override
	public void onPause() {
		mBtMgr.releaseAdapter();	
		super.onPause();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mBtMgr = new BluetoothManager(this);
        mNfcMgr = new NfcManager (this);
        
        if (mBtListAdapter == null) {
        	mBtListAdapter = new BluetoothRowAdapter(this);
        }
        ListView list = (ListView)findViewById (R.id.btDevicesList);
        list.setAdapter (mBtListAdapter);
        
        list.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        		int position, long id) {
        		
        		mTagInfo.name = mBtListAdapter.getRow(position).name;
        		mTagInfo.address = mBtListAdapter.getRow(position).address;
        		
        		StringBuilder sbuilder = new StringBuilder();
        		sbuilder.append(mBtListAdapter.getRow(position).name);
        		sbuilder.append(" (");
        		sbuilder.append(mBtListAdapter.getRow(position).address);
        		sbuilder.append(")");
        		
        		TextView tview = (TextView)findViewById (
        			R.id.extraoptsSelectedDeviceValue);
        		tview.setText(sbuilder.toString());
        		
        		setCurrentPage (Pages.EXTRA_OPTIONS);
        	}
        });
        
        //Setting HTML to text views has to be done in code. Do it here.
        TextView appTV = (TextView)findViewById (R.id.appDescriptionTextView);
        appTV.setText (Html.fromHtml (getString (
        	R.string.about_info_str)));
        
        TextView limitTV = (TextView)findViewById (R.id.limitationsTextView);
        limitTV.setText (Html.fromHtml (getString (
        	R.string.about_issues_str)));
        
        TextView credsTV = (TextView)findViewById (R.id.creditsTextView);
        credsTV.setText (Html.fromHtml (getString (
        	R.string.about_credits_str)));
        
        TextView eonTV = (TextView)findViewById (R.id.extraoptsNoticeCaption);
        eonTV.setText (Html.fromHtml (getString (
        	R.string.extraopts_notice_str)));
        
        TextView linksTV = (TextView)findViewById (R.id.linksTextView);
        linksTV.setText (Html.fromHtml (getString (
        	R.string.about_links_str)));
        
        connectSignals();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (Pages.ABOUT.equal(getCurrentPage())) {
                setCurrentPage(lastPage);
                return true;
            }
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    private boolean toPrevPage() {
        
        int curPage = getCurrentPage();
        boolean ret = true;
        
        if (Pages.ABOUT.equal(curPage)) {
            setCurrentPage(Pages.START);
        } else if (Pages.BT_SELECT.equal(curPage)) {
            setCurrentPage(Pages.START);
        } else if (Pages.EXTRA_OPTIONS.equal(curPage)) {
            setCurrentPage(Pages.BT_SELECT);
        } else if (Pages.TAG.equal(curPage)) {
            setCurrentPage(Pages.EXTRA_OPTIONS);
        } else {
            ret = false;
        }
        
        return ret;
    }
    
    @Override
    public void onNewIntent(Intent intent) {
    	setIntent(intent);
    	String action = intent.getAction();
    	
    	
    	if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
    		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);    		
    		if (mTagWriter.writeToTag(tag, mTagInfo) == false) {
    			showActionDialog(R.string.tag_unsupported_str,
    				mWriteFailedDialogListener, false, null);
    		}

    	} else {
        	Toast toast = Toast.makeText(this, action, 
        		Toast.LENGTH_SHORT);
        	toast.show();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int page = getCurrentPage();
        menu.findItem(R.id.backItem).setVisible (!Pages.START.equal(page)
                && !Pages.SUCCESS.equal(page) && !Pages.ABOUT.equal(page));
        menu.findItem(R.id.aboutItem).setVisible(!Pages.ABOUT.equal(page));
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aboutItem:
                setCurrentPage (Pages.ABOUT);
                return true;
            case R.id.backItem:
                toPrevPage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void showActionDialog (int textResId, 
    	DialogInterface.OnClickListener clickListener,
    	boolean cancelable,
    	DialogInterface.OnCancelListener cancelListener) {
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(textResId);
    	
    	AlertDialog dialog = builder.create();
    	
    	if (clickListener != null) {
    		dialog.setCancelable(cancelable);
    		dialog.setButton(AlertDialog.BUTTON_POSITIVE,
    			getResources().getText(R.string.action_dialog_ok),
    			clickListener);
    		
    	} else {
    		dialog.setCancelable(true);
    	}
    	
    	if (cancelListener != null) {
    		dialog.setOnCancelListener(cancelListener);
    	}
    	
    	dialog.show();
    	
    }

	/* (non-Javadoc)
	 * @see fi.siika.bttagwriter.BluetoothManager.Listener#bluetoothDeviceFound(android.bluetooth.BluetoothDevice)
	 */
	public void bluetoothDeviceFound(BluetoothDevice device) {
		
		BluetoothClass btClass = device.getBluetoothClass();
		
		if (btClass == null) {
			Log.w(TAG, "null from BluetoothDevice.getBluetoohClass");
			return;
		}
		
		//For now filter out everything but audio
		if (btClass.hasService(BluetoothClass.Service.AUDIO) == false) {
			return;
		}
		
		//Check that it is not headset (can not get those connected)
		int devClass = btClass.getDeviceClass();
		if (devClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
			devClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
			return;
		}
			
		mBtListAdapter.addDeviceIfNotPresent (device);
		
	}

	/* (non-Javadoc)
	 * @see fi.siika.bttagwriter.BluetoothManager.Listener#bluetoothDiscoveryStateChanged(boolean)
	 */
	public void bluetoothDiscoveryStateChanged(boolean active) {
		ProgressBar pb = (ProgressBar)findViewById (R.id.btScanProgressBar);
		pb.setIndeterminate (active);
		
		ViewFlipper flip = (ViewFlipper)findViewById (
			R.id.btScanActionsFlipper);

		if (active) {
			flip.setDisplayedChild(0);
			pb.setVisibility(View.VISIBLE);
			
			Toast toast = Toast.makeText(this, R.string.btscan_started_str,
				Toast.LENGTH_LONG);
			toast.show();
			
			TextView nodevText = (TextView)findViewById (
				R.id.btScanNoDevicesFoundText);
			if (nodevText != null) {
				nodevText.setVisibility(View.GONE);
			}
			
		} else {
			flip.setDisplayedChild(1);
			pb.setVisibility(View.INVISIBLE);
			
			if (this.mBtListAdapter.getCount() == 0) {
				TextView nodevText = (TextView)findViewById (
					R.id.btScanNoDevicesFoundText);
				if (nodevText != null) {
					nodevText.setVisibility(View.VISIBLE);
				}
			}
			
		}
		
	}


}