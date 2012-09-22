package net.georgewhiteside.android.aapreset;

import org.json.JSONException;
import org.json.JSONObject;

public class PaletteEffect implements Updateable {
	
    int cycle1Begin;
	int cycle1End;
	int cycle2Begin;
	int cycle2End;
	int speed;
	int type;
	
	int paletteRotation;
	double time;
	
	public static final int CYCLE_NONE = 0x00;
	public static final int CYCLE_ROTATE1 = 0x01;
	public static final int CYCLE_ROTATE2 = 0x02;
	public static final int CYCLE_TRIANGLE = 0x03;

	public PaletteEffect(JSONObject paletteObject) throws JSONException {
		cycle1Begin = paletteObject.getInt("cycle1Begin");
		cycle1End = paletteObject.getInt("cycle1End");
		cycle2Begin = paletteObject.getInt("cycle2Begin");
		cycle2End = paletteObject.getInt("cycle2End");
		speed = paletteObject.getInt("speed");
		type = paletteObject.getInt("type");
	}
	
	public void update(float deltaTime) {
		if(time * 60 >= getSpeed()) {
			time = 0;

			switch(getType()) {
			case CYCLE_ROTATE1: // FALLTHROUGH
			case CYCLE_ROTATE2:
				// TODO: for cycle type 0x02, if the lengths of both cycle ranges is not equal, this will give incorrect output...
				// I may need to figure something else out, but for now it's tricky because there's no integer modulus in the GLSL and I can't get it
				// to work consistently with floats. Sending the modified palette indices over isn't an attractive option either because that could take
				// up to 32 varyings, or one 16x2 texture upload every frame... maybe look into the palette texture re-ups, it might be feasible
				// OTHERWISE, I may want to consider adding an extra varying for each layer to track cycle rotation independently for type 0x02
				if(getRotation() >= (getCycle1End() - getCycle1Begin() + 1)) {
					paletteRotation = 1;
				} else {
					paletteRotation++;
				}
				break;

			case CYCLE_TRIANGLE:
				if(paletteRotation >= (getCycle1End() - getCycle1Begin() + 1) * 2) {
					paletteRotation = 1;
				} else {
					paletteRotation++;
				}
				break;
			}
		}
		time += deltaTime;
	}
	
	public int getRotation() {
		return paletteRotation;
	}

	public int getCycle1Begin() {
		return cycle1Begin;
	}

	public void setCycle1Begin(int cycle1Begin) {
		this.cycle1Begin = cycle1Begin;
	}

	public int getCycle1End() {
		return cycle1End;
	}

	public void setCycle1End(int cycle1End) {
		this.cycle1End = cycle1End;
	}

	public int getCycle2Begin() {
		return cycle2Begin;
	}

	public void setCycle2Begin(int cycle2Begin) {
		this.cycle2Begin = cycle2Begin;
	}

	public int getCycle2End() {
		return cycle2End;
	}

	public void setCycle2End(int cycle2End) {
		this.cycle2End = cycle2End;
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
