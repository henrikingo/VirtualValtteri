package cc.openlife.virtualvaltteri;

import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Preference.OnPreferenceChangeListener prefChanged;
    public CharSequence[] sortedDrivers;
    public DynamicMultiSelectListPreference driversPreference;
    public MultiSelectListPreference favoritedDriversPreference;
    public Preference speakerPreference;
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
        System.out.println("Populate follow driver list (Settings)");
        driversPreference = (DynamicMultiSelectListPreference) findPreference("drivers_key");
        driversPreference.setEntries(sortedDrivers);
        driversPreference.setEntryValues(sortedDrivers);


        // favoritedDriversPreference are saved values from driversPreference that aren't driving
        // in the current session. This preference is not persisted. It is essentially a helper or sidecar to the previous list.
        favoritedDriversPreference = (MultiSelectListPreference) findPreference("favorited_drivers_key");
        Set<String> favDriversCS = driversPreference.getReducedValues(false);
        favoritedDriversPreference.setEntries(favDriversCS.stream().toArray(CharSequence[]::new));
        favoritedDriversPreference.setEntryValues(favDriversCS.stream().toArray(CharSequence[]::new));
        // In this list everything is always selected
        Set<String> favDriversString = new HashSet<>();
        for(CharSequence cs: favDriversCS)
            favDriversString.add((String) cs);

        favoritedDriversPreference.setValues(favDriversString);
        favoritedDriversPreference.setSummaryProvider(new Preference.SummaryProvider() {
            @Nullable
            @Override
            public CharSequence provideSummary(@NonNull Preference preference) {
                String text = "";
                StringBuilder builder = new StringBuilder();
                Set<String> values = driversPreference.getReducedValues(false);
                System.out.println("favoritedDrivers.getSummary() values: " + values);
                boolean notEmpty = false;
                for (CharSequence v: values) {
                    if(values.contains(v)) {
                        if(notEmpty){
                            builder.append(", ");
                        }
                        builder.append(v);
                        notEmpty=true;
                    }
                }
                // Leftovers will be shown in their own MultiSelectList. SettingsFragment will take it from here
                text = builder.toString();
                return text;
            }
        });

        // Register listeners ...
        favoritedDriversPreference.setOnPreferenceChangeListener(prefChanged);
        driversPreference.setOnPreferenceChangeListener(prefChanged);
        speakerPreference = findPreference("speaker_key");
        speakerPreference.setOnPreferenceChangeListener(prefChanged);
    }
}
