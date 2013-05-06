package com.example.blufinder;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceListActivity extends Activity{
	
	
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    	setContentView(R.layout.device_list);
    	
    	setResult(Activity.RESULT_CANCELED);
    	
    	Button btnScan =(Button) findViewById(R.id.btnScan);
    	btnScan.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				doDiscovery();
				v.setVisibility(View.GONE);
				
			}
		});
    	
    	mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.device_name);
    	mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
    	
    	ListView pairedList = (ListView) findViewById(R.id.paired_devices);
    	pairedList.setAdapter(mPairedDevicesArrayAdapter);
    	pairedList.setOnItemClickListener(mDeviceClickListener);
    	
    	ListView otherDevices = (ListView) findViewById(R.id.other_devices);
    	otherDevices.setAdapter(mNewDevicesArrayAdapter);
    	otherDevices.setOnItemClickListener(mDeviceClickListener);
    	
    	IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    	registerReceiver(mReceiver, filter);
    	
    	mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    	Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
    	
    	 if (pairedDevices.size() > 0) {
             findViewById(R.id.text_new_devices).setVisibility(View.VISIBLE);
             for (BluetoothDevice device : pairedDevices) {
                 mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
             }
         } else {
             String noDevices = "Device is not paired currently";
             mPairedDevicesArrayAdapter.add(noDevices);
         }
    	
    	
    }
    
    private void doDiscovery()
    {
    	setProgressBarIndeterminateVisibility(true);
    	setTitle("Scanning");
    	findViewById(R.id.text_new_devices).setVisibility(View.VISIBLE);
    	if(mBtAdapter.isDiscovering())
    		mBtAdapter.cancelDiscovery();
    	
    	mBtAdapter.startDiscovery();
    }
    
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	
    	if(mBtAdapter.isDiscovering())
    		mBtAdapter.cancelDiscovery();
    	
    	unregisterReceiver(mReceiver);
    }
    
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int arg2,
				long arg3) {
			mBtAdapter.cancelDiscovery();
			
			String info = ((TextView)v).getText().toString();
			String address = info.substring(info.length()-17);
			
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
			
			setResult(RESULT_OK,intent);
			finish();
			
			
		}
	};
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			
			if(BluetoothDevice.ACTION_FOUND.equals(action))
			{
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(device.getBondState() != BluetoothDevice.BOND_BONDED)
					mNewDevicesArrayAdapter.add(device.getName()+"\n"+device.getAddress());
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
			{
				setProgressBarIndeterminateVisibility(false);
				setTitle("Please select any device to pair");
				
             if (mNewDevicesArrayAdapter.getCount() == 0) {
                 String noDevices = "Currently devices are unavailable to connect";
                 mNewDevicesArrayAdapter.add(noDevices);
             }
         }
			
		}
		
	};

}
