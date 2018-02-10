package org.langwiki.alphatalk.script;

import java.io.IOException;
import java.util.Arrays;

/**
 * Represents a script bundle loaded from a JSON file, and its storage
 * volume.
 */
public class ScriptBundle {
    /**
     * The content of the bundle file is loaded from a JSON file.
     */
    public static class ScriptBundleFile {
        public String name;
        public ScriptBindingEntry[] binding;

        @Override
        public String toString() {
            return "ScriptBundleFile [name=" + name + ", binding="
                    + Arrays.toString(binding) + "]";
        }
    }

    /**
     * The contents of the script bundle from a JSON file.
     */
    protected ScriptBundleFile file;

    /**
     * The volume of the script bundle. The script bundle
     * is loaded from this volume, and it also serves as the default
     * volume for scripts referenced in the bundle.
     */
    protected ResourceVolume volume;

    /**
     * Returns the contents of the bundle.
     * @return The {@link ScriptBundleFile} object.
     */
    public ScriptBundleFile getContent() {
        return file;
    }

    /**
     * Loads a {@link ScriptBundle} from a file.
     * @param scriptManager
     *         The script manager.
     * @param filePath
     *         The file name of the script bundle in JSON format.
     * @param volume
     *         The {@link ResourceVolume} from which to load script bundle.
     * @return
     *         The {@link ScriptBundle} object with contents from the JSON file.
     *
     * @throws IOException
     */
    /*
    public static ScriptBundle loadFromFile(String filePath,
            ResourceVolume volume) throws IOException {
        PIGAndroidResource fileRes = volume.openResource(filePath);
        String fileText = TextFile.readTextFile(fileRes.getStream());

        ScriptBundle bundle = new ScriptBundle();
        Gson gson = new Gson();
        try {
            bundle.PIGContext = PIGContext;
            bundle.file = gson.fromJson(fileText, ScriptBundleFile.class);
            bundle.volume = volume;
            return bundle;
        } catch (Exception e) {
            throw new IOException("Cannot load the script bundle", e);
        }
    }
    */

    @Override
    public String toString() {
        return "ScriptBundle [file=" + file + ", volume=" + volume + "]";
    }
}
