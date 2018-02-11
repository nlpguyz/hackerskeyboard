package org.langwiki.alphatalk.script;

//import com.naef.jnlua.script.LuaScriptEngineFactory;
import org.langwiki.alphatalk.script.javascript.RhinoScriptEngineFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.io.IOException;
import java.util.*;

/**
 * The script manager class handles script engines, script attachment/
 * detachment with scriptable objects, and other operation related to
 * scripting.
 */
public class ScriptManager {
    private static final String TAG = ScriptManager.class.getSimpleName();
    public static final String LANG_LUA = "lua";
    public static final String LANG_JAVASCRIPT = "js";
    public static final String VAR_NAME_SYS = "$sys";

    protected Map<String, ScriptEngine> mEngines;

    protected Map<String, Object> mGlobalVariables;

    protected List<String> mGlobalVariablesToRemove;

    protected Map<IScriptable, ScriptFile> mScriptMap;

    // For script bundles. All special targets start with @.
    public static final String TARGET_PREFIX = "@";
    //public static final String TARGET_PIGSCRIPT = "@PIGScript";

    private static ScriptManager sInstance;

    interface TargetResolver {
        IScriptable getTarget(String name);
    }

    static Map<String, TargetResolver> sBuiltinTargetMap;

    // Provide getters for non-scene-object targets.
    static {
        sBuiltinTargetMap = new TreeMap<String, TargetResolver>();

        /*
        // Target resolver for "@PIGScript"
        sBuiltinTargetMap.put(TARGET_PIGSCRIPT, new TargetResolver() {
            @Override
            public IScriptable getTarget(String name) {
                return PIGContext.getActivity().getScript();
            }
        });*/
    }

    /**
     * Constructor.
     */
    private ScriptManager() {
        mGlobalVariables = new TreeMap<>();
        mGlobalVariablesToRemove = new ArrayList<>();
        mScriptMap = Collections.synchronizedMap(new HashMap<IScriptable, ScriptFile>());

        /* android only
        Thread.currentThread().setContextClassLoader(
                PIGContext.getActivity().getClassLoader()); */

        initializeGlobalVariables();
        initializeEngines();
    }

    public static ScriptManager getInstance() {
        if (sInstance != null)
            return sInstance;

        synchronized (ScriptManager.class) {
            sInstance = new ScriptManager();
            return sInstance;
        }
    }

    private void initializeGlobalVariables() {
        mGlobalVariables.put(VAR_NAME_SYS, null);
    }

    private void initializeEngines() {
        mEngines = new TreeMap<String, ScriptEngine>();

        // Add languages
        //mEngines.put(LANG_LUA, new LuaScriptEngineFactory().getScriptEngine());
        mEngines.put(LANG_JAVASCRIPT, new RhinoScriptEngineFactory().getScriptEngine());

        // Add variables to engines
        refreshGlobalBindings();
    }

    public void setGlobalVariable(String varName, Object value) {
        mGlobalVariables.put(varName, value);
        refreshGlobalBindings();
    }

    private void refreshGlobalBindings() {
        for (ScriptEngine se : mEngines.values()) {
            addGlobalBindings(se);
            removeGlobalBindings(se);
        }
    }

    protected void addGlobalBindings(ScriptEngine engine) {
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (bindings == null) {
            bindings = engine.createBindings();
            engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        }

        synchronized (mGlobalVariables) {
            for (Map.Entry<String, Object> ent : mGlobalVariables.entrySet()) {
                bindings.put(ent.getKey(), ent.getValue());
            }
        }
    }

    protected void removeGlobalBindings(ScriptEngine engine) {
        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        if (bindings == null) {
            return;
        }

        synchronized (mGlobalVariablesToRemove) {
            for (String name : mGlobalVariablesToRemove) {
                bindings.remove(name);
            }
            mGlobalVariablesToRemove.clear();
        }
    }

