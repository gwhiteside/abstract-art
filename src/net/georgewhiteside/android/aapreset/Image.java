package net.georgewhiteside.android.aapreset;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Image {
	
	Palette palette;
	byte[] bytes;

	public Image(JSONObject imageObject, Map<String, byte[]> resources) throws JSONException {
		String file = imageObject.getString("file");
		bytes = resources.get(file);
		JSONArray paletteArray = imageObject.getJSONArray("palette");
		
		palette = new Palette(paletteArray);
	}

	public Palette getPalette() {
		return palette;
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

}
