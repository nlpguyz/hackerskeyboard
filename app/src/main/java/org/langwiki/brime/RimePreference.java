package org.langwiki.brime;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RimePreference extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "BRime-Pref";

    private List<CheckBoxPreference> mSchemaPrefs = new ArrayList<>();

    private String selected;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.pref_rime);

        // Add schema checkboxes
        List<Map<String, String>> schemas = Rime.getInstance().get_available_schema_list();

        PreferenceGroup parent = getPreferenceScreen();
        PreferenceCategory schemaParent = (PreferenceCategory)findPreference("rime_schemata");

        for (Map<String, String> schema : schemas) {
            String name = schema.get("name");
            String id = schema.get("schema_id");

            CheckBoxPreference pref = new CheckBoxPreference(this);
            pref.setTitle(name);
            pref.setKey(id);
            pref.setOnPreferenceChangeListener(this);
            mSchemaPrefs.add(pref);

            schemaParent.addPreference(pref);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value= ((Boolean)newValue).booleanValue();

        // Enforce one selection
        if (value) {
            selected = preference.getKey();
            for (CheckBoxPreference p : mSchemaPrefs) {
                if (p != preference) {
                    p.setChecked(false);
                }
            }
        }

        return true;
    }
}