    /**
     * Returns an engine based on language.
     *
     * @param language The name of the language. Please use constants
     * defined in {@code ScriptManager}, such as LANG_LUA and LANG_JAVASCRIPT.
     *
     * @return The engine object. {@code null} if the specified engine is
     * not found.
     */
    public ScriptEngine getEngine(String language) {
        return mEngines.get(language);
    }

    /**
     * Add a variable to the scripting context.
     * 
     * @param varName The variable name.
     * @param value The variable value.
     */
    public void addVariable(String varName, Object value) {
        addVariable(varName, value, true);
    }

    public void addVariable(String varName, Object value, boolean effective) {
        synchronized (mGlobalVariables) {
            mGlobalVariables.put(varName, value);
        }
        if (effective) {
            refreshGlobalBindings();
        }
    }

    public void removeVariable(String varName) {
        removeVariable(varName, true);
    }

    public void removeVariable(String varName, boolean effective) {
        synchronized (mGlobalVariablesToRemove) {
            mGlobalVariablesToRemove.add(varName);
        }

        if (effective) {
            refreshGlobalBindings();
        }
    }

    /**
     * Attach a script file to a scriptable target.
     *
     * @param target The scriptable target.
     * @param scriptFile The script file object.
     */
    public void attachScriptFile(IScriptable target, ScriptFile scriptFile) {
        mScriptMap.put(target, scriptFile);
    }

    /**
     * Detach any script file from a scriptable target.
     *
     * @param target The scriptable target.
     */
    public void detachScriptFile(IScriptable target) {
        mScriptMap.remove(target);
    }

    /**
     * Gets a script file from a scriptable target.
     * @param target The scriptable target.
     * @return The script file or {@code null}.
     */
    public ScriptFile getScriptFile(IScriptable target) {
        return mScriptMap.get(target);
    }

    /**
     * Loads a script file using {@PIGAndroidResource}.
     * @param resource The resource object.
     * @param language The language string.
     * @return A script file object or {@code null} if not found.
     * @throws IOException
     */
    /*
    public ScriptFile loadScript(PIGAndroidResource resource, String language) throws IOException, ScriptError {
        if (getEngine(language) == null) {
            throw new ScriptError(String.format("The language is unknown: %s", language));
        }

        ScriptFile script = null;
        if (language.equals(LANG_LUA)) {
            script = new LuaScriptFile(mPIGContext, resource.getStream());
        } else if (language.equals(LANG_JAVASCRIPT)) {
            script = new JavascriptScriptFile(mPIGContext, resource.getStream());
        }

        return script;
    }
**/
    /**
     * Load a script bundle file. It defines bindings between scripts and PIGf objects
     * (e.g., scene objects and the {@link PIGScript} object).
     *
     * If {@linkplain PIGScriptEntry script entry} contains a {@code volume} attribute, the
     * script is loaded from the specified volume. Otherwise, it is loaded from the volume
     * specified by the {@code volume} parameter.
     *
     * @param filePath
     *        The path and filename of the script bundle.
     * @param volume
     *        The {@link ResourceVolume} from which to load the bundle file and scripts.
     * @return
     *         The loaded {@linkplain ScriptBundle script bundle}.
     *
     * @throws IOException
     */
    public ScriptBundle loadScriptBundle(String filePath, ResourceVolume volume) throws IOException {
        //ScriptBundle bundle = ScriptBundle.loadFromFile(mPIGContext, filePath, volume);
        //return bundle;
        return null;
    }

    /**
     * Binds a script bundle to a {@link PIGScene} object.
     *
     * @param scriptBundle
     *     The script bundle.
     * @param PIGScript
     *     The {@link PIGScript} to bind to.
     * @param bindToMainScene
     *     If {@code true}, also bind it to the main scene on the event {@link PIGScript#onAfterInit}.
     * @throws ScriptError
     * @throws IOException
     */
    /*
    public void bindScriptBundle(final ScriptBundle scriptBundle, final PIGScript PIGScript, boolean bindToMainScene)
            throws IOException, ScriptError {
        bindHelper(scriptBundle, null, BIND_MASK_PIGSCRIPT);

        if (bindToMainScene) {
            final IScriptEvents bindToSceneListener = new IScriptEvents() {
                PIGScene mainScene = null;

                @Override
                public void onInit(PIGContext PIGContext) throws Throwable {
                    mainScene = PIGContext.getNextMainScene();
                }

                @Override
                public void onAfterInit() {
                    try {
                        bindScriptBundleToScene(scriptBundle, mainScene);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ScriptError e) {
                        e.printStackTrace();
                    } finally {
                        // Remove the listener itself
                        PIGScript.getEventReceiver().removeListener(this);
                    }
                }

                @Override
                public void onStep() {
                }
            };

            // Add listener to bind to main scene when event "onAfterInit" is received
            PIGScript.getEventReceiver().addListener(bindToSceneListener);
        }
    }
*/

