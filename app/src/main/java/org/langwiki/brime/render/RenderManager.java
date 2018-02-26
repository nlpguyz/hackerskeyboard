package org.langwiki.brime.render;

import android.content.Context;

public class RenderManager {
    private static final String TAG = "BRime";
    private static RenderManager sInstance;
    private Context context;

    public static RenderManager getInstance(Context context) {
        if (sInstance != null)
            return sInstance;

        synchronized (RenderManager.class) {
            sInstance = new RenderManager(context);
            return sInstance;
        }
    }

    public RenderManager(Context context) {
        this.context = context;
    }
}