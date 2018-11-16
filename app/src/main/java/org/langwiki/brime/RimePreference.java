package org.langwiki.brime;

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.View;

import org.langwiki.brime.schema.SchemaManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RimePreference extends PreferenceActivity {
    private static final String TAG = IMEConfig.TAG + "-Pref";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new RimePreferenceFragment()).commit();
    }

    public static class RimePreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener{
        private List<CheckBoxPreference> mSchemaPrefs = new ArrayList<>();
        private SettingManager mSettingsManager;

        public RimePreferenceFragment() {
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mSettingsManager = new SettingManager(getContext());
            addPreferencesFromResource(R.xml.pref_rime);
        }

        private void refresh() {
            // Add schema checkboxes
            List<Map<String, String>> schemas = Rime.getInstance().get_available_schema_list();

            PreferenceCategory schemaParent = (PreferenceCategory)findPreference("rime_schemata");

            schemaParent.removeAll();
            mSchemaPrefs.clear();

            String selectedId = mSettingsManager.getCurrentRimeSchemaId();
            CheckBoxPreference first = null;
            CheckBoxPreference selected = null;

            if (schemas != null) {
                for (Map<String, String> schema : schemas) {
                    String name = schema.get("name");
                    String id = schema.get("schema_id");

                    CheckBoxPreference pref = new CheckBoxPreference(getContext());
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
                    pref.setChecked(false);
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

        public void onDeployButton(View v) {
            SchemaManager.getInstance().redeploy(getContext(), false, true);
        }

        @Override
        public void onResume() {
            super.onResume();
            refresh();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean value = ((Boolean)newValue).booleanValue();

            // Cannot deselect last schema
            if (value == false && preference.getKey().equals(mSettingsManager.getCurrentRimeSchemaId())) {
                return false;
            }

            // Enforce one selection
            if (value) {
                for (CheckBoxPreference p : mSchemaPrefs) {
                    if (!p.getKey().equals(preference.getKey())) {
                        p.setChecked(false);
                    }
                }

                // Save
                mSettingsManager.setCurrentRimeSchemaId(preference.getKey());

                // Enable
                SchemaManager.getInstance().selectSchema(preference.getKey());
            }

            return true;
        }

    }
}
