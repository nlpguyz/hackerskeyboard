package org.langwiki.brime.schema;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.langwiki.brime.utils.FileHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SchemaManager {
    private static final String TAG = "BRime";

    private static SchemaManager sInstance;
    private Context context;
    private Resources resources;

    public static final String USER_DIR = "/sdcard/brime";
    public static final String DEFAULT_IMDF = "brime_basic.json";

    public static final String IMDF_SERVER_URL = "";

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
        // Make sure the path exists
        File brimePath = new File(USER_DIR);
        brimePath.mkdir();

        final AssetManager assetMgr = context.getAssets();
        FileOpener opener = new FileOpener() {
            @Override
            public InputStream open(String path) throws IOException {
                return assetMgr.open(path);
            }
        };
        try {
            InputStream is = assetMgr.open(DEFAULT_IMDF);
            String imdfText = FileHelper.read(is);
            IMDF imdf = parseImdf(imdfText);
            deployImdf(imdf, opener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deployImdf(IMDF imdf, FileOpener opener) {
        String versionFileName = USER_DIR + File.separator + imdf.name + ".ver";
        File versionFile = new File(versionFileName);
        String version = FileHelper.loadFile(versionFile, null);
        version = version.trim();
        if (version != null && version.compareTo(imdf.version) >= 0) {
            return;
        }

        for (String fn : imdf.files) {
            try {
                FileHelper.copyTo(opener.open(imdf.baseUrl + fn), USER_DIR + File.separator + fn);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileHelper.writeFile(versionFile, imdf.version);
    }

    public IMDF parseImdf(String imdfString) {
        Gson gson = new Gson();
        IMDF imdf = gson.fromJson(imdfString, IMDF.class);
        return imdf;
    }
}