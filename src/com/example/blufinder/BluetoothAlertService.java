package com.example.blufinder;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class BluetoothAlertService extends Service{

	private int mStartMode;
	private IBinder mBinder;
	private boolean mAllowRebind;
	private AlarmManager alarmManager;

	final private String SETTING = "Setting";			// User settings
	final private String RINGTONE_BUTTON_KEY = "Ringtone";

	private NotificationCompat.Builder builder;
	private Boolean ringtoneCheck = true;
	private Boolean vibrateCheck= true;


	@Override
	public void onCreate() {

		super.onCreate();



		IntentFilter filter_disconnetced = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		IntentFilter filter_disconnected_requested= new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		IntentFilter filter_bluetoothAdapter_state = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

		IntentFilter filter_ringtone_checked_change = new IntentFilter("RINGTONE_CHECKED_CHANGE");
		IntentFilter filter_vibrate_checked_change = new IntentFilter("VIBRATE_CHECKED_CHANGE");

		//Register Broadcast Receiver
		registerReceiver(mReceiver, filter_disconnetced);
		registerReceiver(mReceiver, filter_disconnected_requested);
		registerReceiver(mReceiver, filter_bluetoothAdapter_state);

		registerReceiver(mRingtoneReceiver, filter_ringtone_checked_change);
		registerReceiver(mRingtoneReceiver, filter_vibrate_checked_change);
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {

		if(intent != null )
		{
			ringtoneCheck = intent.getBooleanExtra("RINGTONE_CHECK_KEY", false);
			vibrateCheck = intent.getBooleanExtra("VIBRATE_CHECK_KEY", false);
		}
        
        return mStartMode;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return null;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return false;
    }
    
    @Override
    public void onDestroy() {
    	
    	super.onDestroy();
    	unregisterReceiver(mReceiver);
    	unregisterReceiver(mRingtoneReceiver);
    }
    
    
  //Receiver for Bluetooth adapter state change and Bluetooth connection mode action
    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();

			if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
			{
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch(state)
				{
					case BluetoothAdapter.STATE_OFF:
		                stopSelf();
		                break;
		            case BluetoothAdapter.STATE_TURNING_OFF:
		            	 stopSelf();
		                break;
				}
			}
			else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) || 
					BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action))
			{
				alarmManager = (AlarmManager)getBaseContext().getSystemService(ALARM_SERVICE);
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				createNotificationBuilder(device.getName(), ringtoneCheck, vibrateCheck);
				setNotifications(device.getName());

             }
			else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) 
			{

			}


         }


	};

	//Receiver for Ringtone checked change and vibrate checked change action
    private final BroadcastReceiver mRingtoneReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();

			if(action.equalsIgnoreCase("RINGTONE_CHECKED_CHANGE"))
			{
				ringtoneCheck = intent.getBooleanExtra("RINGTONE_CHECK_KEY", false);
			}
			if(action.equalsIgnoreCase("VIBRATE_CHECKED_CHANGE"))
			{
				vibrateCheck = intent.getBooleanExtra("VIBRATE_CHECK_KEY", false);
			}


         }


	};

	// Get current ringtone
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

	//Push Notifications
	public void setNotifications(String name)
	{
      	Notification notification = builder.build();
    	notification.flags |= Notification.FLAG_AUTO_CANCEL;
    	
    	NotificationManager nfmanager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    	nfmanager.notify(1,notification);
    	
    	Toast.makeText(getBaseContext(), "Bluetooth Connection with "+name+" lost", Toast.LENGTH_LONG).show();
	}

	//Create Notification Builder
	private void createNotificationBuilder(String name, Boolean ringtoneCheck, Boolean vibrateCheck)
	{
		Intent resultIntent = new Intent(this, MainActivity.class);

		Uri notificationRingone = getRingtoneUri();
		 long[] vibrate = new long[]{100, 200, 100, 500};

		resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	
		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    	
		builder = new NotificationCompat.Builder(this)
    	.setContentTitle("Bluetooth Disconnected")
		.setSmallIcon(R.drawable.ic_launcher)
    	.setContentText("Bluetooth Connection with the remote device "+name+" is lost")
    	.setAutoCancel(true)
    	.setOnlyAlertOnce(false);
    	    	
		if(ringtoneCheck)
			builder.setSound(notificationRingone);
		if(vibrateCheck)
			builder.setVibrate(vibrate);

		builder.setContentIntent(resultPendingIntent);
    	 		

	}





}