package org.langwiki.brime.schema;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.google.gson.Gson;

import org.langwiki.brime.utils.FileHelper;
import org.langwiki.brime.utils.NetHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchemaManager {
    private static final String TAG = "BRime";

    private static SchemaManager sInstance;
    private Context context;
    private Resources resources;

    public static final String USER_DIR = "/sdcard/brime";
    public static final String DEFAULT_IMDF = "brime_basic.json";

    public static final String IMDF_SERVER_URL
            = "https://raw.githubusercontent.com/nlpguyz/hackerskeyboard/gradle/remote_data/downloadable.json";

    protected Object mLock;
    protected boolean mListReady;
    protected List<IMDF> mList;

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
        String versionFileName = USER_DIR + File.separator + imdf.id + ".ver";
        File versionFile = new File(versionFileName);
        String version = FileHelper.loadFile(versionFile, null);
        if (version != null && version.trim().compareTo(imdf.version) >= 0) {
            return;
        }
        for (String fn : imdf.files) {
            try {
                FileHelper.copyTo(opener.open(fn), USER_DIR + File.separator + fn);
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
}