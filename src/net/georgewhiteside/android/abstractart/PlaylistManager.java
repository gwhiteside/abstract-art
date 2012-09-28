package net.georgewhiteside.android.abstractart;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;

public class PlaylistManager {

	Context context;
	Set<String> playlist;
	
	public PlaylistManager(Context context) {
		this.context = context;
		playlist = new HashSet<String>();
	}

	public void createDefault(PresetManager presetManager) {
		
	}

}
