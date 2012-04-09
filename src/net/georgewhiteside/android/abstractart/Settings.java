package net.georgewhiteside.android.abstractart;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	    getPreferenceManager().setSharedPreferencesName(Wallpaper.SHARED_PREFS_NAME);
	    addPreferencesFromResource(R.xml.settings);
	}
	
	@Override
	protected void onPause()
	{
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	    super.onPause();
	}

	@Override
	protected void onResume()
	{
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	    super.onResume();
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		// automatically set listpreference summaries to reflect their current values
	    Preference preference = findPreference(key);

	    if (preference instanceof ListPreference) {
	        ListPreference listPreference = (ListPreference)preference;
	        preference.setSummary(listPreference.getEntry());
	        Log.i("bbdebug", (String) listPreference.getEntry());
	    }
	}
}





