package net.georgewhiteside.android.aapreset;

import org.json.JSONException;
import org.json.JSONObject;

public class TranslationParameters {
	
	private int duration;
	private int horizontalAcceleration;
	private int horizontalVelocity;
	private int verticalAcceleration;
	private int verticalVelocity;

	public TranslationParameters(JSONObject translationObject) throws JSONException {
		duration = translationObject.getInt("duration");
		horizontalAcceleration = translationObject.getInt("horizontalAcceleration");
		horizontalVelocity = translationObject.getInt("horizontalVelocity");
		verticalAcceleration = translationObject.getInt("verticalAcceleration");
		verticalVelocity = translationObject.getInt("verticalVelocity");
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getHorizontalAcceleration() {
		return horizontalAcceleration;
	}

	public void setHorizontalAcceleration(int horizontalAcceleration) {
		this.horizontalAcceleration = horizontalAcceleration;
	}

	public int getHorizontalVelocity() {
		return horizontalVelocity;
	}

	public void setHorizontalVelocity(int horizontalVelocity) {
		this.horizontalVelocity = horizontalVelocity;
	}

	public int getVerticalAcceleration() {
		return verticalAcceleration;
	}

	public void setVerticalAcceleration(int verticalAcceleration) {
		this.verticalAcceleration = verticalAcceleration;
	}

	public int getVerticalVelocity() {
		return verticalVelocity;
	}

	public void setVerticalVelocity(int verticalVelocity) {
		this.verticalVelocity = verticalVelocity;
	}
}
