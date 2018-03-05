package org.langwiki.brime;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;

import java.util.List;
import java.util.Map;

public class RimePreference extends PreferenceActivity {
    private static final String TAG = "BRime-Pref";

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
            schemaParent.addPreference(pref);
        }
    }
}
