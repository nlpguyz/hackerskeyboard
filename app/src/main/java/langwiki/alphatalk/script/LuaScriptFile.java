package org.langwiki.alphatalk.script;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a Lua script file.
 *
 * Once a script text is set or loaded, you can invoke functions in the
 * script using {@link ScriptFile#invokeFunction(String, Object[]),
 * or attach it to a scriptable object using {@link ScriptManager#attachScriptFile(IScriptable, ScriptFile)}
 * to handle events delivered to it.
 */
public class LuaScriptFile extends ScriptFile {
    /**
     * Loads a Lua script from {@code inputStream}.
     *
     * @param inputStream
     *     The input stream from which the script is loaded.
     * @throws IOException
     */
    public LuaScriptFile(InputStream inputStream) throws IOException {
        super(ScriptManager.LANG_LUA);
        load(inputStream);
    }

    protected String getInvokeStatement(String eventName, Object[] params) {
        StringBuilder sb = new StringBuilder();
        sb.append("return ");

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

        sb.append(")");
        return sb.toString();
    }
}
