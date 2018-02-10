package org.langwiki.alphatalk.script;

/**
 * A script entry in a script description file in JSON format.
 *
 * The format of the file is:
 *
 * <pre>
 * [
 *   {
 *      target: "pig",
 *      script: "script/pig.js",
 *      language: "js"
 *   },
 *   {
 *      target: "@controller",
 *      script: "http://mysite/script.lua",
 *      language: "lua",
 *      volumeType: "NETWORK"
 *   }
 * ]
 * </pre>
 */
public class ScriptBindingEntry {
    /**
     * The target to bind the script to. The string can be a name of an object,
     * or a special object beginning with the '@' character, such as some object
     * represented by "@controller".
     */
    public String target;

    /**
     * The path of the script.
     */
    public String script;

    /**
     * The language of the script.
     */
    public String language;

    /**
     * The type of the volume. It corresponds to the enum {@link ResourceVolume#VolumeType}.
     * If this field is omitted, it defaults to the volume from which the {@link ScriptBundle}
     * is loaded.
     */
    public String volumeType;
}