package cc.openlife.virtualvaltteri;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Set;

public class DynamicMultiSelectListPreference extends MultiSelectListPreference {
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
        String text = "";
        StringBuilder builder = new StringBuilder();
        Set<String> values = getValues();
        int pos=0;
        System.out.println("getSummary() values: " + values);
            for (String value : values) {
                builder.append(value);
                pos++;
                if (pos < values.size())
                    builder.append(", ");
            }
            text = builder.toString();
        return text;
    }
}
