package net.georgewhiteside.android.aapreset;

import org.json.JSONArray;
import org.json.JSONException;

public class Palette {
	
	byte[] bytes;

	public Palette(JSONArray paletteArray) throws JSONException {
		int colors = paletteArray.length();
		bytes = new byte[colors * 4];
		for(int i = 0; i < colors; i++) {
			String hexRgba = paletteArray.getString(i);
			
			int red = Integer.valueOf(hexRgba.substring(0, 2), 16);
			int green = Integer.valueOf(hexRgba.substring(2, 4), 16);
			int blue = Integer.valueOf(hexRgba.substring(4, 6), 16);
			int alpha = Integer.valueOf(hexRgba.substring(6, 8), 16);
			
			bytes[i * 4 + 0] = (byte) red;
			bytes[i * 4 + 1] = (byte) green;
			bytes[i * 4 + 2] = (byte) blue;
			bytes[i * 4 + 3] = (byte) alpha;
		}
	}

	public byte[] getBytes() {
		return bytes;
	}
}
