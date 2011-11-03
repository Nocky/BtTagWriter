package fi.siika.bttagwriter;

import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MainActivity extends Activity {
	
	private OnClickListener mStartButtonListener = new OnClickListener() {
		public void onClick(View v) {
			
			ViewFlipper flip = (ViewFlipper)findViewById (R.id.mainFlipper);
			flip.setDisplayedChild(1);
			
			mBtListAdapter.addDevice("My fake device", "00:00:00:00:00:00:00:00");
			
		}
	};
	
	private void addBluetoothDevice (String name, String address) {
		ListView list = (ListView)findViewById (R.id.btDevicesList);
	}
	
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
		}
	}
	private BluetoothRowAdapter mBtListAdapter = null;
	
	
	//row adapter for writer list
	public class StepRowAdapter extends ArrayAdapter<Object> {
		public class Row {
			public boolean done = false;
			public String name;
			public String info;
		};
		
		private Vector<Row> mList;
		
		public StepRowAdapter(Context context) {
			super(context, R.layout.step_layout, R.id.stepNameTextView);
			
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
				row = inflater.inflate(R.layout.step_layout, null);
			}
			
			Row rowData = mList.elementAt(position);
			TextView line =(TextView)row.findViewById (R.id.stepNameTextView);
			line.setText (rowData.name);
			
			line =(TextView)row.findViewById (R.id.stepInfoTextView);
			line.setText (rowData.info);
			
			if (rowData.done) {
				ImageView image = (ImageView)row.findViewById(R.id.stepImageView);
				image.setImageDrawable(getResources().getDrawable(R.drawable.done));
				
			}
			
			return(row);                         
		}
		
		public void addStep(String name, String info) {
			Row row = new Row();
			row.name = name;
			row.info = info;
			row.done = false;
			
			mList.add(row);
		}
		
		public void setActiveStep (int index) {
			for (int i = 0; i <= index; ++i) {
				mList.elementAt(i).done = true;
			}
			for (int i = index + 1; i < mList.size(); ++i) {
				mList.elementAt(i).done = false;
			}
		}
	
	};
	private StepRowAdapter mStepListAdapter = null;
   
	protected void connectSignals() {
		Button button = (Button)findViewById (R.id.startButton);
		button.setOnClickListener (mStartButtonListener);
		
		button = (Button)findViewById (R.id.restartButton);
		button.setOnClickListener (mStartButtonListener);
		
		ViewFlipper flip = (ViewFlipper)findViewById (R.id.mainFlipper);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ListView list = (ListView)findViewById (R.id.btDevicesList);
        if (mBtListAdapter == null) {
        	mBtListAdapter = new BluetoothRowAdapter(this);
        }
        list.setAdapter (mBtListAdapter);
        
        list.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        		int position, long id) {
        		
    			ViewFlipper flip = (ViewFlipper)findViewById (R.id.mainFlipper);
    			flip.setDisplayedChild(2);

        	}
        });
        
        list = (ListView)findViewById (R.id.stepsList);
        if (mStepListAdapter == null) {
        	mStepListAdapter = new StepRowAdapter(this);
        }
        list.setAdapter (mStepListAdapter);
        mStepListAdapter.addStep(
        	getResources().getString(R.string.step_bt_device_str),
        	"00:00:00:00:00:00");
        mStepListAdapter.addStep(
        	getResources().getString(R.string.step_tag_str),
        	getResources().getString(R.string.step_tag_info_str));
        mStepListAdapter.addStep(
            	getResources().getString(R.string.step_write_str),
            	getResources().getString(R.string.step_write_info_wait_str));
        mStepListAdapter.setActiveStep(0);
        
        list.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        		int position, long id) {
        		
    			ViewFlipper flip = (ViewFlipper)findViewById (R.id.mainFlipper);
    			flip.setDisplayedChild(3);

        	}
        });
        
        connectSignals();
    }
}