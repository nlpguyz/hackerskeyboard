package org.langwiki.alphatalk.script;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * This class represents a script file, which can be attached to an
 * object to handle events delivered to it. <p>
 *
 * It can be used in two ways:
 * <ul>
 *   <li> Construct the {@code ScriptFile} object and set the script
 *   text using {@link #setScriptText(String)}, or load it from a stream using
 *   {@link #load(InputStream)}. </li>
 * </ul>
 */
public abstract class ScriptFile {
    private static final String TAG = ScriptFile.class.getSimpleName();
    protected final String mLanguage;

    // Lock for engine access and mBadFunctions
    protected final Object mEngineLock = new Object();
    protected final ScriptEngine mLocalEngine;
    private Set<String> mBadFunctions;

    // Lock for mScriptText and dirty flag
    protected final Object mScriptTextLock = new Object();
    protected String mScriptText;
    protected boolean mScriptTextDirty;

    // Caching parameter names to reduce object creation
    private static final int sNumOfCachedParamNames = 10;
    private static String[] sCachedParamName;

    static {
        // Generate parameter names, arg0, arg1, ...
        sCachedParamName = new String[sNumOfCachedParamNames];
        for (int i = 0; i < sNumOfCachedParamNames; ++i) {
            sCachedParamName[i] = getDefaultParamNameRaw(i);
        }
    }

    // Cache for function invocation statements
    protected final Map<String, String> mInvokeStatementCache;

    /**
     * Constructor.
     * @param language
     *     The language of the script file. Please use the constants
     *     {@code LANG_*} defined in {@link ScriptManager}, such
     *     as {@code LANG_LUA}, {@code LANG_JAVASCRIPT}, and so on.
     */
    public ScriptFile(String language) {
        mLanguage = language;
        mInvokeStatementCache = new TreeMap<String, String>();

        // Get an engine because some impl. requires a new engine to
        // enforce context
        ScriptEngine engine = ScriptManager.getInstance().getEngine(mLanguage);
        mLocalEngine = engine.getFactory().getScriptEngine();

        // Add globals
        ScriptManager.getInstance().addGlobalBindings(mLocalEngine);
    }

    /**
     * Loads a script into a {@ScriptFile} object.
     *
     * @param inputStream
     *     The input stream from which to load the script.
     * @throws IOException
     */
    public void load(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder out = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(System.lineSeparator());
        }

        setScriptText(out.toString());
    }

    /**
     * Sets the script using a string.
     *
     * @param scriptText The script string.
     */
    public void setScriptText(String scriptText) {
        synchronized (mScriptTextLock) {
            mScriptText = scriptText;
            mScriptTextDirty = true;
        }
    }

    /**
     * Gets the script from the {@ScriptFile} object.
     *
     * @return The script string.
     */
    public String getScriptText() {
        return mScriptText;
    }

    /**
     * Invokes a function defined in the script.
     *
     * @param funcName
     *     The function name.
     * @param params
     *     The parameter array.
     * @return
     *     A boolean value representing whether the function is
     * executed correctly. If the function cannot be found, or
     * parameters don't match, {@code false} is returned.
     */
    public boolean invokeFunction(String funcName, Object[] params) {
        // Run script if it is dirty. This makes sure the script is run
        // on the same thread as the caller (suppose the caller is always
        // calling from the same thread).
        checkDirty();

        // Skip bad functions
        if (isBadFunction(funcName)) {
            return false;
        }

        String statement = getInvokeStatementCached(funcName, params);

        Bindings localBindings = null;
        synchronized (mEngineLock) {
            localBindings = mLocalEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            if (localBindings == null) {
                localBindings = mLocalEngine.createBindings();
                mLocalEngine.setBindings(localBindings, ScriptContext.ENGINE_SCOPE);
            }
        }

        fillBindings(localBindings, params);

        try {
            mLocalEngine.eval(statement);
        } catch (ScriptException e) {
            // The function is either undefined or throws, avoid invoking it later
            addBadFunction(funcName);
            return false;
        } finally {
            removeBindings(localBindings, params);
        }

        return true;
    }

    private void resetBadFunctions() {
        if (mBadFunctions == null) {
            return;
        }

        synchronized (mEngineLock) {
            mBadFunctions.clear();
        }
    }

    private boolean isBadFunction(String funcName) {
        if (mBadFunctions == null) {
            return false;
        }

        synchronized (mEngineLock) {
            return mBadFunctions.contains(funcName);
        }
    }

    private void addBadFunction(String funcName) {
        synchronized (mEngineLock) {
            // Lazy initialization
            if (mBadFunctions == null) {
                mBadFunctions = new HashSet<String>();
            }
            mBadFunctions.add(funcName);
        }
    }

    protected void checkDirty() {
        synchronized (mScriptTextLock) {
            if (mScriptTextDirty) {
                mScriptTextDirty = false;

                // Remove marked bad functions
                resetBadFunctions();

                try {
                    mLocalEngine.eval(mScriptText);
                } catch (ScriptException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected String getDefaultParamName(int i) {
        if (i < sNumOfCachedParamNames) {
            return sCachedParamName[i];
        }

        return getDefaultParamNameRaw(i);
    }

    private final static String getDefaultParamNameRaw(int i) {
        return "arg" + Integer.toString(i);
    }

    protected void fillBindings(Bindings localBindings, Object[] params) {
        for (int i = 0; i < params.length; ++i) {
            localBindings.put(getDefaultParamName(i), params[i]);
        }
    }

    protected void removeBindings(Bindings localBindings, Object[] params) {
        for (int i = 0; i < params.length; ++i) {
            localBindings.remove(getDefaultParamName(i));
        }
    }

    private final String getInvokeStatementCached(String eventName, Object[] params) {
        synchronized (mInvokeStatementCache) {
            String invokeStatement = mInvokeStatementCache.get(eventName);
            if (invokeStatement == null) {
                invokeStatement = getInvokeStatement(eventName, params);
                mInvokeStatementCache.put(eventName, invokeStatement);
            }

            return invokeStatement;
        }
    }

    protected abstract String getInvokeStatement(String eventName, Object[] params);
}
