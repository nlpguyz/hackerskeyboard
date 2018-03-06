package org.langwiki.brime;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;

import org.langwiki.brime.schema.SchemaManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RimePreference extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "BRime-Pref";

    private List<CheckBoxPreference> mSchemaPrefs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.pref_rime);

        // Add schema checkboxes
        List<Map<String, String>> schemas = Rime.getInstance().get_available_schema_list();

        PreferenceGroup parent = getPreferenceScreen();
        PreferenceCategory schemaParent = (PreferenceCategory)findPreference("rime_schemata");

        String selectedId = SettingManager.getInstance().getCurrentRimeSchemaId();
        CheckBoxPreference first = null;
        CheckBoxPreference selected = null;

        if (schemas != null) {
            for (Map<String, String> schema : schemas) {
                String name = schema.get("name");
                String id = schema.get("schema_id");

                CheckBoxPreference pref = new CheckBoxPreference(this);
                pref.setTitle(name);
                pref.setKey(id);

                if (first == null) {
                    first = pref;
                }

                // select previous schema or first
                if (selected == null && selectedId != null && selectedId.equals(id)) {
                    selected = pref;
                }

                pref.setOnPreferenceChangeListener(this);
                mSchemaPrefs.add(pref);
                schemaParent.addPreference(pref);
            }
        }

        // Selected is gone. Use first.
        if (selected == null && first != null) {
            selected = first;
        }

        // Toggle here to avoid invoking listeners before all prefs are added
        if (selected != null) {
            selected.setChecked(true);
        }
    }

    @Override
    protected void onDestroy() {
        // Change schema on exit
        SchemaManager.getInstance().selectSchema(
                SettingManager.getInstance().getCurrentRimeSchemaId()
        );
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = ((Boolean)newValue).booleanValue();

        // Cannot deselect last schema
        if (value == false && preference.getKey().equals(SettingManager.getInstance().getCurrentRimeSchemaId())) {
            return false;
        }

        // Enforce one selection
        if (value) {
            for (CheckBoxPreference p : mSchemaPrefs) {
                if (p != preference) {
                    p.setChecked(false);
                }
            }
        }

        // Save
        SettingManager.getInstance().setCurrentRimeSchemaId(preference.getKey());

        return true;
    }
}
