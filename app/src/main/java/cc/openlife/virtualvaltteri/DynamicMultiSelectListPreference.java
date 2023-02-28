package cc.openlife.virtualvaltteri;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DynamicMultiSelectListPreference extends MultiSelectListPreference {
    public Set<String> favoritedDrivers;
    public DynamicMultiSelectListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DynamicMultiSelectListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // Note: This is simple because our values are the same as the entries
    // The below is the same as ", ".join(getValues()), but juggling Strings, CharSequence and StringBuilder...
    @Override
    public CharSequence getSummary() {
        Set<String> values = getReducedValues(true);
        System.out.println("getSummary() values: " + values);
        StringBuilder builder = new StringBuilder();
        CharSequence[] entries = getEntries();
        boolean notEmpty = false;
        for (CharSequence entry: entries) {
            if(values.contains(entry)) {
                if(notEmpty){
                    builder.append(", ");
                }
                builder.append(entry);
                notEmpty=true;
            }
        }
        return builder.toString();
    }

    public static Set<String> getReducedValuesHelper(boolean currentSession, Set<String> values, CharSequence[] entries){
        Set<String> currentFollows = new HashSet<>();
        Set<String> oldFollows = new HashSet<>();

        // Values will include also drivers that were followed in previous races but aren't in this race.
        // For clarity, leave them out and show them in a separate list.
        for (CharSequence entry: entries) {
            if(values.contains(entry)) {
                currentFollows.add(entry.toString());
            }
        }
        for(String v: values){
            if(!currentFollows.contains(v))
                oldFollows.add(v);
        }
        if(currentSession){
            return currentFollows;
        }
        return oldFollows;
    }
    public Set<String> getReducedValues(boolean currentSession){
        Set<String> values = new HashSet<>(super.getValues());
        System.out.println("getReducedValues() initial getValues()" + values);
        CharSequence[] entries = getEntries();
        return getReducedValuesHelper(currentSession, values, entries);
    }

}
