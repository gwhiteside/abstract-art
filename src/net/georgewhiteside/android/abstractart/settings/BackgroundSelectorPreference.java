package net.georgewhiteside.android.abstractart.settings;

import net.georgewhiteside.android.abstractart.R;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

/* TODO: when loading and saving thumbnails, the application version should ideally be checked because a thumbnail's
 * appearance may change based on code improvements (or regressions)
 */

public class BackgroundSelectorPreference extends Activity
{
	private LinearLayout linearLayout;
	private GridView gridView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.background_selector_preference);
		
		gridView = (GridView)findViewById(R.id.bgThumbGridView);
		
		gridView.setColumnWidth(128);
		gridView.setAdapter(new BackgroundThumbnailAdapter(this));
		
		gridView.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
	            Toast.makeText(getApplicationContext(), "" + position, Toast.LENGTH_SHORT).show();
	        }
	    });
		
		// TODO: adapt to orientation changes
	}

	
}

/* (http://stackoverflow.com/questions/6524212/run-android-opengl-in-background-as-rendering-resource-for-app)

	Many current drivers on Android devices don't support multiple active GL
	contexts across processes; if the driver does support this, the feature has
	not been exercised much because Android itself does not do this, so there are
	likely to be nasty bugs that result.
	
	Multiple GL context is only being used by the platform starting with Android 3.0.
*/
