package net.georgewhiteside.android.abstractart;

import android.util.Log;
import android.os.SystemClock;

public class FPSCounter {
	private long initTime = SystemClock.uptimeMillis();
	private long currentTime = 0;
	
	private int frames = 0;
	
	private long avgTime = 0;
	private long frames2 = 0;
	
	public void logFrame() {
		currentTime = SystemClock.uptimeMillis();
		frames++;
		
		if(currentTime - initTime >= 1000) {
			//Log.d("FPSCounter", "fps: " + (float)frames / (float)(currentTime - initTime) * 1000.0f);
			Log.d("FPSCounter", "fps: " + frames);
			frames = 0;
			initTime = SystemClock.uptimeMillis();
		}
	}
	
	public void logEndFrame() {
		avgTime += SystemClock.uptimeMillis() - currentTime;
		frames2++;
		
		if(frames == 0) {
			float average = (float)avgTime / (float)frames2;
			//frames2 = 0;
			//avgTime = 0;
			Log.d("RenderTime", "Avg (running) time to render frame: " + average);
		}
	}
}
