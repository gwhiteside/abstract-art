package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.net.URISyntaxException;

import org.jf.GLWallpaper.GLWallpaperService;
//import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
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
			
			try
			{
				// check to see if this is the first time running a new version, and take
				// any appropriate actions here
				
				PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
				int lastVersionCode = sharedPreferences.getInt("lastVersionCode", 0);
				if(lastVersionCode < packageInfo.versionCode)
				{
					// we only want to do this once per new version
					Editor editor = sharedPreferences.edit();
		            editor.putInt("lastVersionCode", packageInfo.versionCode);
		            editor.commit();
		            
		            if(detectPaletteBug())
					{
		            	clearCache();
		            	
		            	editor.putBoolean("enablePaletteEffects", false);
		            	editor.commit();
		            	
		            	// hack to display a dialog from my wallpaper service... I know dialogs aren't meant to be run from services,
		            	// but I promise this is actually helpful and desirable in this case.
		            	Intent myIntent = new Intent();
		        		myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        		myIntent.putExtra("message", getResources().getText(R.string.message_palette_bug));
		        		myIntent.setComponent(new ComponentName("net.georgewhiteside.android.abstractart", "net.georgewhiteside.android.abstractart.ServiceDialog"));
		        		startActivity(myIntent);
					}
				}
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
			
			
			setEGLContextClientVersion(2);
			setRenderer(renderer);
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
		}
		
		private boolean detectPaletteBug()
		{
			int width = 256, height = 256;
			GLOffscreenSurface glOffscreenSurface = new GLOffscreenSurface(width, height);
			glOffscreenSurface.setEGLContextClientVersion(2);
			glOffscreenSurface.setRenderer(renderer);
			
			renderer.loadBattleBackground(1);
 			
 			Bitmap thumbnail = glOffscreenSurface.getBitmap();
 			int firstPixel = thumbnail.getPixel(0, 0);
 			
 			for(int y = 0; y < height; y++)
 			{
 				for(int x = 0; x < width; x++)
 				{
 					if(thumbnail.getPixel(x, y) != firstPixel)
 					{
 						return false;
 					}
 				}
 			}
			
			return true;
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
	
	public void clearCache() {
        File cache = getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!s.equals("lib")) {
                    deleteDir(new File(appDir, s));
                    Log.i("TAG", "**************** File /data/data/APP_PACKAGE/" + s + " DELETED *******************");
                }
            }
        }
    }
	
	public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }
}
