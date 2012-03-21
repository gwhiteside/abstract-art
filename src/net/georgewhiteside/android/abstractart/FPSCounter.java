package net.georgewhiteside.android.abstractart;

import android.util.Log;
import android.os.SystemClock;

public class FPSCounter {
	private long startTime = 0;
	private long startFrame = 0;
	private long endTime = 0;
	private int frames = 0;
	private long avgFrameTime = 0;
	private int interval = 2000; // output update interval in milliseconds
	
	public void logStartFrame()
	{
		if(startTime == 0)
		{
			startTime = SystemClock.uptimeMillis();
		}
		
		startFrame = SystemClock.uptimeMillis();
	}
	
	public void logEndFrame()
	{
		endTime = SystemClock.uptimeMillis();
		
		avgFrameTime += (endTime - startFrame);
		frames++;
		
		if(endTime - startTime >= interval)
		{
			Log.d("FPS", "FPS: " + (float)frames / (endTime - startTime) * 1000);
			Log.d("RenderTime", "Time to render frame: " + ((float)avgFrameTime / frames) + "ms");
			avgFrameTime = 0;
			startTime = 0;
			frames = 0;
		}
	}

}
