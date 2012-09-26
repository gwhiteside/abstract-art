package net.georgewhiteside.android.aapreset;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Sprite {

	Image image;
	int amount;
	int row;
	int textureId;
	
	public Sprite(JSONObject spriteObject, Map<String, byte[]> resources) throws JSONException {
		JSONObject imageObject = spriteObject.getJSONObject("image");
		image = new Image(imageObject, resources);
		amount = spriteObject.getInt("amount");
		row = spriteObject.getInt("row");
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}
	
	public Image getImage() {
		return image;
	}
	
	public int getHeight() {
		return image.getHeight();
	}
	
	public int getWidth() {
		return image.getWidth();
	}
	
	public int getRow() {
		return row;
	}
	
	public int getTextureId() {
		return textureId;
	}
	
	public void setTextureId(int id) {
		textureId = id;
	}
}
