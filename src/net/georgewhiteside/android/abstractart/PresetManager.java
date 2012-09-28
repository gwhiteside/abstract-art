package net.georgewhiteside.android.abstractart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.content.Context;

import net.georgewhiteside.android.aapreset.Preset;

public class PresetManager {

	// first check if preset exists in assets
	// if match, load; then check data folder for overrides?
	// if no match, check in data folder to load
	
	private Context context;
	private String presetsPath = "presets";
	
	private Map<String, Integer> counts;
	
	public PresetManager(Context context) {
		this.context = context;
		counts = new HashMap<String, Integer>();
	}
	
	public Preset load(String filename) {
		InputStream is = null;
		Preset preset = null;
		
		try {
			is = context.getAssets().open(presetsPath + File.separator + filename);
			preset = new Preset(is);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return preset;
	}
	
	public int getCount(String group) {
		if(!counts.containsKey(group)) {
			List<String> presets = listGroup(group);
			counts.put(group, presets.size());
		}
		return counts.get(group);
	}
	
	public List<String> getGroups() {
		List<String> groups = new ArrayList<String>();
		
		try {
			String[] files = context.getAssets().list(presetsPath);
			groups.addAll(Arrays.asList(files));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//File internalPresetDir = context.getFilesDir();
		
		return groups;
	}
	
	public List<String> listGroup(String group) {
		List<String> presets = new ArrayList<String>();
		
		try {
			String[] files = context.getAssets().list(presetsPath + File.separator + group);
			presets.addAll(Arrays.asList(files));
			
			// prepend the group path (relative to preset directory) to each filename in list
			for(int i = 0; i < presets.size(); i++) {
				String preset = presets.get(i);
				presets.set(i, group + File.separator + preset);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return presets;
	}

}
