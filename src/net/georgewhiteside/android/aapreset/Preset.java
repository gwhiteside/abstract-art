package net.georgewhiteside.android.aapreset;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Preset implements Updateable {
	
	String title;
	int letterboxSize;
	List<Layer> layers;
	List<Sprite> sprites;
	
	public Preset(InputStream aapreset) throws JSONException, IOException {
		String jsonString = null;
		Map<String, byte[]> resources = new HashMap<String, byte[]>();
		
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(aapreset));
		try {
			ZipEntry ze;
			byte[] buffer = new byte[1024];
			while ((ze = zis.getNextEntry()) != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int count;
				while ((count = zis.read(buffer)) != -1) {
					baos.write(buffer, 0, count);
				}
				String filename = ze.getName();
				byte[] bytes = baos.toByteArray();

				if(filename.equals("preset.json")) {
					jsonString = new String(bytes, "UTF-8");
				} else {
					resources.put(filename, bytes);
				}
			}
		} finally {
			zis.close();
		}
		
		JSONObject root = new JSONObject(jsonString);
		JSONObject data = root.getJSONObject("data");
		
		title = data.getString("title");
		
		letterboxSize = data.getInt("letterboxSize");
		
		layers = new ArrayList<Layer>();
		JSONArray layersArray = data.getJSONArray("layers");
		for(int i = 0; i < layersArray.length(); i++) {
			JSONObject layerObject = layersArray.getJSONObject(i);
			Layer layer = new Layer(layerObject, resources);
			layers.add(layer);
		}
		
		sprites = new ArrayList<Sprite>();
		JSONArray spritesArray = data.optJSONArray("sprites");
		if(spritesArray != null) {
			for(int i = 0; i < spritesArray.length(); i++) {
				JSONObject spriteObject = spritesArray.getJSONObject(i);
				Sprite sprite = new Sprite(spriteObject, resources);
				sprite.setTextureId(i); // identifier unique to the sprite's texture, but not the sprite itself
				sprites.add(sprite);
			}
		}
	}
	
	public void update(float deltaTime) {
		for(Layer layer : layers) {
			layer.update(deltaTime);
		}
	}
	
	public List<Layer> getLayers() {
		return layers;
	}
	
	public int getLetterbox() {
		return letterboxSize;
	}
	
	public List<Sprite> getSprites() {
		return sprites;
	}
	
	public String getTitle() {
		return title;
	}
}



/*

		List<? extends ZipEntry> entries = Collections.list(aapreset.entries());
		
		byte[] buffer = new byte[1024];
		for(ZipEntry entry : entries) {
			InputStream is = null;
			try {
				is = aapreset.getInputStream(entry);
				ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				
				int count;
				while ((count = zis.read(buffer)) != -1) {
					baos.write(buffer, 0, count);
				}
				String filename = entry.getName();
				byte[] bytes = baos.toByteArray();
				
				if(filename.equals("preset.json")) {
					jsonString = new String(bytes, "UTF-8");
				} else {
					resources.put(filename, bytes);
				}
				
			} catch (IOException e) {
				
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
		
*/