package org.langwiki.brime;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SettingManager {
    public static final String KEY_CURRENT_RIME_SCHEMA_ID = "current_rime_schema_id";

    private Context context;
    private SharedPreferences prefs;

    public SettingManager(Context ctx) {
        context = ctx;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    private void setValue(String key, String val) {
        SharedPreferences.Editor e = prefs.edit();
        e.putString(key, val);
        e.commit();
    }

    public String getCurrentRimeSchemaId() {
        return prefs.getString(KEY_CURRENT_RIME_SCHEMA_ID, null);
    }

    public void setCurrentRimeSchemaId(String id) {
        setValue(KEY_CURRENT_RIME_SCHEMA_ID, id);
    }
}
