package org.langwiki.brime.schema;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.Context;

import org.langwiki.brime.utils.ResourceFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SchemaManager {
    private static SchemaManager sInstance;
    private Context context;
    private Resources resources;

    private static final String USER_DIR = "/sdcard/brime";

    private static final String[] brimeFiles = {
            "default.yaml",
            "essay.txt",
            "luna_pinyin.dict.yaml",
            "luna_pinyin.schema.yaml",
            "symbols.yaml",
    };

    public static SchemaManager getInstance(Context context) {
        if (sInstance != null)
            return sInstance;

        synchronized (SchemaManager.class) {
            sInstance = new SchemaManager(context);
            return sInstance;
        }
    }

    public SchemaManager(Context context) {
        this.context = context;
        resources = context.getResources();
    }

    public void initializeDataDir() {
        // Check if the user dir has been initialized
        File checkFile = new File(USER_DIR + File.separator + "symbols.yaml");
        if (checkFile.exists()) {
            return;
        }

        // Make sure the path exists
        File brimePath = new File(USER_DIR);
        brimePath.mkdir();

        for (String fn : brimeFiles) {
            try {
                AssetManager assetMgr = context.getAssets();
                ResourceFile.save(USER_DIR + File.separator + fn, assetMgr.open(fn));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}