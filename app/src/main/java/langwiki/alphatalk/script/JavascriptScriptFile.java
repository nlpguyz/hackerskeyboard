package org.langwiki.alphatalk.script;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a Javascript file. The script text can be loaded in one
 * of the following ways.
 * <ul>
 * <li>
 *   Constructed locally and then set the text using {@link #setScriptText(String)}.
 * </li>
 * <li>
 *   Constructed locally and then load the text using {@link #load(InputStream)}.
 * </li>
 * </ul>
 *
 * Once a script text is set or loaded, you can invoke functions in the
 * script using {@link ScriptFile#invokeFunction(String, Object[]),
 * or attach it to a scriptable object using {@link ScriptManager#attachScriptFile(IScriptable, ScriptFile)}
 * to handle events delivered to it.
 */
public class JavascriptScriptFile extends ScriptFile {
    /**
     * Loads a Javascript file from {@code inputStream}.
     *
     * @param inputStream
     *     The input stream from which the script is loaded.
     * @throws IOException
     */
    public JavascriptScriptFile(InputStream inputStream) throws IOException {
        super(ScriptManager.LANG_JAVASCRIPT);
        load(inputStream);
    }

    protected String getInvokeStatement(String eventName, Object[] params) {
        StringBuilder sb = new StringBuilder();

        // function name
        sb.append(eventName);
        sb.append("(");

        // params
        for (int i = 0; i < params.length; ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(getDefaultParamName(i));
        }

        sb.append(");");
        return sb.toString();
    }
}
