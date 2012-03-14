package net.georgewhiteside.android.abstractart;

import android.util.Log;
import android.os.SystemClock;

public class FPSCounter {
	private long startTime = 0;
	private long endTime = 0;
	private int frames = 0;
	private long avgTime = 0;
	private int interval = 2000; // output update interval in milliseconds
	
	public void logStartFrame() {
		startTime = SystemClock.uptimeMillis();
	}
	
	public void logEndFrame() {
		endTime = SystemClock.uptimeMillis();
		frames++;
		
		avgTime += endTime - startTime;
		
		if(avgTime >= interval)
		{
			float average = avgTime / (float)frames;
			float fps = 1000 / average;
			Log.d("RenderTime", "Avg time to render frame: " + average);
			Log.d("FPSCounter", "fps: " + fps);
			avgTime = 0;
			frames = 0;
		}
	}
}
