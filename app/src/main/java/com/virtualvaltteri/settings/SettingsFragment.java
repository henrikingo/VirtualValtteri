package com.virtualvaltteri.settings;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.virtualvaltteri.MainActivity;
import com.virtualvaltteri.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
    public CharSequence[] sortedDrivers2;
    public DynamicMultiSelectListPreference driversPreference;
    public MultiSelectListPreference favoritedDriversPreference;
    Preference.OnPreferenceChangeListener prefChanged;
    boolean refreshing=false;
    private boolean androidBrokeThis=false;
    Intent returnIntent;

    public SettingsFragment(){
        super();
    }

    public SettingsFragment(Bundle savedInstanceState){
        System.out.println("Would you prefer this constructor if I create it?");
        //restoreState(savedInstanceState);

    }

    public SettingsFragment(Preference.OnPreferenceChangeListener prefChanged, SortedSet<String> sortedDrivers2){
        this.prefChanged=prefChanged;
        this.sortedDrivers2=sortedDrivers2.toArray(new CharSequence[0]);
    }


    public CharSequence driverInSession(CharSequence matchDriver, boolean matchPrefix){
        return _matchDriver(sortedDrivers2, matchDriver, matchPrefix, true);
    }

    public static CharSequence _matchDriver(CharSequence[] sortedDrivers2, CharSequence matchDriver, boolean matchPrefix, boolean currentSession){
        if(matchDriver==null) return null;
        if(matchPrefix && !matchDriver.equals("")){
            matchDriver = new StringBuilder("^").append(matchDriver).append(".*");
        }
        else {
            matchDriver = new StringBuilder("^").append(matchDriver).append("$");
        }

        Pattern pattern = Pattern.compile(matchDriver.toString(), Pattern.CASE_INSENSITIVE);
        if(sortedDrivers2!=null){
            for(CharSequence d: sortedDrivers2){
                Matcher matcher = pattern.matcher(d);
                if(matcher.find()) {
                    // Return the exact string used as the value for the driver setting
                    return d;
                }
            }
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequenceArray("sortedDrivers2", sortedDrivers2);
    }

    public void restoreState(Bundle savedInstanceState){
        System.out.println(Arrays.asList(sortedDrivers2));
        System.out.println(savedInstanceState);
        if(savedInstanceState!=null) {
            CharSequence[] newSortedDrivers = savedInstanceState.getCharSequenceArray("sortedDrivers");
            if (newSortedDrivers!=null){
            }
            CharSequence[] newSortedDrivers2 = savedInstanceState.getCharSequenceArray("sortedDrivers2");
            if (newSortedDrivers2!=null){
                //this.sortedDrivers2=newSortedDrivers2;
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        System.out.println("SettingsFragment.onCreate() " + rootKey + " " + savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        System.out.println("onCreatePreferences()");
        restoreState(savedInstanceState);
        //this.sortedDrivers = ((SettingsActivity)getActivity()).sortedDriversArray;
        this.prefChanged = ((SettingsActivity)getActivity()).prefChanged;

        if(refreshing){
            System.out.println("skipping refresh, already done somewhere else/ we're looping");
        }
        refreshing = true;
        refreshEverything();
        refreshing = false;

        // Register listeners ...
        driversPreference.setOnPreferenceChangeListener(prefChanged);
        final Preference prefList = findPreference("speaker_key");
        prefList.setOnPreferenceChangeListener(prefChanged);

        SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if(refreshing){
                    System.out.println("skipping refresh, already done somewhere else/loop prevention");
                    return;
                }
                refreshing = true;
                refreshEverything();
                refreshing = false;
            }
        });
        EditTextPreference writein = (EditTextPreference) findPreference("writein_driver_name_key");
        SwitchPreference autoFavoritePreference = ((SwitchPreference)findPreference("auto_favorite_key"));
        writein.setOnPreferenceChangeListener(this);
        autoFavoritePreference.setOnPreferenceChangeListener(this);
        driversPreference.setOnPreferenceChangeListener(this);
        favoritedDriversPreference.setOnPreferenceChangeListener(this);

    }
    public void refreshEverything (){

        // below line is used to add preference
        // fragment from our xml folder.

        CharSequence writeInName = ((EditTextPreference)findPreference("writein_driver_name_key")).getText();
        if(writeInName!=null)
            writeInName= ((String) writeInName).toLowerCase();
        boolean matchPrefix = ((SwitchPreference)findPreference("match_writein_prefix_key")).isChecked();
        System.out.println("writein name: " + writeInName);
        boolean autoFavorite = ((SwitchPreference)findPreference("auto_favorite_key")).isChecked();
        // Populate with drivers from the latest race
        driversPreference = (DynamicMultiSelectListPreference) findPreference("drivers_key");
        driversPreference.setEntries(sortedDrivers2);
        driversPreference.setEntryValues(sortedDrivers2);
        Set<String> driversNotInThisSession = driversPreference.getReducedValues(false);
        Set<String> followDriversInThisSession = driversPreference.getReducedValues(true);
        CharSequence[] allDriversInThisSession = driversPreference.getEntryValues();
        System.out.println(driversNotInThisSession);
        System.out.println(followDriversInThisSession);

        CharSequence writeInNameFound = driverInSession(writeInName, matchPrefix);
        System.out.println("Writein name in session: " +writeInNameFound);
        if( writeInName!=null && (!writeInName.equals("")) && writeInNameFound != null){
            System.out.println("Adding writein driver");
            followDriversInThisSession.add(writeInNameFound.toString());
            driversPreference.setValues(followDriversInThisSession);
        }

        // favoritedDriversPreference are saved values from driversPreference that aren't driving
        // in the current session. This preference is not persisted. It is essentially a helper or sidecar to the previous list.
        favoritedDriversPreference = (MultiSelectListPreference) findPreference("favorited_drivers_key");
        Set<String> favDrivers = favoritedDriversPreference.getValues();
        // For whatever reason simple favDrivers.addAll(driversNotInThisSession) refused to work...
        SortedSet<String> newFavDrivers = new ConcurrentSkipListSet<>();
        for(String s: favDrivers){
            // System.out.println(s);
            newFavDrivers.add(s);
        }
        for(String s: driversNotInThisSession){
            //System.out.println(s);
            newFavDrivers.add(s);
        }

        System.out.println("favDrivers: " + favDrivers);
        //favDrivers.addAll(driversNotInThisSession);
        // Add all old follows to favorites, and make them checked/on
        System.out.println(autoFavorite);
        if(autoFavorite){
            favoritedDriversPreference.setValues(newFavDrivers);
        }
        System.out.println(favoritedDriversPreference.getValues());
        // Otherwise add them to the list but leave them unchecked. They will eventually disappear if not checked.
        favoritedDriversPreference.setEntries((CharSequence[]) newFavDrivers.toArray(new CharSequence[0]));
        favoritedDriversPreference.setEntryValues((CharSequence[]) newFavDrivers.toArray(new CharSequence[0]));



        System.out.println(favoritedDriversPreference.getValues());

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

        if(allDriversInThisSession!=null){
            for (CharSequence s: allDriversInThisSession){
                if(favDrivers.contains(s) && !followDriversInThisSession.contains(s)){
                    followDriversInThisSession.add(s.toString());
                }
            }
        }

        driversPreference.setValues(followDriversInThisSession);
        System.out.println("driversPreference.getValues "  +driversPreference.getValues());


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

        if(getActivity()!=null){

        Set<String> values = driversPreference.getReducedValues(true);
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.SHARED_PREFS_MAGIC_WORD, MODE_PRIVATE);
        prefs.edit().putStringSet("follow_driver_names_key", values).commit();
        }

    }
    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        System.out.println("onPreferenceChange");
        if(refreshing){
            System.out.println("skipping refresh, already done somewhere else");
        }
        refreshing = true;
        refreshEverything();
        if(preference.getKey().equals("drivers_key")){
            DynamicMultiSelectListPreference thePref = (DynamicMultiSelectListPreference) preference;
            boolean matchPrefix = ((SwitchPreference)findPreference("match_writein_prefix_key")).isChecked();
            EditTextPreference writeInNamePreference = (EditTextPreference) findPreference("writein_driver_name_key");
            CharSequence writeInName = writeInNamePreference.getText();
            CharSequence writeInNameFound = driverInSession(writeInName, matchPrefix);
            System.out.println("writein name: " + writeInNameFound);
            System.out.println("Writein name in session: " +writeInNameFound);
            if( writeInName!=null && (!writeInName.equals("")) && writeInNameFound != null){
                System.out.println("Adding writein driver v2");
                Set<String> newFollowDrivers = DynamicMultiSelectListPreference.getReducedValuesHelper(true,(Set<String>) newValue,thePref.getEntryValues());
                System.out.println(newFollowDrivers);
                newFollowDrivers.add(writeInNameFound.toString());
                System.out.println(newFollowDrivers);
                driversPreference.setValues(newFollowDrivers);


                favoritedDriversPreference = (MultiSelectListPreference) findPreference("favorited_drivers_key");
                Set<String> favDrivers = favoritedDriversPreference.getValues();
                for (CharSequence s: favDrivers){
                    if(!newFollowDrivers.contains(s)){
                        newFollowDrivers.add(s.toString());
                    }
                }

                driversPreference.setValues(newFollowDrivers);
                refreshing=false;
                // Returning false to caller means we already took care of everything, don't go and set any values behind our back
                return false;
            }
        }
        if(preference.getKey().equals("writein_driver_name_key")){
            CharSequence writeInName = newValue.toString();
            boolean matchPrefix = ((SwitchPreference)findPreference("match_writein_prefix_key")).isChecked();
            System.out.println("writein name: " + writeInName);
            driversPreference = (DynamicMultiSelectListPreference) findPreference("drivers_key");
            CharSequence writeInNameFound = driverInSession(writeInName, matchPrefix);
            System.out.println("Writein name in session: " +writeInNameFound);
            if( writeInName!=null && (!writeInName.equals("")) && writeInNameFound != null){
                System.out.println("Adding writein driver");
                Set<String> followDriversInThisSession = driversPreference.getReducedValues(true);
                System.out.println(followDriversInThisSession);
                followDriversInThisSession.add(writeInNameFound.toString());
                driversPreference.setValues(followDriversInThisSession);
            }

        }
        refreshing = false;
        return true;
    }
}
