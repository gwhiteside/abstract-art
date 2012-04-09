package net.georgewhiteside.android.abstractart;

import java.net.URISyntaxException;

import org.jf.GLWallpaper.GLWallpaperService;
//import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

public class Wallpaper extends GLWallpaperService 
{
	public static AbstractArtEngine engine;
	private SharedPreferences sharedPreferences;
	
	public Wallpaper()
	{
		super();
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	@Override
	public Engine onCreateEngine()
	{
		engine = new AbstractArtEngine(this);
		return engine;
	}
	
	public class AbstractArtEngine extends GLEngine
	{
		public net.georgewhiteside.android.abstractart.Renderer renderer;
		
		private long lastTap = 0; 
        private static final long TAP_THRESHOLD = 500;
		
		AbstractArtEngine(GLWallpaperService glws)
		{
			super();
			renderer = new net.georgewhiteside.android.abstractart.Renderer(glws);
			
			setEGLContextClientVersion(2);
			setRenderer(renderer);
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		}
		
		@Override
	    public void onCreate(SurfaceHolder surfaceHolder)
	    {
	        super.onCreate(surfaceHolder);
	        
	        //android.os.Debug.waitForDebugger();
	        setTouchEventsEnabled(true);
	        
	        /*gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener()
	        {
	            @Override
	            public boolean onSingleTapConfirmed(MotionEvent e)
	            {
	            	Log.e("tapdebug", e.toString());
	            	return true;
	            }
	        });*/
	    }
		
		/*@Override
		public void onTouchEvent(MotionEvent event)
		{
			gestureDetector.onTouchEvent(event);         
        }*/
		
		@Override
        public Bundle onCommand(final String action, int x, int y, int z, final Bundle extras, final boolean resultRequested)
        {
	            if (action.equals(WallpaperManager.COMMAND_TAP))
	            {
	            	long thisTap = System.currentTimeMillis();
	            	if(thisTap - lastTap < TAP_THRESHOLD)
	            	{
	            		String behavior = sharedPreferences.getString("stringDoubleTapBehavior", "next");
	            		
	            		Log.i("aadebug", behavior);
	            		
	            		if(behavior.equals("nothing")) {
	            			// do nothing
	            		} else if(behavior.equals("next")) { // load next background
	            			queueEvent( new Runnable() {
		            			public void run() {
		            				renderer.setRandomBackground();
		            			}
		            		});
	            		} else if(behavior.equals("chooser")) {
	            			Intent myIntent = new Intent();
	            			myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            			myIntent.setComponent(new ComponentName("net.georgewhiteside.android.abstractart", "net.georgewhiteside.android.abstractart.settings.BackgroundSelectorPreference"));
	            			startActivity(myIntent);
	            		} else if(behavior.equals("settings")) {
	            			Intent myIntent = new Intent();
	            			myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            			myIntent.setComponent(new ComponentName("net.georgewhiteside.android.abstractart", "net.georgewhiteside.android.abstractart.Settings"));
	            			startActivity(myIntent);
	            		}
	            		
	            		lastTap = 0;
	            	}
	            	else
	            	{
	            		lastTap = thisTap;
	            	}
	            } 
	
	            return super.onCommand(action, x, y, z, extras, resultRequested);
        }
	}
}
