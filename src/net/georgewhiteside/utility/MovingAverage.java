package net.georgewhiteside.utility;

import java.util.Arrays;



public class MovingAverage {
	private float samples[];
	private int sampleCount = 0;
	private boolean isDirty = true;
	private int index = 0;
	private float average = 0;
	
	public MovingAverage(int windowSize) {
		samples = new float[windowSize]; // initializes to 0
	}
	
	public void clear() {
		Arrays.fill(samples, 0);
        sampleCount = 0;
        index = 0;
        isDirty = true;
	}
	
	public void addSample(float value) {
		samples[index++] = value;
		if(index >= samples.length) index = 0; 
		if(sampleCount < samples.length) sampleCount++;
		isDirty = true;
	}
	
	public float getAverage() {
		if(isDirty) {
			average = 0;
			for(int i = 0; i < sampleCount; i++) {
				average += samples[i];
			}
			average /= sampleCount;
			isDirty = false;
		}
		return average;
	}
	
	public boolean isWindowFull() {
		return sampleCount >= samples.length;
	}
}
