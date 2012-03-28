package net.georgewhiteside.android.abstractart.settings;

import net.georgewhiteside.android.abstractart.Wallpaper;
import net.georgewhiteside.android.abstractart.R;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class CreateImageCachePreference extends Preference
{
	Context context;
	
	public CreateImageCachePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}
	
	public CreateImageCachePreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}
	
	public CreateImageCachePreference(Context context) {
		this(context, null, android.R.attr.dialogPreferenceStyle);
	}
	
	 /*@Override
	 protected void onBindView(View view) {
		 super.onBindView(view);
	 }*/
	
	/*public DialogPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.DialogPreference, defStyle, 0);
        mDialogTitle = a.getString(com.android.internal.R.styleable.DialogPreference_dialogTitle);
        if (mDialogTitle == null) {
            // Fallback on the regular title of the preference
            // (the one that is seen in the list)
            mDialogTitle = getTitle();
        }
        mDialogMessage = a.getString(com.android.internal.R.styleable.DialogPreference_dialogMessage);
        mDialogIcon = a.getDrawable(com.android.internal.R.styleable.DialogPreference_dialogIcon);
        mPositiveButtonText = a.getString(com.android.internal.R.styleable.DialogPreference_positiveButtonText);
        mNegativeButtonText = a.getString(com.android.internal.R.styleable.DialogPreference_negativeButtonText);
        mDialogLayoutResId = a.getResourceId(com.android.internal.R.styleable.DialogPreference_dialogLayout,
                mDialogLayoutResId);
        a.recycle();
        
    }

    public DialogPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.dialogPreferenceStyle);
    }*/
	
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
		private int numberOfBackgrounds = Wallpaper.engine.renderer.getBackgroundsTotal();
		private int loaded = 0;

		@Override
		protected Void doInBackground(Integer... params) {
			loaded = 0;
			for(int i = 0; i < numberOfBackgrounds; i++)
			{
				Wallpaper.engine.renderer.cacheImage(i);
				publishProgress( i );
				loaded++;
				
				if(isCancelled()) {
					break;
				}
			}

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
