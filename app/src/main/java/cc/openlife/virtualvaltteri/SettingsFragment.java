package cc.openlife.virtualvaltteri;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListSet;

public class SettingsFragment extends PreferenceFragmentCompat {
    public CharSequence[] sortedDrivers;
    public DynamicMultiSelectListPreference driversPreference;
    public MultiSelectListPreference favoritedDriversPreference;
    Intent returnIntent;
    public SettingsFragment(Preference.OnPreferenceChangeListener prefChanged, CharSequence[] sortedDrivers, Intent returnIntent){
        this.sortedDrivers = sortedDrivers;
        this.returnIntent = returnIntent;
    }

    @SuppressLint("NewApi")
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
        Set<String> driversNotInThisSession = driversPreference.getReducedValues(false);
        Set<String> followDriversInThisSession = driversPreference.getReducedValues(true);
        CharSequence[] allDriversInThisSession = driversPreference.getEntryValues();
        System.out.println(driversNotInThisSession);
        System.out.println(followDriversInThisSession);
        System.out.println(allDriversInThisSession.length);

        // favoritedDriversPreference are saved values from driversPreference that aren't driving
        // in the current session. This preference is not persisted. It is essentially a helper or sidecar to the previous list.
        favoritedDriversPreference = (MultiSelectListPreference) findPreference("favorited_drivers_key");
        Set<String> favDrivers = favoritedDriversPreference.getValues();
        // For whatever reason simple favDrivers.addAll(driversNotInThisSession) refused to work...
        Set<String> newFavDrivers = new ConcurrentSkipListSet<>();
        for(String s: favDrivers){
            System.out.println(s);
            newFavDrivers.add(s);
        }
        for(String s: driversNotInThisSession){
            System.out.println(s);
            newFavDrivers.add(s);
        }
        System.out.println("favDrivers: " + favDrivers);
        favDrivers.addAll(driversNotInThisSession);
        favoritedDriversPreference.setValues(newFavDrivers);
        favoritedDriversPreference.setEntries((CharSequence[]) newFavDrivers.toArray(new CharSequence[0]));
        favoritedDriversPreference.setEntryValues((CharSequence[]) newFavDrivers.toArray(new CharSequence[0]));
        System.out.println("newfavDrivers: " + " " + newFavDrivers);

        // In this list we remove items that are unselected
        CharSequence[] allEntries = favoritedDriversPreference.getEntryValues();
        Vector<CharSequence> prunedEntries = new Vector<CharSequence>();
        for(CharSequence cs:allEntries){
            if(newFavDrivers.contains(cs.toString())){
                prunedEntries.add(cs);
            }
        }
        System.out.println("pruned favorites: " + prunedEntries);
        if(prunedEntries.size()>0){
            favoritedDriversPreference.setEntries((CharSequence[]) prunedEntries.toArray(new CharSequence[0]));
            favoritedDriversPreference.setEntryValues((CharSequence[]) prunedEntries.toArray(new CharSequence[0]));
        }

        for (CharSequence s: allDriversInThisSession){
            if(favDrivers.contains(s) && !followDriversInThisSession.contains(s)){
                followDriversInThisSession.add(s.toString());
            }
        }
        driversPreference.setValues(followDriversInThisSession);


        favoritedDriversPreference.setSummaryProvider(new Preference.SummaryProvider() {
            @Nullable
            @Override
            public CharSequence provideSummary(@NonNull Preference preference) {
                String text = "";
                StringBuilder builder = new StringBuilder();
                Set<String> values = ((MultiSelectListPreference)preference).getValues();
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
                text = builder.toString();
                return text;
            }
        });
    }

}
