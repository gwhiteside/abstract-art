package net.georgewhiteside.android.aapreset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Layer implements Updateable {
	
	Image image;
	PaletteEffect paletteEffect;
	DistortionEffect distortionEffect;
	TranslationEffect translationEffect;
	
	List<Updateable> updateables = new ArrayList<Updateable>();
	
	public Layer(JSONObject layerObject, Map<String, byte[]> resources) throws JSONException {
		JSONObject imageObject = layerObject.getJSONObject("image");
		image = new Image(imageObject, resources);
		
		JSONObject jsonPaletteEffect = layerObject.optJSONObject("paletteEffect");
		if(jsonPaletteEffect != null) {
			paletteEffect = new PaletteEffect(jsonPaletteEffect);
			updateables.add(paletteEffect);
		}
		
		JSONArray jsonDistortionEffect = layerObject.optJSONArray("distortionEffects");
		if(jsonDistortionEffect != null) {
			distortionEffect = new DistortionEffect(jsonDistortionEffect);
			updateables.add(distortionEffect);
		}
		
		JSONArray jsonTranslationEffect = layerObject.optJSONArray("translationEffects");
		if(jsonTranslationEffect != null) {
			translationEffect = new TranslationEffect(jsonTranslationEffect);
			updateables.add(translationEffect);
		}
	}
	
	public void update(float deltaTime) {
		for(Updateable effect : updateables) {
			effect.update(deltaTime);
		}
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public PaletteEffect getPaletteEffect() {
		return paletteEffect;
	}

	public void setPaletteEffect(PaletteEffect paletteEffect) {
		this.paletteEffect = paletteEffect;
	}

	public DistortionEffect getDistortionEffect() {
		return distortionEffect;
	}

	public void setDistortionEffect(DistortionEffect distortionEffect) {
		this.distortionEffect = distortionEffect;
	}

	public TranslationEffect getTranslationEffect() {
		return translationEffect;
	}

	public void setTranslationEffect(TranslationEffect translationEffect) {
		this.translationEffect = translationEffect;
	}
}