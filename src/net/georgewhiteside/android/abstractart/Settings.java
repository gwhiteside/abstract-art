package net.georgewhiteside.android.abstractart;

import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
	    super.onCreate(savedInstanceState);
	    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	    addPreferencesFromResource(R.xml.settings);
	    
	    initSummaries(getPreferenceScreen());
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// automatically set listpreference summaries to reflect their current values
	    Preference preference = findPreference(key);
	    setSummary(preference);
	}
	
	/**
	 * Recursively initializes preference item summaries to their current preference values
	 * 
	 * @param preferenceGroup the top level PreferenceGroup
	 */
	private void initSummaries(PreferenceGroup preferenceGroup) {
		for(int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
	    	Preference preference = preferenceGroup.getPreference(i);
	    	if(preference instanceof PreferenceGroup) {
	    		initSummaries((PreferenceGroup)preference);
	    	} else {
	    		setSummary(preference);
	    	}
	    }
	}
	
	private void setSummary(Preference preference) {
		if(preference instanceof ListPreference) {
			ListPreference listPreference = (ListPreference)preference;
			preference.setSummary(listPreference.getEntry());
		}
	}
}





