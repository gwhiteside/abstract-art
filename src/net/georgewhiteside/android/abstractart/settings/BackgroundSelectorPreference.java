package net.georgewhiteside.android.abstractart.settings;

//import com.android.debug.hv.ViewServer;

import net.georgewhiteside.android.abstractart.R;
import net.georgewhiteside.android.abstractart.Renderer;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Toast;

/* TODO: when loading and saving thumbnails, the application version should ideally be checked because a thumbnail's
 * appearance may change based on code improvements (or regressions)
 */

public class BackgroundSelectorPreference extends Activity
{
	private Context context;
	private GLSurfaceView glSurfaceView;
	private Renderer renderer;
	private GridView gridView;
	private SharedPreferences sharedPreferences;
	private int selectedPosition = 1;
	
	private int selectionMode;
	
	private static final int SINGLE_BACKGROUND = 0;
	private static final int MULTIPLE_BACKGROUNDS = 1;
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt("selectedPosition", selectedPosition);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		selectedPosition = savedInstanceState.getInt("selectedPosition", 1);
		renderer.setInitialBackground(selectedPosition);
	}
	
	private void loadBattleBackground(final int index)
	{
		glSurfaceView.queueEvent(new Runnable(){
            public void run() {
            	renderer.loadBattleBackground(index);
            }
        });
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		
		setContentView(R.layout.background_selector_preference);
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		selectionMode = Integer.valueOf(sharedPreferences.getString("selectionMode", "1"));
		
		Spinner spinner = (Spinner)findViewById(R.id.selection_mode);
		spinner.setSelection(selectionMode);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				selectionMode = position;
			}

			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
			
		});
		
		glSurfaceView = (GLSurfaceView)findViewById(R.id.thumbnailGLSurfaceView);
		
		renderer = new Renderer(this); // TODO: recycle one renderer
		renderer.setCompletelyRandomMode(false);
		renderer.setInitialBackground(selectedPosition);
		glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setRenderer(renderer);
		glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		
		gridView = (GridView)findViewById(R.id.bgThumbGridView);
		
		gridView.setColumnWidth(128);
		gridView.setAdapter(new ThumbnailAdapter(this));
		
		gridView.setOnItemClickListener(new OnItemClickListener() { 
	        public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {
	        	if(position == selectedPosition)
	        	{
	        		((ThumbnailAdapter) parent.getAdapter()).toggleItem(position);
	        		Toast.makeText(context, "item " + renderer.getRomBackgroundIndex(position) + " toggled", Toast.LENGTH_SHORT).show();
	        	}
	        	else
	        	{
	        		selectedPosition = position;
	        		loadBattleBackground(position);
	        	}
	        }
	    });
		
		//ViewServer.get(this).addWindow(this); // TODO REMOVE THIS
	}
	
	public void savePreferences()
	{
		Editor editor = sharedPreferences.edit();
        editor.putString("selectionMode", String.valueOf(selectionMode));
        editor.commit();
	}
	
	public void onDestroy() {  
        super.onDestroy();  
        
        savePreferences();
        //ViewServer.get(this).removeWindow(this); // TODO REMOVE THIS
    }  
   
    public void onResume() {  
        super.onResume();  
        //ViewServer.get(this).setFocusedWindow(this); // TODO REMOVE THIS
    }  

	
}

/* (http://stackoverflow.com/questions/6524212/run-android-opengl-in-background-as-rendering-resource-for-app)

	Many current drivers on Android devices don't support multiple active GL
	contexts across processes; if the driver does support this, the feature has
	not been exercised much because Android itself does not do this, so there are
	likely to be nasty bugs that result.
	
	Multiple GL context is only being used by the platform starting with Android 3.0.
*/
