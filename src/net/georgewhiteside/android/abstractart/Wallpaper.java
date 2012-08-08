package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jf.GLWallpaper.GLWallpaperService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class Wallpaper extends GLWallpaperService 
{
	public static final String TAG = "AbstractArt";
	
	private static Context context;
	
	private static SharedPreferences sharedPreferences;
	public static Random random = new Random();
	
	public static boolean backgroundListIsDirty = false;
	
	private static List<Integer> backgroundList;
	
	static String backgroundListFileName = "playlist.json";
	static File backgroundListFile;
	
	public Wallpaper()
	{
		super();
		context = this;
	}
	
	@Override
	public Engine onCreateEngine()
	{
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        PreferenceManager.setDefaultValues(context, R.xml.settings, true); // fill out the default preference values if they're not yet set
		backgroundListFile = new File(context.getFilesDir(), backgroundListFileName);
		
		return new AbstractArtEngine(this);
	}
	
	public class AbstractArtEngine extends GLEngine
	{
		public net.georgewhiteside.android.abstractart.Renderer renderer;
		private GLWallpaperService glws;
		
		private long lastTap = 0; 
        private static final long TAP_THRESHOLD = 500;
		
		AbstractArtEngine(GLWallpaperService glws)
		{
			super();
			this.glws = glws;
		}
		
		@Override
	    public void onCreate(SurfaceHolder surfaceHolder)
	    {
	        super.onCreate(surfaceHolder);
	        setTouchEventsEnabled(true);
	        
	        
	        
	        // snag some display information
	        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
	        int displayPixelFormat = display.getPixelFormat();
	        int displayWidth = display.getWidth(); 
	        int displayHeight = display.getHeight();
	        float displayRefreshRate = display.getRefreshRate();
	        
	        /*
	         * http://developer.android.com/reference/android/graphics/PixelFormat.html
	         * 5 is for BGRA_8888
	         * 1 = RGBA_8888
	         */
	        
	        PixelFormat pixelFormat = new PixelFormat();
	        PixelFormat.getPixelFormatInfo(displayPixelFormat, pixelFormat);
	        
	        Log.i(TAG, String.format("PixelFormat: %d Screen: %dx%d RefreshRate: %f", displayPixelFormat, displayWidth, displayHeight, displayRefreshRate));
	        Log.i(TAG, String.format("PixelFormat.bitsPerPixel: %d PixelFormat.bytesPerPixel %d", pixelFormat.bitsPerPixel, pixelFormat.bytesPerPixel));
	        
			renderer = new net.georgewhiteside.android.abstractart.Renderer(glws);
			renderer.isPreview = isPreview();
			
			handleUpgrades(); // just as it sounds
			
			setEGLContextClientVersion(2);
			setRenderer(renderer);
			setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
	    }
		
		/*
		 * Expect this method to only grow messier, and messier, and messier as time goes on... ;)
		 */
		private void handleUpgrades()
		{
			
			
			//boolean dpb = detectPaletteBug();
			//Log.i(TAG, "detectPaletteBug: " + dpb);
			
			
			try
			{
				// This part is slick at least. We need only bump the app version in AndroidManifest.xml, and this code will
				// only run once per upgrade, forevermore.
				int currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionCode;
				int previousVersionCode = sharedPreferences.getInt("previousVersionCode", 0);
				if(previousVersionCode < currentVersionCode)
				{
					// bump the last version code in the shared preferences
					Editor editor = sharedPreferences.edit();
		            editor.putInt("previousVersionCode", currentVersionCode);
		            
		            // detect the dreaded palette bug
		            if(detectPaletteBug())
					{
		            	Log.w(TAG, "Palette cycling bug detected. Effect disabled by default.");
		            	clearCache();
		            	
		            	editor.putBoolean("enablePaletteEffects", false);
		            	editor.putBoolean("infoPaletteBugDetected", true);
		            	
		            	// hack to display a dialog from my wallpaper service... I know dialogs aren't meant to be run from services,
		            	// but I promise this is actually helpful and desirable in this case.
		            	/*Intent myIntent = new Intent();
		        		myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        		myIntent.putExtra("message", getResources().getText(R.string.message_palette_bug));
		        		myIntent.setComponent(new ComponentName("net.georgewhiteside.android.abstractart", "net.georgewhiteside.android.abstractart.ServiceDialog"));
		        		startActivity(myIntent);*/
					} else {
						editor.putBoolean("enablePaletteEffects", true);
						editor.putBoolean("infoPaletteBugDetected", false);
					}
		            
		            // versionCode 8 will need to regenerate thumbnails (indices are different), so detect and clear out any old cache
		            // TODO: make thumbnail filenames independent of gridview position indices (should just be the ROM background index)
		            int minimumCompatibleCacheVersion = 10;
		            if(previousVersionCode < minimumCompatibleCacheVersion)
		            {
		            	Log.i(TAG, "previousVersionCode < " + minimumCompatibleCacheVersion + " detected; clearing thumbnail cache and playlist.");
		            	clearCache(); // ok, actually we'll clear out ALL the cache... so sue me
		            	if(backgroundListFile.exists()) {
		            		backgroundListFile.delete(); // need to clear the playlist out too
		            	}
		            }
		            
		            editor.commit(); // write the preferences out
				}
			}
			catch (NameNotFoundException e)
			{
				e.printStackTrace();
			}
		}
		
		@Override
        public Bundle onCommand(final String action, int x, int y, int z, final Bundle extras, final boolean resultRequested)
        {
            if (action.equals(WallpaperManager.COMMAND_TAP))
            {
            	long thisTap = System.currentTimeMillis();
            	if(thisTap - lastTap < TAP_THRESHOLD)
            	{
            		String behavior = sharedPreferences.getString("stringDoubleTapBehavior", null);
            		
            		if(behavior.equals("nothing"))
            		{
            			// do nothing
            		}
            		else if(behavior.equals("next"))
            		{
            			// load next background
            			queueEvent( new Runnable() {
	            			public void run() {
	            				setNewBackground(renderer);
	            			}
	            		});
            		}
            		else if(behavior.equals("chooser"))
            		{
            			Intent myIntent = new Intent();
            			myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			myIntent.setComponent(new ComponentName("net.georgewhiteside.android.abstractart", "net.georgewhiteside.android.abstractart.settings.BackgroundSelector"));
            			startActivity(myIntent);
            		}
            		else if(behavior.equals("settings"))
            		{
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
		
		private boolean detectPaletteBug()
		{
			int width = 256, height = 256;
			GLOffscreenSurface glOffscreenSurface = new GLOffscreenSurface(width, height);
			glOffscreenSurface.setEGLContextClientVersion(2);
 			glOffscreenSurface.setRenderer(renderer);
			
			// we need to make sure that enemies aren't drawn for this test, but we don't want to clobber the default/previous
			// value for the preference, so we're saving it, setting it false, performing the test, then restoring the old value
			
			Editor editor = sharedPreferences.edit();
			boolean originalEnableEnemiesValue = sharedPreferences.getBoolean("enableEnemies", false); // grab original preference value
			boolean originalEnablePaletteEffectsValue = sharedPreferences.getBoolean("enablePaletteEffects", false);
			editor.putBoolean("enableEnemies", false); // explicitly disable enemy drawing
			editor.putBoolean("enablePaletteEffects", true); // and enable palette effects
			editor.commit();
			
			renderer.loadBattleBackground(1); // load up a test background
 			Bitmap thumbnail = glOffscreenSurface.getBitmap();
 			
 			editor.putBoolean("enableEnemies", originalEnableEnemiesValue); // restore original preference values
 			editor.putBoolean("enablePaletteEffects", originalEnablePaletteEffectsValue);
 			editor.commit();
 			
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

	}
	
	public static void clearCache()
	{
        File cacheDir = context.getCacheDir();
        if (cacheDir.exists())
        {
            String[] children = cacheDir.list();
            for (String s : children)
            {
                deleteDir(new File(cacheDir, s));
            }
        }
    }
	
	public static boolean deleteDir(File dir)
	{
        if (dir != null && dir.isDirectory())
        {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++)
            {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success)
                {
                	return false;
                }
            }
        }
        return dir.delete();
    }
	
	public static void setNewBackground(net.georgewhiteside.android.abstractart.Renderer renderer)
	{
		// if the playlist hasn't been loaded yet
		if(backgroundList == null)
		{
			backgroundList = new ArrayList<Integer>(renderer.getBackgroundsTotal());
		}
		
		// is the playlist runs through or is updated in the chooser, reload it from file
		if(backgroundList.isEmpty() || backgroundListIsDirty)
		{
	 		backgroundList = getBackgroundListFromFile(renderer);
	 		backgroundListIsDirty = false;
		}
		
		// if a playlist exists and has elements in it
		if(backgroundList.size() > 0)
		{
			// pull up a random background from the playlist
			int location = random.nextInt(backgroundList.size());
			int selection = backgroundList.get(location);
			backgroundList.remove(location);
			
			renderer.loadBattleBackground(selection);
		}
		else // it's possible to create a blank playlist; this accounts for it by loading randomly
		{
			renderer.setRandomBackground();
		}
	}
	
	/**
	 * Gets the saved background list file. If it doesn't exist, it creates the default one and returns that.
	 * @return
	 */
	public static List<Integer> getBackgroundListFromFile(net.georgewhiteside.android.abstractart.Renderer renderer)
	{
		List<Integer> bgList = null;
		
		if(backgroundListFile.exists())
		{
			try
			{
				byte[] data = null;
				FileInputStream fileInputStream = new FileInputStream(backgroundListFile);
				int bytesRead = 0;
		        int count = (int)backgroundListFile.length();
		        data = new byte[count];
		        while(bytesRead < count) {
		        	bytesRead += fileInputStream.read(data, bytesRead, count - bytesRead);
		        }
		        fileInputStream.close();
		        
		        String jsonString = new String(data);
		        JSONObject jsonObject = new JSONObject(jsonString);
		        JSONArray jsonArray = jsonObject.getJSONArray("backgrounds");
		        
		        int entries = jsonArray.length();
		        bgList = new ArrayList<Integer>(entries);
		        
		        for(int i = 0; i < entries; i++)
		        {
		        	bgList.add(jsonArray.getInt(i));
		        }
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			bgList = createInitialBackgroundList(renderer);
		}
		
		return bgList;
	}
	
	public static void saveBackgroundList(List<Integer> bgList)
	{
		// write it out to a file
	    try
	    {
	    	JSONObject jsonObject = new JSONObject();
	    	JSONArray jsonArray = new JSONArray(bgList);
	    	jsonObject.put("backgrounds", jsonArray);
			FileOutputStream fos = new FileOutputStream(backgroundListFile);
			String jsonString = jsonObject.toString();
			fos.write(jsonString.getBytes());
			fos.close();
			backgroundListIsDirty = true;
		}
	    catch (FileNotFoundException e)
	    {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    catch (IOException e)
	    {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static List<Integer> createInitialBackgroundList(net.georgewhiteside.android.abstractart.Renderer renderer)
	{
		int total = renderer.getBackgroundsTotal();
		List<Integer> bgList = new ArrayList<Integer>(total);
 		
		for(int i = 0; i < total; i++)
		{
			bgList.add(new Integer(i));
		}
		
	    return bgList;
	}
	
	public static List<Integer> createEmptyBackgroundList(net.georgewhiteside.android.abstractart.Renderer renderer)
	{
		int total = renderer.getBackgroundsTotal();
		List<Integer> bgList = new ArrayList<Integer>(total);
		return bgList;
	}
	
	public static List<Integer> getBackgroundList(net.georgewhiteside.android.abstractart.Renderer renderer)
	{
 		int total = renderer.getBackgroundsTotal();
		
		if(backgroundList == null)
		{
			backgroundList = new ArrayList<Integer>(total);
		}
		
		return backgroundList;
	}
}
