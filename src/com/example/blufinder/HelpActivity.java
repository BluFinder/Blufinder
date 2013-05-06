package com.example.blufinder;

import android.os.Bundle;
import android.app.Activity;
import android.text.Layout;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.PopupWindow;

public class HelpActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		Button okB = (Button) findViewById(R.id.okButton);
	    // helpButton listener
		okB.setOnClickListener(new Button.OnClickListener() {
	         public void onClick(View v) {
	        	 finish();
	        	 overridePendingTransition(R.anim.animation_enter,
	                        R.anim.animation_leave);
	         }
	         
	    });
	}

	
}
