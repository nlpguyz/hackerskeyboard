package org.langwiki.brime;

public class SettingManager {
    private static SettingManager sInstance;

    public static SettingManager getInstance() {
        if (sInstance != null)
            return sInstance;

        synchronized (SettingManager.class) {
            sInstance = new SettingManager();
            return sInstance;
        }
    }
}