    /**
     * Binds a script bundle to a scene.
     * @param scriptBundle
     *         The {@code ScriptBundle} object containing script binding information.
     * @param PIGScene
     *         The scene to bind to.
     * @throws ScriptError
     * @throws IOException
     */
    /*
    public void bindScriptBundleToScene(ScriptBundle scriptBundle, PIGScene scene) throws IOException, ScriptError {
        for (PIGSceneObject sceneObject : scene.getSceneObjects()) {
            bindBundleToSceneObject(scriptBundle, sceneObject);
        }
    }
    */

    /**
     * Binds a script bundle to scene graph rooted at a scene object.
     * @param scriptBundle
     *         The {@code ScriptBundle} object containing script binding information.
     * @param rootSceneObject
     *         The root of the scene object tree to which the scripts are bound.
     * @throws IOException
     */
    /*
    public void bindBundleToSceneObject(ScriptBundle scriptBundle, PIGSceneObject rootSceneObject)
            throws IOException, ScriptError
    {
        bindHelper(scriptBundle, rootSceneObject, BIND_MASK_SCENE_OBJECTS);
    }

    protected int BIND_MASK_SCENE_OBJECTS = 0x0001;
    protected int BIND_MASK_PIGSCRIPT     = 0x0002;

    // Helper function to bind script bundler to various targets
    protected void bindHelper(ScriptBundle scriptBundle, PIGSceneObject rootSceneObject, int bindMask)
            throws IOException, ScriptError
    {
        for (ScriptBindingEntry entry : scriptBundle.file.binding) {
            PIGAndroidResource rc;
            if (entry.volumeType == null || entry.volumeType.isEmpty()) {
                rc = scriptBundle.volume.openResource(entry.script);
            } else {
                ResourceVolume.VolumeType volumeType = ResourceVolume.VolumeType.fromString(entry.volumeType);
                if (volumeType == null) {
                    throw new ScriptError(String.format("Volume type %s is not recognized, script=%s",
                            entry.volumeType, entry.script));
                }
                rc = new ResourceVolume(mPIGContext, volumeType).openResource(entry.script);
            }

            ScriptFile scriptFile = loadScript(rc, entry.language);

            String targetName = entry.target;
            if (targetName.startsWith(TARGET_PREFIX)) {
                TargetResolver resolver = sBuiltinTargetMap.get(targetName);
                IScriptable target = resolver.getTarget(mPIGContext, targetName);

                // Apply mask
                boolean toBind = false;
                if ((bindMask & BIND_MASK_PIGSCRIPT) != 0 && targetName.equalsIgnoreCase(TARGET_PIGSCRIPT)) {
                    toBind = true;
                }

                if (toBind) {
                    attachScriptFile(target, scriptFile);
                }
            } else {
                if ((bindMask & BIND_MASK_SCENE_OBJECTS) != 0) {
                    if (targetName.equals(rootSceneObject.getName())) {
                        attachScriptFile(rootSceneObject, scriptFile);
                    }

                    // Search in children
                    PIGSceneObject[] sceneObjects = rootSceneObject.getSceneObjectsByName(targetName);
                    if (sceneObjects != null) {
                        for (PIGSceneObject sceneObject : sceneObjects) {
                            attachScriptFile(sceneObject, scriptFile);
                        }
                    }
                }
            }
        }
    }
    */
}
