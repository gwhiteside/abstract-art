package net.georgewhiteside.android.abstractart.settings;

import com.android.debug.hv.ViewServer;

import net.georgewhiteside.android.abstractart.R;
import net.georgewhiteside.android.abstractart.Renderer;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

/* TODO: when loading and saving thumbnails, the application version should ideally be checked because a thumbnail's
 * appearance may change based on code improvements (or regressions)
 */

public class BackgroundSelectorPreference extends Activity
{
	private GLSurfaceView glSurfaceView;
	private Renderer renderer;
	private GridView gridView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.background_selector_preference);
		
		glSurfaceView = (GLSurfaceView)findViewById(R.id.thumbnailGLSurfaceView);
		renderer = new Renderer(this); // TODO: recycle one renderer
		glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setRenderer(renderer);
		glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		
		gridView = (GridView)findViewById(R.id.bgThumbGridView);
		
		gridView.setColumnWidth(128);
		gridView.setAdapter(new ThumbnailAdapter(this));
		
		gridView.setOnItemClickListener(new OnItemClickListener() { 
	        public void onItemClick(AdapterView<?> parent, View v, final int position, long id) {
	        	glSurfaceView.queueEvent(new Runnable(){
	                public void run() {
	                	renderer.loadBattleBackground(position);
	                }
	            }); // (yuck)
	        	Toast toast = Toast.makeText(glSurfaceView.getContext(), "" + position, Toast.LENGTH_SHORT);
	        	toast.setGravity(Gravity.CENTER, 0, 0);
	        	toast.show();
	        }
	    });
		
		ViewServer.get(this).addWindow(this); // TODO REMOVE THIS
		
		// TODO: adapt to orientation changes
	}
	
	public void onDestroy() {  
        super.onDestroy();  
        ViewServer.get(this).removeWindow(this); // TODO REMOVE THIS
    }  
   
    public void onResume() {  
        super.onResume();  
        ViewServer.get(this).setFocusedWindow(this); // TODO REMOVE THIS
    }  

	
}

/* (http://stackoverflow.com/questions/6524212/run-android-opengl-in-background-as-rendering-resource-for-app)

	Many current drivers on Android devices don't support multiple active GL
	contexts across processes; if the driver does support this, the feature has
	not been exercised much because Android itself does not do this, so there are
	likely to be nasty bugs that result.
	
	Multiple GL context is only being used by the platform starting with Android 3.0.
*/
