package net.georgewhiteside.android.abstractart;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity //implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    getPreferenceManager().setSharedPreferencesName(Wallpaper.SHARED_PREFS_NAME);
	    addPreferencesFromResource(R.xml.settings);
	    //getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume()
	{
	    super.onResume();
	}

	@Override
	protected void onDestroy()
	{
	    //getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    super.onDestroy();
	}

	/*public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		// placeholder
	}*/
}





