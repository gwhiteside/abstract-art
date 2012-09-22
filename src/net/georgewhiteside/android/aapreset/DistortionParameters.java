package net.georgewhiteside.android.aapreset;

import org.json.JSONException;
import org.json.JSONObject;

class DistortionParameters {
	private int amplitude;
	private int amplitudeDelta;
	private int compression;
	private int compressionDelta;
	private int duration;
	private int frequency;
	private int frequencyDelta;
	private int speed;
	private int type;
	
	public DistortionParameters(JSONObject distortionObject) throws JSONException {
		setAmplitude(distortionObject.getInt("amplitude"));
		setAmplitudeDelta(distortionObject.getInt("amplitudeDelta"));
		setCompression(distortionObject.getInt("compression"));
		setCompressionDelta(distortionObject.getInt("compressionDelta"));
		setDuration(distortionObject.getInt("duration"));
		setFrequency(distortionObject.getInt("frequency"));
		setFrequencyDelta(distortionObject.getInt("frequencyDelta"));
		setSpeed(distortionObject.getInt("speed"));
		setType(distortionObject.getInt("type"));
	}
	
	public DistortionParameters() {
		// blank effect parameter set
	}

	public int getAmplitude() {
		return amplitude;
	}

	public void setAmplitude(int amplitude) {
		this.amplitude = amplitude;
	}

	public int getAmplitudeDelta() {
		return amplitudeDelta;
	}

	public void setAmplitudeDelta(int amplitudeDelta) {
		this.amplitudeDelta = amplitudeDelta;
	}

	public int getCompression() {
		return compression;
	}

	public void setCompression(int compression) {
		this.compression = compression;
	}

	public int getCompressionDelta() {
		return compressionDelta;
	}

	public void setCompressionDelta(int compressionDelta) {
		this.compressionDelta = compressionDelta;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public int getFrequencyDelta() {
		return frequencyDelta;
	}

	public void setFrequencyDelta(int frequencyDelta) {
		this.frequencyDelta = frequencyDelta;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}