package cc.openlife.virtualvaltteri;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Preference.OnPreferenceChangeListener prefChanged;
    public CharSequence[] sortedDrivers;
    public DynamicMultiSelectListPreference driversPreference;
    public MultiSelectListPreference favoritedDriversPreference;
    public Preference speakerPreference;
    Intent returnIntent;
    SharedPreferences p;
    public SettingsFragment(Preference.OnPreferenceChangeListener prefChanged, CharSequence[] sortedDrivers, Intent returnIntent, SharedPreferences p){
        this.prefChanged = prefChanged;
        this.sortedDrivers = sortedDrivers;
        this.returnIntent = returnIntent;
        this.p = p;
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
        Set<String> newFavDrivers = new HashSet<>();
        for(String s: favDrivers){
            System.out.println(s);
            newFavDrivers.add(s);
        }
        for(String s: driversNotInThisSession){
            System.out.println(s);
            newFavDrivers.add(s);
        }
        System.out.println("favDrivers: " + favDrivers);
        boolean res = favDrivers.addAll(driversNotInThisSession);
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


/*
        Set<String> favDrivers = driversPreference.getReducedValues(false);
        favDrivers.addAll(driversNotInThisSession);
        Set<String> favDrivers = driversPreference.getReducedValues(false);
        favoritedDriversPreference.setEntries(favDriversCS.stream().toArray(CharSequence[]::new));
        favoritedDriversPreference.setEntryValues(favDriversCS.stream().toArray(CharSequence[]::new));
        // In this list everything is always selected
        Set<String> favDriversString = new HashSet<>();
        for(CharSequence cs: favDriversCS)
            favDriversString.add((String) cs);

        favoritedDriversPreference.setValues(favDriversString);
        */
        favoritedDriversPreference.setSummaryProvider(new Preference.SummaryProvider() {
            @Nullable
            @Override
            public CharSequence provideSummary(@NonNull Preference preference) {
                String text = "";
                StringBuilder builder = new StringBuilder();
                //Set<String> values = driversPreference.getReducedValues(false);
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
                // Leftovers will be shown in their own MultiSelectList. SettingsFragment will take it from here
                text = builder.toString();
                return text;
            }
        });

        // Register listeners ...
        Preference.OnPreferenceChangeListener prefChanged2 = new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object value) {
                System.out.println("prefChanged: " + preference.getKey() + " = " + value);
                if(preference.getKey().equals("speaker_key")){
                    returnIntent.putExtra("settings_speaker",value.toString());
                    // This is called in SettingsActivity.onStop();
                    //setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                if(preference.getKey().equals("drivers_key")){
                    DynamicMultiSelectListPreference driversPreference = (DynamicMultiSelectListPreference) preference;
                    Set<String> driversValuesCurrent = driversPreference.getReducedValues(true);
                    System.out.println("pref change handler, getReducedValues: " + driversValuesCurrent);
                    ArrayList<CharSequence> csList = new ArrayList<>(Collections.emptyList());
                    HashSet<CharSequence> valueAsHash = (HashSet<CharSequence>)value;
                    for(CharSequence cs: valueAsHash){
                        System.out.println(cs);
                        csList.add(cs);
                    }
                    returnIntent.putCharSequenceArrayListExtra("settings_drivers",csList);
                    // This is called in SettingsActivity.onStop();
                    //setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                // Logic: All the drivers ever followed remain in driversPreference. We just don't show them in the list or getSummary();
                // Only once unselected from this list do we also remove the same from driversPreference.
                // In mathematical terms, a driver is removed if they aren't driving in this session and aren't selected in favoritedDrivers;
                if(preference.getKey().equals("favorited_drivers_key")){
                    System.out.println("favorited_drivers_key");
                    System.out.println(value);
                    final MultiSelectListPreference favoritedDriversPreference = (MultiSelectListPreference) preference;
                    final DynamicMultiSelectListPreference driversPreference = findPreference("drivers_key");
                    Set<String> newOldFollows = (HashSet<String>) value;
                    Set<String> oldOldFollows = favoritedDriversPreference.getValues();
                    System.out.println("oldoldfollows"+ oldOldFollows);
                    System.out.println("newoldfollows"+newOldFollows);
                    // Subtract
                    // Note that for this widget you can only ever unselect widgets
                    assert newOldFollows.size() <= oldOldFollows.size();
                    oldOldFollows.removeAll(newOldFollows);
                    Set<String> diff =  oldOldFollows;
                    System.out.println("diff"+ diff);

                    Set<String> currentFollows = new HashSet<>();
                    currentFollows = p.getStringSet("drivers_key", currentFollows);
                    currentFollows = driversPreference.getValues();
                    System.out.println("currentfollows"+ currentFollows);

                    currentFollows.removeAll(diff);
                    System.out.println("newCurrentFollows "+ currentFollows);
                    p.edit().putStringSet("drivers_key", currentFollows);
                    driversPreference.setValues(currentFollows);


                    favoritedDriversPreference.setValues(newOldFollows);

                    ArrayList<String> a = new ArrayList<>();
                    a.addAll(currentFollows);
                    returnIntent.putStringArrayListExtra("settings_drivers",a);
                    // This is called in SettingsActivity.onStop();
                    //setResult(Activity.RESULT_OK,returnIntent);
                    return true;
                }
                return false;
            }
        };
/*        favoritedDriversPreference.setOnPreferenceChangeListener(prefChanged2);
        driversPreference.setOnPreferenceChangeListener(prefChanged2);
        speakerPreference = findPreference("speaker_key");
        speakerPreference.setOnPreferenceChangeListener(prefChanged2);
        */
    }

}
