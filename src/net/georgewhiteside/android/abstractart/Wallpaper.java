package net.georgewhiteside.android.abstractart;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipFile;

import net.georgewhiteside.android.aapreset.Preset;

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
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
	
	//public static float renderUpdatePeriodMs = 1 / 60.0f * 1000;
	//private static Long autoCycleTime;
	
	//private static boolean refreshOutput = false;
	//private static boolean refreshPreview = false; // refreshOutput, but for LWP preview
	
	public static final int CYCLE_HIDDEN = 1;
	public static final int CYCLE_INTERVAL = 2;
	public static final int CYCLE_NEVER = 3;
	
	private static int cycleBehavior;
	
	private static List<AbstractArtEngine> engineInstances = new ArrayList<AbstractArtEngine>();
	
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
		
		loadPresetTest();
		
		AbstractArtEngine engine = new AbstractArtEngine(this);
		engineInstances.add(engine);
		
		return engine;
	}
	
	public static void refresh() {
		for(AbstractArtEngine engine : engineInstances) {
			engine.refresh();
		}
	}
	
	public class AbstractArtEngine extends GLEngine
	{
		public net.georgewhiteside.android.abstractart.Renderer renderer;
		private GLWallpaperService glws;
		
		private long autoCycleTime = Long.valueOf(sharedPreferences.getString("autoCycleTime", null));
		
		private long lastTap = 0; 
        private static final long TAP_THRESHOLD = 500;
        
        private Thread renderThread;
    	private RenderRunnable renderRunnable = new RenderRunnable();
    	
    	private Handler backgroundCyclerHandler = new Handler();
    	
    	private Runnable backgroundCyclerRunnable = new Runnable() {
    		public void run() {
    			if(renderer != null) {
    				String autoCycleBehavior = sharedPreferences.getString("autoCycleBehavior", null);
    				if(autoCycleBehavior.equals("interval")) {
    					renderer.requestNewBackground(true);
    				}
    			}
    			backgroundCyclerHandler.postDelayed(this, autoCycleTime);
    		}
    	};
		
		AbstractArtEngine(GLWallpaperService glws) {
			super();
			this.glws = glws;
			
			backgroundCyclerHandler.postDelayed(backgroundCyclerRunnable, autoCycleTime);
		}
		
		public void refresh() {
			renderRunnable.refresh();
		}
		
		private class RenderRunnable implements Runnable {
			private boolean running;
			private float renderUpdatePeriod = 0;
			private boolean enableFrameskipping = false;
			
			public synchronized void refresh() {

				// update frame refresh rate
				
				int fps = Integer.valueOf(sharedPreferences.getString("framerateCap", null));
				renderUpdatePeriod = 1.0f / fps;
				
				// update frameskipping option
				
				enableFrameskipping = sharedPreferences.getBoolean("enableFrameskipping", false);
				
				// update background panning option
				
				boolean enablePanning = sharedPreferences.getBoolean("enablePanning", false);
				renderer.setEnablePanning(enablePanning);
				
				// update background auto cycle behavior and interval
				
				String autoCycleBehavior = sharedPreferences.getString("autoCycleBehavior", null);
				int previousCycleBehavior = cycleBehavior;
				
				if(autoCycleBehavior.equals("hidden")) {
					cycleBehavior = CYCLE_HIDDEN;
				} else if(autoCycleBehavior.equals("interval")) {
					cycleBehavior = CYCLE_INTERVAL;
				} else if(autoCycleBehavior.equals("never")) {
					cycleBehavior = CYCLE_NEVER;
				}
				
				switch(cycleBehavior) {
					case CYCLE_HIDDEN:
						renderer.setPersistBackgroundSelection(false);
						backgroundCyclerHandler.removeCallbacks(backgroundCyclerRunnable);
						break;
						
					case CYCLE_INTERVAL:
						long newAutoCycleTime = Long.valueOf(sharedPreferences.getString("autoCycleTime", null));
						if(newAutoCycleTime != autoCycleTime || previousCycleBehavior != CYCLE_INTERVAL) {
							autoCycleTime = newAutoCycleTime;
							renderer.setPersistBackgroundSelection(true);
							backgroundCyclerHandler.removeCallbacks(backgroundCyclerRunnable);
							backgroundCyclerHandler.postDelayed(backgroundCyclerRunnable, autoCycleTime);
						}
						
						break;
						
					case CYCLE_NEVER:
						renderer.setPersistBackgroundSelection(true);
						if(backgroundListIsDirty) {
							renderer.requestNewBackground(true);
						} else {
							renderer.requestNewBackground(false); // cancel any pending background change (if background playlist hasn't changed)
						}
						backgroundCyclerHandler.removeCallbacks(backgroundCyclerRunnable);
						break;
				}
					
				// update renderer options
				
				renderer.refreshOutput();
			}
			
			public void run() {
				long previousTime = System.nanoTime();
				float deltaTime = 0;
				long currentTime;
				int sleepyGuard = 0;
				running = true;
				
				refresh();
				
				while(running) {
					if(renderer.ready) {
						
						currentTime = System.nanoTime();
						deltaTime += (currentTime - previousTime) / 1000000000.0f;
						previousTime = currentTime;
						
						if(deltaTime < renderUpdatePeriod - 0.002 && sleepyGuard < 4)
						{
							sleepyGuard++;
							
							try {
								Thread.sleep((long)((renderUpdatePeriod - deltaTime) * 1000));
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
							//Log.d(TAG, "    ---- slept for: " + (renderUpdatePeriod - deltaTime) * 1000 + "ms");
						} else {
							sleepyGuard = 0;
							
							if(enableFrameskipping) {
								//renderer.battleGroup.battleBackground.doTick(deltaTime);
								preset.update(deltaTime);
							} else {
								//renderer.battleGroup.battleBackground.doTick(Math.min(deltaTime, 1 / 60.0f));
								preset.update(Math.min(deltaTime, 1 / 60.0f));
							}
							
							requestRender();
							//Log.d(TAG, "render delta update: " + deltaTime * 1000 + "ms");
							
							deltaTime -= renderUpdatePeriod;
							
							if(deltaTime < 0)
								deltaTime = 0;
						}
					}
				}
			}

			public void stop() {
				//Log.i(TAG, "stopping render update thread");
				renderer.ready = false;
				running = false;
			}
		}
		
		public void startRendering() {
			if(renderThread == null) {
				renderThread = new Thread(renderRunnable, "Render Draw Thread " + Thread.currentThread().getId());
				renderThread.start();
			}
		}
		
		public void stopRendering() {
			// !!! DO NOT FORGET TO CALL THIS WHEN DONE WITH A RENDERER !!!
			renderRunnable.stop();
			renderThread = null;
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
		
		@Override
	    public void onCreate(SurfaceHolder surfaceHolder) {
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
			renderer.setPersistBackgroundSelection(true);
			renderer.setIsPreview(isPreview());
			
			handleUpgrades(); // just as it sounds
			
			renderer.requestNewBackground(true); // load up a background from the playlist (when ready)... the handleUpgrades() loads in background 1
			
			setEGLContextClientVersion(2);
			setRenderer(renderer);
			setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			renderer.setRenderWhenDirty(true);
			
			preset = getRandomPreset();
			renderer.queueImmediate(preset);
	    }
		
		@Override
		public void onDestroy() {
			stopRendering();
			engineInstances.remove(this);
			super.onDestroy();
		}
		
		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
			super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
			renderer.setOffsets(xOffset, yOffset);
		}
		
		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
			startRendering();
		}
		
		@Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
			stopRendering();
			super.onSurfaceDestroyed(holder);
		}
		
		@Override
        public void onVisibilityChanged(final boolean visible) {
			if(renderer != null) {
				if(visible) {
					super.onVisibilityChanged(visible);
					startRendering();
				} else {
					stopRendering();
					super.onVisibilityChanged(visible);
				}
			} else {
				super.onVisibilityChanged(visible);
			}
		}
		
		/*
		 * Expect this method to only grow messier, and messier, and messier as time goes on... ;)
		 */
		private void handleUpgrades() {

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
		            /*if(detectPaletteBug())
					{
		            	Log.w(TAG, "Palette cycling bug detected. Effect disabled by default.");
		            	clearCache();
		            	
		            	editor.putBoolean("enablePaletteEffects", false);
		            	editor.putBoolean("infoPaletteBugDetected", true);
					} else {
						editor.putBoolean("enablePaletteEffects", true);
						editor.putBoolean("infoPaletteBugDetected", false);
					}*/ editor.putBoolean("enablePaletteEffects", true);
		            
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
		
		/*
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
 			
 			for(int y = 0; y < height; y++) {
 				for(int x = 0; x < width; x++) {
 					if(thumbnail.getPixel(x, y) != firstPixel) {
 						return false;
 					}
 				}
 			}
			
			return true;
		}
		*/
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
		/*
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
		
		int selection;
		
		// if a playlist exists and has elements in it
		if(backgroundList.size() > 0)
		{
			// pull up a random background from the playlist
			int location = random.nextInt(backgroundList.size());
			selection = backgroundList.get(location);
			backgroundList.remove(location);
		}
		else // it's possible to create a blank playlist; this accounts for it by loading randomly
		{
			selection = random.nextInt(renderer.battleGroup.battleBackground.getNumberOfBackgrounds() - 1) + 1;
		}
		*/
		
		Preset newPreset = getRandomPreset();
		preset = newPreset;
		
		//renderer.queueImmediate(preset);
		renderer.loadBattleBackground(preset);
	}
	
	public static Preset getRandomPreset() {
		int rand = random.nextInt(presetList.size());
		String filename = presetList.get(rand);
		InputStream is = null;
		Preset newPreset = null;
		try {
			is = context.getAssets().open(presetsPath + File.separator + filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			newPreset = new Preset(is);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return newPreset;
	}
	
	/**
	 * Gets the saved background list file. If it doesn't exist, it creates the default one and returns that.
	 * @return
	 */
	/*
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
	*/
	
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
			refresh();
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
	
	/*
	public static List<Integer> createInitialBackgroundList(net.georgewhiteside.android.abstractart.Renderer renderer)
	{
		int total = renderer.getBackgroundsTotal();
		List<Integer> bgList = new ArrayList<Integer>(total);
 		
		for(int i = 0; i < total; i++)
		{
			bgList.add(Integer.valueOf(i));
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
	*/
	
	static Preset preset;
	static List<String> presetList;
	static String presetsPath = "presets";
	
	public static void loadPresetTest()
	{	
		String number = "";
		
		try {
			presetList = new ArrayList<String>();
			String[] filenames = context.getAssets().list(presetsPath);
			
			for(int i = 0; i < filenames.length; i++) {
				if(filenames[i].endsWith(".aapreset")) {
					presetList.add(filenames[i]);
				}
			}
			
			Collections.addAll(presetList, filenames);
			//int numpresets = context.getAssets().list(presetsPath).length;
			//int rand = random.nextInt(numpresets) + 1;
			//number = String.format("%03d", rand);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/*
		String filename = "Mother2Battle" + number + ".aapreset";
		try {
			InputStream is = context.getAssets().open(presetsPath + File.separator + filename);
			preset = new Preset(is);
            
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		*/
	}

}
