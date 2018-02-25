package org.langwiki.brime.schema;

import android.content.res.Resources;
import android.content.Context;

import org.langwiki.brime.utils.ResourceFile;

import java.io.File;
import java.io.IOException;

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

    public void initializeDataDir(String userDir) {
        // Make sure the path exists
        File brimePath = new File(USER_DIR);
        brimePath.mkdir();

        for (String fn : brimeFiles) {
            try {
                String str = ResourceFile.loadFile(resources, "assets/" + fn, false);
                ResourceFile.save(USER_DIR + File.separator + fn, str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}