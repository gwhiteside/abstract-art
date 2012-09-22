package net.georgewhiteside.android.aapreset;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Sprite {

	byte[] bytes;
	int amount;
	
	public Sprite(JSONObject spriteObject, Map<String, byte[]> resources) throws JSONException {
		String file = spriteObject.getString("file");
		bytes = resources.get(file);
		amount = spriteObject.getInt("amount");
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}
}
