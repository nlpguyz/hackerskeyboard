package org.langwiki.brime;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;

import org.langwiki.brime.schema.IMDF;
import org.langwiki.brime.schema.SchemaManager;

import java.util.List;

import jline.internal.Nullable;

public class RimeInstallSchema extends PreferenceActivity implements SchemaManager.SchemaManagerListener {
    private static final String TAG = "BRime-Pref";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.pref_rime_install_schemata);

        PreferenceCategory schemaParent = (PreferenceCategory)findPreference("rime_schemata");
        //schemaParent.addPreference();

        startDownload();
    }

    protected void startDownload() {
        new Thread() {
            @Override
            public void run() {
                SchemaManager sm = SchemaManager.getInstance(getApplicationContext());
                sm.addListener(RimeInstallSchema.this);
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
        PreferenceCategory schemaParent = (PreferenceCategory)findPreference("rime_schemata");

        schemaParent.removeAll();

        for (IMDF imdf : list) {
            SchemaManager sm = SchemaManager.getInstance(getApplicationContext());
            String name = sm.getLocaleString(imdf.name);
            if (name == null) {
                name = imdf.id;
            }

            CheckBoxPreference pref = new CheckBoxPreference(this);
            pref.setTitle(name);
            schemaParent.addPreference(pref);
        }
    }
}
