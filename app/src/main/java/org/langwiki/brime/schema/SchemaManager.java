package org.langwiki.brime.schema;

import org.langwiki.brime.R;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Handler;
import android.widget.Toast;

import com.google.gson.Gson;

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
    private static final String TAG = "BRime";

    private static SchemaManager sInstance;
    private Context context;
    private Resources resources;

    public static final String USER_DIR = "/sdcard/brime";
    public static final String DEFAULT_IMDF = "brime_basic.json";

    public static final String IMDF_SERVER_URL
            = "https://raw.githubusercontent.com/nlpguyz/hackerskeyboard/gradle/remote_data/downloadable.json";

    protected Object mLock = new Object();
    protected boolean mListReady;
    protected List<IMDF> mList;

    protected Handler mHandler;
    protected Toast mToast;

    public void clearCache() {
        mListReady = false;
        mList = null;
    }

    public interface SchemaManagerListener {
        void onSchemaList(List<IMDF> list);
    }

    protected List<SchemaManagerListener> listeners;

    public static SchemaManager getInstance(Context context) {
        if (sInstance != null)
            return sInstance;

        synchronized (SchemaManager.class) {
            sInstance = new SchemaManager(context);
            return sInstance;
        }
    }

    private SchemaManager(Context context) {
        this.context = context;
        resources = context.getResources();
        listeners = new ArrayList<>();
        mHandler = new Handler();
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

    public boolean deployImdf(IMDF imdf, FileOpener opener) {
        String versionFileName = USER_DIR + File.separator + imdf.id + ".ver";
        File versionFile = new File(versionFileName);
        String version = FileHelper.loadFile(versionFile, null);
        if (version != null && version.trim().compareTo(imdf.version) >= 0) {
            return true;
        }

        boolean fail = false;
        for (String fn : imdf.files) {
            try {
                FileHelper.copyTo(opener.open(fn), USER_DIR + File.separator + fn);
            } catch (IOException e) {
                e.printStackTrace();
                fail = true;
            }
        }

        FileHelper.writeFile(versionFile, imdf.version);

        return !fail;
    }

    private void showToast(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mToast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
                mToast.show();
            }
        });
    }

    public boolean installOnlineImdf(final IMDF imdf, boolean showToast) {
        final String homeUrl = imdf.homeUrl;
        if (homeUrl == null || homeUrl.isEmpty()) {
            return false;
        }

        Resources res = context.getResources();

        if (showToast) {
            showToast(String.format(res.getString(R.string.text_installing_rime_schema),
                    getLocaleString(imdf.name)));
        }

        FileOpener opener = new FileOpener() {
            @Override
            public InputStream open(String path) throws IOException {
                InputStream input = new URL(homeUrl + path).openStream();
                return input;
            }
        };

        boolean successful = deployImdf(imdf, opener);

        if (showToast) {
            String msg = successful
                    ? String.format(res.getString(R.string.rime_install_schema_successful),
                        getLocaleString(imdf.name))
                    : String.format(res.getString(R.string.rime_install_schema_failed),
                        getLocaleString(imdf.name));
            showToast(msg);
        }

        return successful;
    }

    public void installSchema(String id, boolean showToast) {
        if (mList == null) {
            return;
        }

        for (IMDF im : mList) {
            if (id.equals(im.id)) {
                installOnlineImdf(im, showToast);
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

    public String getLocaleString(Map<String,String> map) {
        Locale current = resources.getConfiguration().locale;
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
}