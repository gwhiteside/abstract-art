package net.georgewhiteside.android.aapreset;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DistortionEffect implements Updateable {
	
	private List<DistortionParameters> distortionParameters;
	
	private DistortionParameters current;
	private int effectIndex;
	private double time;
	
	private double runningAmplitude;
	private double runningCompression;
	private double runningFrequency;
	private double runningSpeed;
	
	public DistortionEffect(JSONArray distortionArray) throws JSONException {
		distortionParameters = new ArrayList<DistortionParameters>();

		for(int i = 0; i < distortionArray.length(); i++) {
			distortionParameters.add(new DistortionParameters(distortionArray.getJSONObject(i)));
		}
		
		if(distortionParameters.size() > 0) {
			current = distortionParameters.get(0);
		}
	}
	
	public void update(float deltaTime) {
		if(current.getDuration() > 0 && time * 60 >= current.getDuration()) {
			effectIndex++;
			if(effectIndex >= distortionParameters.size()) {
				effectIndex = 0;
			}
			current = distortionParameters.get(effectIndex);
			time = 0;
		}
		
		runningAmplitude = (current.getAmplitude() + current.getAmplitudeDelta() * time * 60) / 512.0;
		runningCompression = current.getCompression() + current.getCompressionDelta() * time * 60;
		runningFrequency = (current.getFrequency() + current.getFrequencyDelta() * time * 60) * 8.0 * Math.PI / (1024.0 * 256.0);
		runningSpeed = (Math.PI * current.getSpeed() * time * 60) / 120.0f;
		
		time += deltaTime;
	}
	
	public int getNumberOfEffects() {
		return distortionParameters.size();
	}
	
	public int getCompression() {
		return current.getCompression();
	}
	
	public int getCompressionDelta() {
		return current.getCompressionDelta();
	}
	
	public int getType() {
		return current.getType();
	}
	
	public float runningAmplitude() {
		return (float)runningAmplitude;
	}
	
	public float runningCompression() {
		return (float)runningCompression;
	}
	
	public float runningFrequency() {
		return (float)runningFrequency;
	}
	
	public float runningSpeed() {
		return (float)runningSpeed;
	}
}
