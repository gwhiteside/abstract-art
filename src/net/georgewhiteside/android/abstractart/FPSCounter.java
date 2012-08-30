package net.georgewhiteside.android.abstractart;

import net.georgewhiteside.utility.MovingAverage;
import android.util.Log;

public class FPSCounter {
	
	float updateTimer = 0;
	float updateInterval = 2.0f;
	int frameCount = 0;
	MovingAverage movingAverage = new MovingAverage((int) (60));
	
	public void logFrame(float deltaTime) {
		frameCount++;
		movingAverage.addSample(deltaTime);
		updateTimer += deltaTime;
		
		if(updateTimer >= updateInterval) {
			Log.d("FPS", "Framerate: " + (1.0f / movingAverage.getAverage()) + "fps (" + frameCount + "frames counted over " + updateInterval + " seconds)");
			frameCount = 0;
			updateTimer = 0;
		}
	}
}
