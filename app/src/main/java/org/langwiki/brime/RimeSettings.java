package org.langwiki.brime;

import android.preference.PreferenceActivity;
import android.content.SharedPreferences;

public class RimeSettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "RimeSettings";

    private static final String QUICK_FIXES_KEY = "quick_fixes";
    private static final String PREDICTION_SETTINGS_KEY = "prediction_settings";

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }
}
