package apps.mai.sunshine;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // add preferences that defined in xml folder
        addPreferencesFromResource(R.xml.pref_general);
        //for all preferences, we attach OnPreferenceChangeListener to update summery UI
        // when the preference changes
        bindPreferenceSummeryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummeryToValue(findPreference(getString(R.string.pref_units_key)));

    }
    private void bindPreferenceSummeryToValue(Preference preference){
        preference.setOnPreferenceChangeListener(this);
        onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(this)
                .getString(preference.getKey(),""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String stringValue=o.toString();
        if (preference instanceof ListPreference){
            ListPreference listPreference= (ListPreference) preference;
            int prefIndex=listPreference.findIndexOfValue(stringValue);
            if (prefIndex>=0){
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        }
        else {
            preference.setSummary(stringValue);
        }
        return true;
    }



}
