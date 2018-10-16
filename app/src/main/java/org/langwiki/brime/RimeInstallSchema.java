package org.langwiki.brime;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.langwiki.brime.schema.ExternalStorage;
import org.langwiki.brime.schema.IMDF;
import org.langwiki.brime.schema.SchemaManager;

import java.util.List;

import jline.internal.Nullable;

public class RimeInstallSchema extends PreferenceActivity implements SchemaManager.SchemaManagerListener {
    private static final String TAG = IMEConfig.TAG;
    public static final String SDCARD_IS_NOT_WRITABLE = "SDCard is not writable!";
    PreferenceCategory schemaParent;
    SchemaManager sm;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.pref_rime_install_schemata);

        schemaParent = (PreferenceCategory)findPreference("rime_schemata");
        sm = SchemaManager.getInstance();
        sm.addListener(RimeInstallSchema.this);

        startDownload(false);
    }

    protected void startDownload(final boolean refresh) {
        new Thread() {
            @Override
            public void run() {
                if (refresh) {
                    sm.clearCache();
                }
                sm.getInstallList();
            }
        }.start();
    }

    @Override
    public void onSchemaList(@Nullable List<IMDF> list) {
        if (list == null) {
            return;
        }

        // Add schema checkboxes
        for (IMDF imdf : list) {
            SchemaManager sm = SchemaManager.getInstance();
            String name = sm.getLocaleString(imdf.name);
            if (name == null) {
                name = imdf.id;
            }

            CheckBoxPreference pref = new CheckBoxPreference(this);
            pref.setTitle(name);
            pref.setKey(imdf.id);

            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (!ExternalStorage.isWritable()) {
                        Toast.makeText(RimeInstallSchema.this,
                                SDCARD_IS_NOT_WRITABLE,
                                Toast.LENGTH_LONG);
                        return true;
                    }

                    final String id = preference.getKey();
                    new Thread() {
                        @Override
                        public void run() {
                            SchemaManager.getInstance().installSchema(id, true);
                        }
                    }.start();

                    return true;
                }
            });

            schemaParent.addPreference(pref);
        }
    }

    public void refresh(View view) {
        Log.d(TAG, "refresh");
        schemaParent.removeAll();
        startDownload(true);
    }
}
