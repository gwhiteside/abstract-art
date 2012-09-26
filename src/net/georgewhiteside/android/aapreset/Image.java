package net.georgewhiteside.android.aapreset;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Image {
	
	Palette palette;
	byte[] bytes;
	int height;
	int width;

	public Image(JSONObject imageObject, Map<String, byte[]> resources) throws JSONException {
		String file = imageObject.getString("file");
		setIndexedBytes(resources.get(file));
		JSONArray paletteArray = imageObject.getJSONArray("palette");
		setPalette(new Palette(paletteArray));
		setDimensions(imageObject.getInt("width"), imageObject.getInt("height"));
	}

	public Palette getPalette() {
		return palette;
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
	}

	public byte[] getIndexedBytes() {
		return bytes;
	}

	public void setIndexedBytes(byte[] bytes) {
		this.bytes = bytes;
	}
	
	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public void setDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public byte[] getRgbaBytes() {
		byte[] rgba = new byte[bytes.length * 4];
		byte[] palBytes = palette.getBytes();
		
		for(int i = 0; i < bytes.length; i++) {
			int c = bytes[i];
			rgba[i * 4 + 0] = palBytes[c * 4 + 0];
			rgba[i * 4 + 1] = palBytes[c * 4 + 1];
			rgba[i * 4 + 2] = palBytes[c * 4 + 2];
			rgba[i * 4 + 3] = palBytes[c * 4 + 3];
		}
		return rgba;
	}
}
