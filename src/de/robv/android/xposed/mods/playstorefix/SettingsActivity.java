package de.robv.android.xposed.mods.playstorefix;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        // Display the fragment as the main content.
        if (savedInstanceState == null)
			getFragmentManager().beginTransaction().replace(android.R.id.content,
	                new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment
			implements SharedPreferences.OnSharedPreferenceChangeListener {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			addPreferencesFromResource(R.xml.preferences);
			
			SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();
			sharedPref.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(sharedPref, getString(R.string.pref_density_key));
            onSharedPreferenceChanged(sharedPref, getString(R.string.pref_layout_key));
            onSharedPreferenceChanged(sharedPref, getString(R.string.pref_auto_key));
        }

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(getString(R.string.pref_density_key))) {
				EditTextPreference pref = (EditTextPreference) findPreference(key);
				String value = sharedPreferences.getString(key, "");
				if (value.isEmpty()) {
					value = "(unchanged)";
				} else if (!value.matches("\\d{2,3}")) {
					pref.setText("");
					Toast.makeText(getActivity(), "Invalid density value", Toast.LENGTH_SHORT).show();
					return;
				}
				
				if (isAdded())
					pref.setSummary(getString(R.string.pref_density_summary, value));
			}else if(key.equals(getString(R.string.pref_layout_key))){
                ListPreference pref = (ListPreference) findPreference(key);
                String value = sharedPreferences.getString(key, "");
                pref.setSummary(getString(R.string.pref_layout_summary,
                        CalculateLayout.toString(value)));
            }else if(key.equals(getString(R.string.pref_auto_key))){
                ListPreference pref = (ListPreference) findPreference(getString(R.string.pref_layout_key));
                pref.setEnabled(!sharedPreferences.getBoolean(key, false));
            }
		}
	}

}
