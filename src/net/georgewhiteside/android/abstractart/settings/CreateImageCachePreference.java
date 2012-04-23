package net.georgewhiteside.android.abstractart.settings;

import net.georgewhiteside.android.abstractart.R;
import net.georgewhiteside.android.abstractart.Renderer;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

public class CreateImageCachePreference extends Preference
{
	Context context;
	Renderer renderer;
	
	public CreateImageCachePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		renderer = new Renderer(context);
	}
	
	public CreateImageCachePreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}
	
	public CreateImageCachePreference(Context context) {
		this(context, null, android.R.attr.dialogPreferenceStyle);
	}
	
	@Override
    protected void onClick() {
        //super.onClick();
        //Log.i("Layer", "a click! :)");
		new GenerateImageCacheTask().execute(23, 23, null);
    }
	
	// TODO: this is a slight hack... it's easier at the moment to simply load all the backgrounds with their extra
	// data than it is to iterate over the ~100 unique images
	class GenerateImageCacheTask extends AsyncTask<Integer, Integer, Void> {
		
		private ProgressDialog dialog;
		private int numberOfBackgrounds = renderer.getBackgroundsTotal();
		private int loaded = 0;

		@Override
		protected Void doInBackground(Integer... params) {
			loaded = 0;
			
			long startTime = SystemClock.uptimeMillis();
			
			for(int i = 0; i < numberOfBackgrounds; i++)
			{
				renderer.cacheImage(i);
				publishProgress( i );
				loaded++;
				
				if(isCancelled()) {
					break;
				}
			}
			
			long endTime = SystemClock.uptimeMillis();
			
			Log.i("cacheTime", "total cache time: " + ((float)endTime - startTime) / 1000 + "s");

			return null;
		}
		
		@Override
		protected void onCancelled()
		{
			displayMessage();
		}
		
		@Override
		protected void onPreExecute()
		{
			dialog = new ProgressDialog(context);
			dialog.setIndeterminate(false);
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setMax(numberOfBackgrounds);
			dialog.setTitle(R.string.processing);
			//dialog.setMessage("");
			dialog.setCancelable(true);
	        dialog.setOnCancelListener(new OnCancelListener() {
	            public void onCancel(DialogInterface dialog) {
	                cancel(true);
	            }
	        });
	        
	        dialog.show();
	        
	        //Toast.makeText(context, "Press your back button to cancel.", Toast.LENGTH_SHORT).show();
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			dialog.dismiss();
			displayMessage();
		}
		
		@Override
		protected void onProgressUpdate(Integer... progress) {
			
			dialog.setProgress(progress[0]);
		}
		
		private void displayMessage()
		{
			String message; 
			Resources res = context.getResources();
			
			if(loaded == numberOfBackgrounds) {
				message = res.getString(R.string.processed_all_backgrounds);
			} else {
				message = String.format(res.getString(R.string.processed_n_of_n_backgrounds), loaded, numberOfBackgrounds);
			}

			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	}
}
