package org.langwiki.brime.schema;

import org.langwiki.brime.IMEConfig;
import org.langwiki.brime.LatinIME;
import org.langwiki.brime.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.langwiki.brime.Rime;
import org.langwiki.brime.utils.FileHelper;
import org.langwiki.brime.utils.NetHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SchemaManager {
    private static final String TAG = IMEConfig.TAG;

    private static SchemaManager sInstance;

    // The directory name in external storage
    private static final String USER_DIR = "brime";
    public static final String DEFAULT_IMDF = "brime_basic.json";

    public static final String SHARED_PREF_NAME = "imdf_install";

    public static final String IMDF_SERVER_URL
            = "https://raw.githubusercontent.com/nlpguyz/hackerskeyboard/gradle/remote_data/downloadable.json";

    protected Object mLock = new Object();
    protected boolean mListReady;
    protected List<IMDF> mList;

    protected Handler mHandler;
    protected Toast mToast;
    private boolean mWritePermission = true;

    public void clearCache() {
        mListReady = false;
        mList = null;
    }

    /**
     * Activates a schema.
     *
     * The schema is activated in the engine and the font is selected.
     *
     * @param schemaId The ID of the schema.
     */
    public void selectSchema(final String schemaId) {
        final Rime rime = Rime.getInstance();
        new Thread() {
            public void run() {
                rime.select_schemas(new String[] {schemaId});
                rime.restartEngine();
            }
        }.start();
    }

    public void redeploy(final Context context, final boolean initial, boolean background) {
        if (!mWritePermission) {
            Log.e(IMEConfig.TAG, "Cannot deploy. No write permission!");
            return;
        }

        final Rime rime = Rime.getInstance();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (initial) {
                    Log.i(TAG, "Starting to copy schema files");
                    initializeDataDir(context);
                    rime.initSchema();
                }
                rime.incrementBusy();
                rime.deploy();
                rime.syncUserData();
                rime.decrementBusy();
            }
        };

        if (background) {
            new Thread() {
                public void run() {
                    runnable.run();
                }
            }.start();
        } else {
            runnable.run();
        }
    }

    public void setWritePermission(boolean writable) {
        mWritePermission = writable;
    }

    public interface SchemaManagerListener {
        void onSchemaList(List<IMDF> list);
    }

    protected List<SchemaManagerListener> listeners;

    public static SchemaManager getInstance() {
        if (sInstance != null)
            return sInstance;

        synchronized (SchemaManager.class) {
            sInstance = new SchemaManager();
            return sInstance;
        }
    }

    private SchemaManager() {
        listeners = new ArrayList<>();
        mHandler = new Handler();
    }

    public void initializeDataDir(Context context) {
        if (!mWritePermission) {
            Log.e(IMEConfig.TAG, "initializeDataDir: No write permission!");
            return;
        }

        // Make sure the path exists
        File brimePath = new File(getUserDir());
        brimePath.mkdir();

        final AssetManager assetMgr = context.getAssets();
        FileOpener opener = path->assetMgr.open(path);

        try {
            InputStream is = assetMgr.open(DEFAULT_IMDF);
            String imdfText = FileHelper.read(is);
            IMDF imdf = parseImdf(imdfText);
            deployImdf(imdf, opener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void selectAllSchemata() {
        List<Map<String, String>> schemaList = Rime.getInstance().get_available_schema_list();
        String[] ids = new String[schemaList.size()];

        int i = 0;
        for (Map<String, String> schema : schemaList) {
            ids[i++] = schema.get("schema_id");
        }

        Rime.getInstance().select_schemas(ids);
    }

    public boolean deployImdf(IMDF imdf, FileOpener opener) {
        String versionFileName = getUserDir() + File.separator + imdf.id + ".ver";
        File versionFile = new File(versionFileName);
        String version = FileHelper.loadFile(versionFile, null);
        if (version != null && version.trim().compareTo(imdf.version) >= 0) {
            return true;
        }

        boolean fail = false;

        try {
            Rime.getInstance().incrementBusy();
            String schemaFilepath = null;
            for (String fn : imdf.files) {
                try {
                    String dstPath = getUserDir() + File.separator + fn;
                    if (fn.endsWith("schema.yaml"))
                        schemaFilepath = dstPath;

                    FileHelper.copyTo(opener.open(fn), dstPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    fail = true;
                }
            }

            FileHelper.writeFile(versionFile, imdf.version);

            if (schemaFilepath != null) {
                fail |= !Rime.getInstance().deploy_schema(schemaFilepath);
                Rime.getInstance().initSchema();
            }
        } finally {
            Rime.getInstance().decrementBusy();
        }

        return !fail;
    }

    private void showToast(final Context context, final String msg) {
        mHandler.post(()->{
            mToast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
            mToast.show();
        });
    }

    public boolean installOnlineImdf(final Context context, final IMDF imdf, boolean showToast) {
        final String homeUrl = imdf.homeUrl;
        if (homeUrl == null || homeUrl.isEmpty()) {
            return false;
        }

        Resources res = context.getResources();

        if (showToast) {
            showToast(context, String.format(res.getString(R.string.text_installing_rime_schema),
                    getLocaleString(context, imdf.name)));
        }

        FileOpener opener = path -> {
            InputStream input = new URL(homeUrl + path).openStream();
            return input;
        };

        boolean successful = deployImdf(imdf, opener);

        // Save status in shared preferences
        SharedPreferences pref = context.getSharedPreferences(SchemaManager.SHARED_PREF_NAME, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(imdf.id, successful);
        editor.commit();

        if (showToast) {
            String msg = successful
                    ? String.format(res.getString(R.string.rime_install_schema_successful),
                        getLocaleString(context, imdf.name))
                    : String.format(res.getString(R.string.rime_install_schema_failed),
                        getLocaleString(context, imdf.name));
            showToast(context, msg);
        }

        return successful;
    }

    private void uninstallOnlineImdf(Context context, IMDF imdf, boolean showToast) {
        boolean successful = false;

        // Save status in shared preferences
        /*
        SharedPreferences pref = context.getSharedPreferences(SchemaManager.SHARED_PREF_NAME, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(imdf.id, !successful);
        editor.commit();*/
    }

    public void installSchema(Context context, String id, boolean showToast) {
        if (mList == null) {
            return;
        }

        for (IMDF im : mList) {
            if (id.equals(im.id)) {
                installOnlineImdf(context, im, showToast);
                return;
            }
        }
    }

    public void uninstallSchema(Context context, String id, boolean showToast) {
        if (mList == null) {
            return;
        }

        for (IMDF im : mList) {
            if (id.equals(im.id)) {
                uninstallOnlineImdf(context, im, showToast);
                return;
            }
        }
    }

    public IMDF parseImdf(String imdfString) {
        Gson gson = new Gson();
        IMDF imdf = gson.fromJson(imdfString, IMDF.class);
        return imdf;
    }

    public void getInstallList() {
        synchronized (mLock) {
            if (mListReady) {
                doListReady();
                return;
            }
        }

        startDownloadList();
    }

    private void startDownloadList() {
        try {
            String jsonText = NetHelper.getText(IMDF_SERVER_URL);
            Gson gson = new Gson();
            IMDF[] imdfArray = gson.fromJson(jsonText, IMDF[].class);
            mList = Arrays.asList(imdfArray);
            mListReady = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            doListReady();
        }
    }

    private void doListReady() {
        for (SchemaManagerListener l : listeners) {
            l.onSchemaList(mList);
        }
    }

    public void addListener(SchemaManagerListener l) {
        listeners.add(l);
    }

    public void removeListener(SchemaManagerListener l) {
        listeners.remove(l);
    }

    public String getLocaleString(Context context, Map<String,String> map) {
        Resources res = context.getResources();
        Locale current = res.getConfiguration().locale;
        String lang = current.toString();

        // remove _#HAN
        int pos = lang.indexOf("_#");
        if (pos >= 0) {
            lang = lang.substring(0, pos);
        }

        String value = map.get(lang);
        if (value == null) {
            value = map.get("default");
        }

        // Below are defaults
        if (value == null) {
            value = map.get("en");
        }
        if (value == null) {
            value = map.get("zh");
        }
        return value;
    }

    public static String getUserDir() {
        return ExternalStorage.getSdCardPath() + USER_DIR;
    }
}