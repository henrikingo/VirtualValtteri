package cc.openlife.virtualvaltteri;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import cc.openlife.virtualvaltteri.speaker.Speaker;
import cc.openlife.virtualvaltteri.speaker.VeryShort;

public class SettingsFragment extends PreferenceFragment {
    private Preference.OnPreferenceChangeListener prefChanged;
    public SettingsFragment(Preference.OnPreferenceChangeListener prefChanged){
        this.prefChanged = prefChanged;
    }
    @Override
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
}