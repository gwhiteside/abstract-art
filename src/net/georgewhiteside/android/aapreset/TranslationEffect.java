package net.georgewhiteside.android.aapreset;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TranslationEffect implements Updateable {
    
    private List<TranslationParameters> translationParameters;
    
    private TranslationParameters current;
    private int effectIndex;
    private double initialHorizontalOffset;
    private double initialVerticalOffset;
    private double currentHorizontalOffset;
    private double currentVerticalOffset;
    private double time;
    
	public TranslationEffect(JSONArray translationArray) throws JSONException {
		
		translationParameters = new ArrayList<TranslationParameters>();
		
		for(int i = 0; i < translationArray.length(); i++) {
			translationParameters.add(new TranslationParameters(translationArray.getJSONObject(i)));
		}
		
		if(translationParameters.size() > 0) {
			current = translationParameters.get(0);
		}
	}

	/*
	 * This is based on observation and rough guesswork; be forewarned when referencing this implementation.
	 * Also, it's not 100% accurate since moving to tick-less time-based updates, but I haven't noticed
	 * any glaring problems so I'm sticking with straight up acceleration equations. Again, be forewarned.
	 * 
	 */
	public void update(float deltaTime)
	{
		// x(t) = x0 + v0*t + 1/2*a*t^2

		// translation effect
		//dx += (x_velocity / 256.0) * time + 0.5 * (x_acceleration / 256.0) * time * time;
		//dy += (y_velocity / 256.0) * time + 0.5 * (y_acceleration / 256.0) * time * time;

		if(current.getDuration() > 0 && time * 60 >= current.getDuration()) {
			
			effectIndex++;
			if(effectIndex >= translationParameters.size()) {
				effectIndex = 0;
			}
			current = translationParameters.get(effectIndex);
			initialHorizontalOffset = currentHorizontalOffset;
			initialVerticalOffset = currentVerticalOffset;
			time = 0;
		}
		
		double time60 = time * 60;
		currentHorizontalOffset = initialHorizontalOffset + (current.getHorizontalVelocity() * time60 + 0.5f * current.getHorizontalAcceleration() * time60 * time60) / 256.0;
		currentVerticalOffset = initialVerticalOffset + (current.getVerticalVelocity() * time60 + 0.5f * current.getVerticalAcceleration() * time60 * time60) / 256.0;
		
		time += deltaTime;
	}
	
	public int getHorizontalAcceleration() {
		return current.getHorizontalAcceleration();
	}
	
	public int getHorizontalVelocity() {
		return current.getHorizontalVelocity();
	}
	
	public int getVerticalAcceleration() {
		return current.getVerticalAcceleration();
	}
	
	public int getVerticalVelocity() {
		return current.getVerticalVelocity();
	}
	
	public float currentHorizontalOffset() {
		return (float)currentHorizontalOffset;
	}
	
	public float currentVerticalOffset() {
		return (float)currentVerticalOffset;
	}
}
