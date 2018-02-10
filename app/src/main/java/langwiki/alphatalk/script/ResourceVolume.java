package org.langwiki.alphatalk.script;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Models a file system which supports stream I/O.
 */
public class ResourceVolume {
    private static final String TAG = ResourceVolume.class.getSimpleName();

    public enum VolumeType {
        ANDROID_ASSETS ("assets", "/"),
        ANDROID_SDCARD ("sdcard", "/"),
        LINUX_FILESYSTEM ("linux", "/"),
        NETWORK ("url", "/");

        private String name;
        private String separator;

        VolumeType(String name, String separator) {
            this.name = name;
            this.separator = separator;
        }

        public String getName() {
            return name;
        }

        public String getSeparator() {
            return separator;
        }

        // Gets a volume type from a string. For example, when loading
        // a script from a bundle file, the volume type attribute needs
        // to be converted to a VolumeType.
        public static VolumeType fromString(String name) {
            for (VolumeType type : VolumeType.values()) {
                if (type.getName().equalsIgnoreCase(name)) // case insensitive
                    return type;
            }

            return null;
        }
    }

    protected VolumeType volumeType;
    protected String defaultPath;
    protected boolean enableUrlLocalCache = false;

    public ResourceVolume(VolumeType volume) {
        this(volume, null);
    }

    public ResourceVolume(VolumeType volumeType, String defaultPath) {
        this.volumeType = volumeType;
        this.defaultPath = defaultPath;
    }

    /* package */ ResourceVolume(VolumeType volumeType, String defaultPath, boolean cacheEnabled) {
        this(volumeType, defaultPath);
        this.enableUrlLocalCache = cacheEnabled;
    }

    /*
     * Opens a file from the volume. The filePath is relative to the
     * defaultPath.
     *
     * @param filePath
     *            File path of the resource to open.
     *
     * @throws IOException
     */
    /*
    public PIGAndroidResource openResource(String filePath) throws IOException {
        // Error tolerance: Remove initial '/' introduced by file::///filename
        // In this case, the path is interpreted as relative to defaultPath, which
        // is the root of the filesystem.
        if (filePath.startsWith(File.separator)) {
            filePath = filePath.substring(File.separator.length());
        }

        filePath = adaptFilePath(filePath);

        String path;
        switch (volumeType) {
        case ANDROID_ASSETS:
            // Resolve '..' and '.'
            path = getFullPath(defaultPath, filePath);
            path = new File(path).getCanonicalPath();
            if (path.startsWith(File.separator)) {
                path = path.substring(1);
            }
            return new PIGAndroidResource(PIGContext, path);

        case LINUX_FILESYSTEM:
            return new PIGAndroidResource(getFullPath(defaultPath, filePath));

        case ANDROID_SDCARD:
            String linuxPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            return new PIGAndroidResource(getFullPath(linuxPath, defaultPath, filePath));

        case NETWORK:
            return new PIGAndroidResource(PIGContext,
                    getFullURL(defaultPath, filePath), enableUrlLocalCache);

        default:
            throw new IOException(String.format("Unrecognized volumeType %s", volumeType));
        }
    }
    */

    /**
     * Adapt a file path to the current file system.
     * @param filePath The input file path string.
     * @return File path compatible with the file system of this {@link ResourceVolume}.
     */
    protected String adaptFilePath(String filePath) {
        // Convert windows file path to target FS
        String targetPath = filePath.replaceAll("\\\\", volumeType.getSeparator());

        // FS-specific
        switch (volumeType) {
        case NETWORK:
            try {
                targetPath = URLEncoder.encode(targetPath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            break;
        default:
            break;
        }

        return targetPath;
    }

    private URL getFullURL(String defaultPath, String filePath) throws MalformedURLException {
        return new URL(defaultPath + "/" + filePath);
    }

    /**
     * Changes the default path.
     *
     * @param defaultPath The path to change to.
     */
    public void changeDefaultPath(String defaultPath) {
        defaultPath = new String(defaultPath);
    }

    protected String getFullPath(String... path) {
        StringBuilder fullPath = new StringBuilder();

        boolean first = true;
        for (String fileName : path) {
            if (fileName == null || fileName.isEmpty())
                continue;

            if (!first) {
                fullPath.append(File.separator);
            } else {
                first = false;
            }

            fullPath.append(fileName);
        }

        return fullPath.toString();
    }
}
