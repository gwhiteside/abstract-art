package net.georgewhiteside.android.abstractart.settings;

//import com.android.debug.hv.ViewServer;

import java.util.List;

import net.georgewhiteside.android.abstractart.R;
import net.georgewhiteside.android.abstractart.Renderer;
import net.georgewhiteside.android.abstractart.Wallpaper;
import net.georgewhiteside.android.abstractart.settings.ThumbnailAdapter.ViewHolder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.Toast;

/* TODO: when loading and saving thumbnails, the application version should ideally be checked because a thumbnail's
 * appearance may change based on code improvements (or regressions)
 */

public class BackgroundSelector extends Activity
{
	private Context context;
	private GLSurfaceView glSurfaceView;
	private Renderer renderer;
	private SharedPreferences sharedPreferences;
	private ThumbnailAdapter thumbnailAdapter;
	private GridView gridView;
	private int selectedPosition = 1;
	
	private List<Integer> backgroundList;
	
	private int selectionMode;
	
	
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt("selectedPosition", selectedPosition);
		super.onSaveInstanceState(outState);
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
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		context = this;
		
		// restore any saved instance state if it exists
		
		if(savedInstanceState != null)
		{
			selectedPosition = savedInstanceState.getInt("selectedPosition", Wallpaper.MULTIPLE_BACKGROUNDS);
		}
		
		setContentView(R.layout.background_selector_preference);
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		selectionMode = Integer.valueOf(sharedPreferences.getString("selectionMode", Integer.toString(Wallpaper.MULTIPLE_BACKGROUNDS)));
		
		glSurfaceView = (GLSurfaceView)findViewById(R.id.thumbnailGLSurfaceView);
		
		renderer = new Renderer(this, selectedPosition);
		renderer.setPersistBackgroundSelection(true);
		
		backgroundList = Wallpaper.getBackgroundListFromFile(renderer);
		
		glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setRenderer(renderer);
		glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		
		thumbnailAdapter = new ThumbnailAdapter(this, backgroundList);
		
		gridView = (GridView)findViewById(R.id.bgThumbGridView);
		gridView.setColumnWidth(128);
		gridView.setAdapter(thumbnailAdapter);
		gridView.setOnItemClickListener(new GridViewOnItemClickListener());
		gridView.setOnItemLongClickListener(new GridViewOnItemLongClickListener());
		
		//ViewServer.get(this).addWindow(this); // TODO REMOVE THIS
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.background_selector, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId())
	    {
	        case R.id.select_all:
	        	List<Integer> completeList = Wallpaper.createInitialBackgroundList(renderer);
	        	thumbnailAdapter.setBackgroundList(completeList);
	        	thumbnailAdapter.notifyDataSetChanged();
	        	backgroundList = completeList;
	        	//gridView.invalidateViews();
	            return true;
	            
	        case R.id.clear_all:
	        	List<Integer> emptyList = Wallpaper.createEmptyBackgroundList(renderer);
	        	thumbnailAdapter.setBackgroundList(emptyList);
	        	thumbnailAdapter.notifyDataSetChanged();
	        	backgroundList = emptyList;
	        	//gridView.invalidateViews();
	            return true;
	            
	        case R.id.help:
	        	showHelpDialog();
	            return true;
	            
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void showHelpDialog()
	{
		WebView webView = new WebView(context);
    	webView.setBackgroundColor(0);
    	webView.loadUrl("file:///android_asset/background_selector/index.html");
    	
    	Builder dialog = new AlertDialog.Builder(this);
    	
    	dialog.setPositiveButton("OK!", null);
    	dialog.setView(webView);
    	
    	dialog.create().show();
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
        Wallpaper.saveBackgroundList(backgroundList);
        //ViewServer.get(this).removeWindow(this); // TODO REMOVE THIS
    }  
   
    public void onResume() {  
        super.onResume();  
        //ViewServer.get(this).setFocusedWindow(this); // TODO REMOVE THIS
    }
	
	class GridViewOnItemClickListener implements OnItemClickListener
	{
		public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        	if(position == selectedPosition)
        	{
        		//((ThumbnailAdapter) parent.getAdapter()).toggleItem(view, position);
        		
        		toggleItem(view, position);
        		//Toast.makeText(context, "item " + renderer.getRomBackgroundIndex(position) + " toggled", Toast.LENGTH_SHORT).show();
        	}
        	else
        	{
        		selectedPosition = position;
        		loadBattleBackground(position);
        	}
        }
	}
	
	class GridViewOnItemLongClickListener implements OnItemLongClickListener
	{
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			Toast.makeText(context, "long click", Toast.LENGTH_SHORT).show();
			return true; // consume the click
		}
	}
	
	public void toggleItem(View view, int position)
	{
		Integer item = new Integer(position);
		if(backgroundList.contains(item))
		{
			backgroundList.remove(item);
		}
		else
		{
			backgroundList.add(item);
		}
		
		ViewHolder holder = (ViewHolder) view.getTag();
		thumbnailAdapter.setCheckmark(holder, position);
	}
}

/* (http://stackoverflow.com/questions/6524212/run-android-opengl-in-background-as-rendering-resource-for-app)

	Many current drivers on Android devices don't support multiple active GL
	contexts across processes; if the driver does support this, the feature has
	not been exercised much because Android itself does not do this, so there are
	likely to be nasty bugs that result.
	
	Multiple GL context is only being used by the platform starting with Android 3.0.
*/
