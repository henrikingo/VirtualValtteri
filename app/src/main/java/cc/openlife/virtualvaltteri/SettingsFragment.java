package cc.openlife.virtualvaltteri;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import cc.openlife.virtualvaltteri.speaker.Speaker;
import cc.openlife.virtualvaltteri.speaker.VeryShort;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Preference.OnPreferenceChangeListener prefChanged;
    private CharSequence[] sortedDrivers;
    public SettingsFragment(Preference.OnPreferenceChangeListener prefChanged, CharSequence[] sortedDrivers){
        this.prefChanged = prefChanged;
        this.sortedDrivers = sortedDrivers;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        System.out.println("SettingsFragment.onCreate() " + rootKey + " " + savedInstanceState);

        // below line is used to add preference
        // fragment from our xml folder.
        addPreferencesFromResource(R.xml.settings);

        // Populate with drivers from the latest race
        // TODO: Also show all selected and saved drivers, even when not driving
        System.out.println("Populate follow driver list (Settings)");
        final DynamicMultiSelectListPreference driversPreference = (DynamicMultiSelectListPreference) findPreference("drivers_key");
        System.out.println("drivers list before setEntries(): " + driversPreference.getEntries());
        System.out.println("selected drivers before setEntries(): " + driversPreference.getSummary());
        // Clear the placeholder value, or for that matter whatever old list is there, maybe from previous race, maybe it's the same list...
        driversPreference.setEntries(sortedDrivers);
        driversPreference.setEntryValues(sortedDrivers);


        // Register listeners ...
        driversPreference.setOnPreferenceChangeListener(prefChanged);
        final Preference prefList = findPreference("speaker_key");
        prefList.setOnPreferenceChangeListener(prefChanged);
    }

/*    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("SettingsFragment.onCreate() " + savedInstanceState);

        // below line is used to add preference
        // fragment from our xml folder.
        addPreferencesFromResource(R.xml.settings);

        // Register listeners ...
        final Preference prefList = findPreference("speaker_key");
        prefList.setOnPreferenceChangeListener(prefChanged);
    }
*/
}
