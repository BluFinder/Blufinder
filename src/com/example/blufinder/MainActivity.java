package com.example.blufinder;

import java.io.File;
import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	// Constants
	final private String SETTING = "Setting";			// User settings
	final private String RINGTONE_BUTTON_KEY = "Ringtone";
	final private String RINGTONE_CHECK_KEY = "RingtoneChecked";
	final private String VIBRATE_CHECK_KEY = "VibrateChecked";
	final private String RINGTONE_FOLDER = "/system/media/audio/ringtones";
	
	private BluetoothAdapter mBluetoothAdapter = null;	//Bluetooth Adapter
	private Intent serviceIntent;
	
	private Button rtButton ;
	private Button helpButton ;
	private ToggleButton serviceButton;
	private CheckBox rtCheck ;
	private CheckBox vbCheck ;
	
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_RINGTONE_CODE = 4;
 
    private String mConnectedDeviceName = null;
    private ManageBluetooth mBluetoothManage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Widgets
		rtButton = (Button) findViewById(R.id.ringtoneButton);
		helpButton = (Button) findViewById(R.id.helpButton);
		serviceButton= (ToggleButton) findViewById(R.id.serviceButton);
		rtCheck = (CheckBox)findViewById(R.id.ringtoneCheck);
		vbCheck = (CheckBox)findViewById(R.id.vibrateCheck);

		// Set checkbox status-----moved this block to onResume method ---sheetal

		//Set bluetooth Adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		
		// ringtoneButton listener
		rtButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	if(existFolder(RINGTONE_FOLDER)){
            		// Open ringtone selector
            		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone");
					intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getRingtoneUri());
					// Return ringtone URI to this activity
					startActivityForResult(intent, REQUEST_RINGTONE_CODE);
            	}
            }
        });
		
		// serviceButton listener
		serviceButton.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {  
            @Override  
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            	
            	saveToggleStatus(serviceButton.isChecked());
            	
                if (isChecked) 
                {
                	// start service
                		//Check whether Bluetooth is paired to any device.
                		//Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        				if (!mBluetoothAdapter.isEnabled()) 
			    	  	 	 ensureDiscoverable();		    	  	 	
        				if(mBluetoothAdapter.isEnabled() && mBluetoothManage == null)	
        					setupConnection();
			    	    if(mBluetoothAdapter.isEnabled() && mBluetoothManage != null)
			    	    {
		    				//Create Intent with checkboxes state and pass it to Service
		    				serviceIntent = new Intent(getBaseContext(),BluetoothAlertService.class);
		    				serviceIntent.putExtra("RINGTONE_CHECK_KEY",getCheckBoxStatus(RINGTONE_CHECK_KEY) );
		    				serviceIntent.putExtra("VIBRATE_CHECK_KEY", getCheckBoxStatus(VIBRATE_CHECK_KEY));
		    				startService(serviceIntent);
		    				Toast.makeText(getBaseContext(), "Service Started", Toast.LENGTH_SHORT).show();
			    	    }
			    	    else
			    	    {
			    	    	buttonView.setChecked(false);
			    	    }
        				
        		 } else {
                	 // stop service
        			 if(serviceIntent != null)
        			 {
        				 stopService(serviceIntent);
        				 Toast.makeText(getBaseContext(), "Service Stoped", Toast.LENGTH_SHORT).show();
        			 }
                }
            }
        
        });
		
		// helpButton listener
		helpButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	// Show help info
            	Intent helpActivity = new Intent(MainActivity.this, HelpActivity.class);
            	startActivity(helpActivity);
            	overridePendingTransition(R.anim.animation_enter,
                        R.anim.animation_leave);
            }
        
        });
		
		// ringtone checkbox listener
		rtCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				saveCheckBoxStatus(RINGTONE_CHECK_KEY, isChecked);
				
				Intent ringtone_intent = new Intent("RINGTONE_CHECKED_CHANGE");
				ringtone_intent.putExtra("RINGTONE_CHECK_KEY", isChecked);
				sendBroadcast(ringtone_intent);
			}
		});
		
		// vibrate checkbox listener
		vbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				saveCheckBoxStatus(VIBRATE_CHECK_KEY, isChecked);
				
				Intent vibrate_intent = new Intent("VIBRATE_CHECKED_CHANGE");
				vibrate_intent.putExtra("VIBRATE_CHECK_KEY", isChecked);
				sendBroadcast(vibrate_intent);
			}
		});
		
	}
	
	@Override
    public synchronized void onResume() {
        super.onResume();
       

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothManage != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothManage.getState() == ManageBluetooth.STATE_NONE) {
              // Start the Bluetooth chat services
              mBluetoothManage.start();
            }
        }
        
      //Get values from shared preferences
		rtCheck.setChecked(getCheckBoxStatus(RINGTONE_CHECK_KEY));
		vbCheck.setChecked(getCheckBoxStatus(VIBRATE_CHECK_KEY));

		if(mBluetoothAdapter.isEnabled())
			serviceButton.setChecked(getToggleStatus());
		else
			serviceButton.setChecked(false);
    }
    
	 private void ensureDiscoverable() 
    {
    	  
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
        
    }
	
    private void setupConnection()
    {
    	Intent serverIntent = null;
    	
    	serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
           

    	mBluetoothManage = new ManageBluetooth();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	switch (requestCode) {
    	
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            else
            {
            	Toast.makeText(this, "Connection to remote device failed", Toast.LENGTH_LONG).show();
            	serviceButton.toggle();
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            else
            {
            	Toast.makeText(this, "Connection to remote device failed", Toast.LENGTH_LONG).show();
            	serviceButton.toggle();
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) 
            {
                 //Setup Connection
            	 setupConnection();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.w("Log", "BT not enabled");
                Toast.makeText(this, "Bluetooth not Enabled", Toast.LENGTH_SHORT).show();
            }
            break;
        case REQUEST_RINGTONE_CODE:
            // When the request to Select Ringtones returns
            if (resultCode == Activity.RESULT_OK) 
            {
            	try
        		{
        			// Get ringtone Uri
        			Uri pickedUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        			if (pickedUri != null)
        			{
        				// Save ringtone only for this app
        				saveRingtoneUri(pickedUri);
        			}
        		}
        		catch (Exception e)
        		{
        			Toast.makeText(this, "Error during selecting Ringtone", Toast.LENGTH_LONG).show();
        			return;
        		}
        	}else {
                //an error occurred while selecting ringtone
        		Toast.makeText(this, "Error during selecting Ringtone", Toast.LENGTH_LONG).show();
    			return;
            }
            
        }
    	
    	
    	
    }
    
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBluetoothManage.start();
        mBluetoothManage.connect(device, secure);
    }

	
	// Check if folder exists
	private boolean existFolder(String folder) {
		boolean exist = false;
		File f = new File(folder);
		if (!f.exists())
		{
			if (f.mkdirs()){exist = true;}
			else{exist = false;}
		}
		else{
			exist = true;
		}
		return exist;
	}
	
	// Get checkbox status
	private boolean getCheckBoxStatus(String key){
		boolean status;
		Context cxt = getBaseContext();
        SharedPreferences value = cxt.getSharedPreferences(SETTING, MODE_PRIVATE);
        status = value.getBoolean(key, true);
		return status;
	}
	
	// Save checkbox status
	private void saveCheckBoxStatus(String key, boolean checked){
		Context ctx = MainActivity.this;
		SharedPreferences sta = ctx.getSharedPreferences(SETTING, MODE_PRIVATE);
		SharedPreferences.Editor editor = sta.edit();
		editor.putBoolean(key, checked);
		editor.commit();
	}
	
	// Save ToggleButton status
	private void saveToggleStatus(boolean checked){
		Context ctx = getBaseContext();
		SharedPreferences sta = ctx.getSharedPreferences(SETTING, MODE_PRIVATE);
		SharedPreferences.Editor editor = sta.edit();
		editor.putBoolean("TOGGLE_STATE", checked);
		editor.commit();
	}

	
	private boolean getToggleStatus(){
		boolean status;
		Context cxt = getBaseContext();
        SharedPreferences rtone = cxt.getSharedPreferences(SETTING, MODE_PRIVATE);
        status = rtone.getBoolean("TOGGLE_STATE", false);
		return status;
	}
	
	// Get current ringtone----Moved the method getRingtoneUri to service class --Sheetal
	// Still need this method here to set default ringtone when open Ringtone Dialog  --Yachen
	public Uri getRingtoneUri(){
		Uri rtUri;
		String ringtoneUri = "";
		Context cxt = getBaseContext();
        SharedPreferences rtone = cxt.getSharedPreferences(SETTING, MODE_PRIVATE);
        ringtoneUri = rtone.getString(RINGTONE_BUTTON_KEY, "");
		if(ringtoneUri == ""){	//if no ringtone data found
			//get system default ringtone
			rtUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		}
		else{
			rtUri = Uri.parse(ringtoneUri);
		}
		return rtUri;
	}
	
	// Save ringtone Uri
	private void saveRingtoneUri(Uri pickedUri){
		Context ctx = MainActivity.this;
		SharedPreferences rtone = ctx.getSharedPreferences(SETTING, MODE_PRIVATE);
		SharedPreferences.Editor editor = rtone.edit();
		editor.putString(RINGTONE_BUTTON_KEY, pickedUri.toString());
		editor.commit();
	}
	
	@Override
	protected void onPause() {

		super.onPause();
		//Save current values to shared preferences
		saveCheckBoxStatus("RINGTONE_CHECK_KEY", rtCheck.isChecked());
		saveCheckBoxStatus("VIBRATE_CHECK_KEY", vbCheck.isChecked());
		saveToggleStatus(serviceButton.isChecked());
	}

	
	
	
}
