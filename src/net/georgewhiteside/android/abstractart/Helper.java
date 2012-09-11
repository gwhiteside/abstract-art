package net.georgewhiteside.android.abstractart;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

// http://www.yougli.net/android/live-wallpaper-binding-an-activity-to-the-open-button-of-the-market/

public class Helper extends Activity {
	
	private int REQUEST_CODE = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.helper);
		
		Button start = (Button) findViewById(R.id.start);
		
		start.setOnClickListener(new Button.OnClickListener(){
	        
	        public void onClick(View v) {
	        	new Handler().postDelayed(new Runnable() {
	        		public void run() {
	        			Toast.makeText(getApplicationContext(), "Now choose \"Abstract Art!\" Hmm, it should be here somewhere...", Toast.LENGTH_LONG).show();
	        		}

	        	}, 750);
	        	
	            Intent intent = new Intent();
	            intent.setAction(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
	            startActivityForResult(intent, REQUEST_CODE); // helper launcher closes after wallpaper chooser opens
	        }
	    });
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		super.onActivityResult(requestCode, resultCode, intent);
		if(requestCode == REQUEST_CODE) {
			finish();
		}
	}
}
