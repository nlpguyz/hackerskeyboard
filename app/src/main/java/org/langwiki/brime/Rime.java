    /*
     * Copyright (C) 2015-present, osfans
     * waxaca@163.com https://github.com/osfans
     *
     * This program is free software: you can redistribute it and/or modify
     * it under the terms of the GNU General Public License as published by
     * the Free Software Foundation, either version 3 of the License, or
     * (at your option) any later version.
     *
     * This program is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     * GNU General Public License for more details.
     *
     * You should have received a copy of the GNU General Public License
     * along with this program.  If not, see <http://www.gnu.org/licenses/>.
     */

    package org.langwiki.brime;

    import java.io.File;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.Map;
    import java.util.HashMap;
    import java.util.Iterator;
    import java.util.List;
    import java.util.logging.Logger;

    import org.mozilla.javascript.*;

    import org.langwiki.brime.schema.SchemaManager;
    import android.util.Log;

    /**
     * Rime與OpenCC的Java實現
     *
     * @see <a href="https://github.com/rime/librime">Rime</a> <a
     *     href="https://github.com/BYVoid/OpenCC">OpenCC</a>
     */
    public class Rime {
        private static final String TAG = "BRime";
        private static Rime sInstance;
        private static int sMaxCandidates = 400;

        public static Rime getInstance() {
            if (sInstance != null)
                return sInstance;
            synchronized (Rime.class) {
                sInstance = new Rime();
                return sInstance;
            }
        }

        public interface RimeListener {
            void onMessage(String message_type, String message_value);
            void onEngineStateChanged(boolean busy);
        }

        static class Config {
            static String getSharedDataDir() {
                return SchemaManager.USER_DIR;
            }

            static String getUserDataDir() {
                return SchemaManager.USER_DIR;
            }

            static void deployOpencc() {
                // TODO -- shouldn't be here
            }

            static String getResDataDir(String moduleName) {
                return moduleName; // TODO
            }
        }

        /** Rime編碼區 */
        public static class RimeComposition {
            int length;
            int cursor_pos;
            int sel_start;
            int sel_end;
            String preedit;
            byte[] bytes;

            public String getText() {
                if (length == 0) return "";
                bytes = preedit.getBytes();
                return preedit;
            }

            public int getStart() {
                if (length == 0) return 0;
                return new String(bytes, 0, sel_start).length();
            }

            public int getEnd() {
                if (length == 0) return 0;
                return new String(bytes, 0, sel_end).length();
            }
        }

        /** Rime候選項 */
        public static class RimeCandidate {
            String text;
            String comment;

            @Override
            public String toString() {
                return text + ", " + comment;
            }
        }

        /** Rime候選區，包含多個{@link RimeCandidate 候選項} */
        public static class RimeMenu {
            int page_size;
            int page_no;
            boolean is_last_page;
            int highlighted_candidate_index;
            int num_candidates;
            RimeCandidate[] candidates;
            String select_keys;
        }

        /** Rime上屏的字符串 */
        public static class RimeCommit {
            int data_size;
            // v0.9
            String text;
        }

        /** Rime環境，包括 {@link RimeComposition 編碼區} 、{@link RimeMenu 候選區} */
        public static class RimeContext {
            int data_size;
            // v0.9
            RimeComposition composition;
            RimeMenu menu;
            // v0.9.2
            String commit_text_preview;
            String[] select_labels;

            public int size() {
                if (menu == null) return 0;
                return menu.num_candidates;
            }

            public RimeCandidate[] getCandidates() {
                return size() == 0 ? null : menu.candidates;
            }
        }

        /** Rime狀態 */
        public static class RimeStatus {
            int data_size;
            // v0.9
            String schema_id;
            String schema_name;
            boolean is_disabled;
            boolean is_composing;
            boolean is_ascii_mode;
            boolean is_full_shape;
            boolean is_simplified;
            boolean is_traditional;
            boolean is_ascii_punct;
        }

        /** Rime方案 */
        public class RimeSchema {
            private String kRightArrow = "→ ";
            private String kRadioSelected = " ✓";

            Map<String, Object> schema = new HashMap<String, Object>();
            List<Map<String, Object>> switches = new ArrayList<Map<String, Object>>();

            public RimeSchema(String schema_id) {
                Object o;
                o = schema_get_value(schema_id, "schema");
                if (o == null || !(o instanceof Map)) return;
                schema = (Map<String, Object>) o;
                o = schema_get_value(schema_id, "switches");
                if (o == null || !(o instanceof List)) return;
                switches = (List<Map<String, Object>>) o;
                check(); //檢查不在選單中顯示的選項
            }

            public void check() {
                if (switches.isEmpty()) return;
                for (Iterator it = switches.iterator(); it.hasNext(); ) {
                    Map<String, Object> o = (Map<String, Object>) it.next();
                    if (!o.containsKey("states")) it.remove();
                }
            }

            public RimeCandidate[] getCandidates() {
                if (switches.isEmpty()) return null;
                RimeCandidate[] candidates = new RimeCandidate[switches.size()];
                int i = 0;
                for (Map<String, Object> o : switches) {
                    candidates[i] = new RimeCandidate();
                    List states = (List) o.get("states");
                    Integer value = (Integer) o.get("value");
                    if (value == null) value = 0;
                    candidates[i].text = states.get(value).toString();
                    candidates[i].comment =
                            o.containsKey("options") ? "" : kRightArrow + states.get(1 - value).toString();
                    i++;
                }
                return candidates;
            }

            public void getValue() {
                if (switches.isEmpty()) return; //無方案
                for (int j = 0; j < switches.size(); j++) {
                    Map<String, Object> o = switches.get(j);
                    if (o.containsKey("options")) {
                        List<String> options = (List<String>) o.get("options");
                        for (int i = 0; i < options.size(); i++) {
                            String s = options.get(i);
                            if (get_option(s)) {
                                o.put("value", i);
                                break;
                            }
                        }
                    } else {
                        o.put("value", get_option(o.get("name").toString()) ? 1 : 0);
                    }
                    switches.set(j, o);
                }
            }

            public void toggleOption(int i) {
                if (switches.isEmpty()) return;
                Map<String, Object> o = switches.get(i);
                Integer value = (Integer) o.get("value");
                if (value == null) value = 0;
                if (o.containsKey("options")) {
                    List<String> options = (List<String>) o.get("options");
                    setOption(options.get(value), false);
                    value = (value + 1) % options.size();
                    setOption(options.get(value), true);
                } else {
                    value = 1 - value;
                    setOption(o.get("name").toString(), value == 1);
                }
                o.put("value", value);
                switches.set(i, o);
            }
        }

        private Rime self;
        private Logger Log = Logger.getLogger(Rime.class.getSimpleName());

        private RimeCommit mCommit = new RimeCommit();
        private RimeContext mContext = new RimeContext();
        private RimeStatus mStatus = new RimeStatus();
        private RimeSchema mSchema;
        private List mSchemaList;
        private boolean mOnMessage;
        private FunctionProxy mOnMessageFunc;
        private RimeListener mRimeListener;

        private int mBusyCount;

        static {
            System.loadLibrary("opencc");
            System.loadLibrary("rime");
            System.loadLibrary("rime_jni");
        }

        public int META_SHIFT_ON = get_modifier_by_name("Shift");
        public int META_CTRL_ON = get_modifier_by_name("Control");
        public int META_ALT_ON = get_modifier_by_name("Alt");
        public int META_RELEASE_ON = get_modifier_by_name("Release");
        private boolean showSwitches = true;

        public void setShowSwitches(boolean show) {
            showSwitches = show;
        }

        public boolean hasMenu() {
            return isComposing() && mContext.menu != null && mContext.menu.num_candidates != 0;
        }

        public boolean hasLeft() {
            return hasMenu() && mContext.menu.page_no != 0;
        }

        public boolean hasRight() {
            return hasMenu() && !mContext.menu.is_last_page;
        }

        public boolean isPaging() {
            return hasLeft();
        }

        public boolean isComposing() {
            return mStatus.is_composing;
        }

        public boolean isAsciiMode() {
            return mStatus.is_ascii_mode;
        }

        public RimeComposition getComposition() {
            if (mContext == null) return null;
            return mContext.composition;
        }

        public String getCompositionText() {
            RimeComposition composition = getComposition();
            return (composition == null) ? "" : composition.preedit;
        }

        public String getComposingText() {
            if (mContext == null || mContext.commit_text_preview == null) return "";
            return mContext.commit_text_preview;
        }

        public void setComposition(CharSequence typedWord) {
            String current = getCommitText();
            if (current != null) {
                // Optimized for the case of one more key
                if (typedWord.length() == getCommitText().length() + 1 &&
                        typedWord.toString().startsWith(getCommitText())) {
                    onKey(new int[]{typedWord.charAt(typedWord.length() - 1), 0});
                    return;
                }
            }

            // General case: clear and simulate key sequence
            clearComposition();
            for (int i = 0; i < typedWord.length(); i++) {
                char ch = Character.toLowerCase(typedWord.charAt(i));
                onKey(new int[]{ch, 0});
            }
        }

        private Rime() {
            this(false);
        }

        private Rime(boolean full_check) {
            init(full_check);
            self = this;
        }

        public void initSchema() {
            mSchemaList = get_schema_list();
            String schema_id = getSchemaId();
            mSchema = new RimeSchema(schema_id);
            getStatus();
        }

        private boolean getStatus() {
            mSchema.getValue();
            return get_status(mStatus);
        }

        public void incrementBusy() {
            mBusyCount++;

            if (mRimeListener != null) {
                mRimeListener.onEngineStateChanged(mBusyCount != 0);
            }
        }

        public void decrementBusy() {
            mBusyCount--;

            if (mRimeListener != null) {
                mRimeListener.onEngineStateChanged(mBusyCount != 0);
            }
        }

        public boolean isBusy() {
            return mBusyCount != 0;
        }

        final int pageUp = 65365;
        final int pageDown = 65366;

        /**
         * Gets all candidates from engine.
         * @return a list of RimeCandidate.
         */
        public List<RimeCandidate> getAllCandidates() {
            List<RimeCandidate> allCands = new ArrayList<>();

            if (mContext == null || !hasMenu())
                return allCands;

            RimeCandidate[] cands;
            boolean done = false;
            while (!done && allCands.size() < sMaxCandidates) {
                cands = getCandidates();
                Log.severe("" + cands);
                if (cands != null) {
                    allCands.addAll(Arrays.asList(cands));
                    if (hasRight()) {
                        onKey(pageDown, 0);
                    } else {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }

            return allCands;
        }

        public boolean selectCandidateFromBeginning(int index) {
            // Rewind
            while (hasLeft()) {
                onKey(pageUp, 0);
            }

            RimeCandidate[] cands;
            boolean done = false;
            while (!done) {
                cands = getCandidates();
                if (index < cands.length) {
                    selectCandidate(index);
                    return true;
                }

                if (hasRight()) {
                    index -= cands.length;
                    onKey(pageDown, 0);
                } else {
                    done = true;
                }
            }

            return false;
        }

        private void init(boolean full_check) {
            mOnMessage = false;
            initialize(Config.getSharedDataDir(), Config.getUserDataDir());
            check(full_check);
            set_notification_handler();
            if (!find_session()) {
                if (create_session() == 0) {
                    Log.severe("Error creating rime session");
                    return;
                }
            }
            initSchema();
        }

        public void destroy() {
            destroy_session();
            finalize1();
            self = null;
        }

        // Called after selecting new schemata
        public void restartEngine() {
            destroy();
            get(true);
        }

        public String getCommitText() {
            return mCommit.text;
        }

        public boolean getCommit() {
            return get_commit(mCommit);
        }

        private boolean getContexts() {
            boolean b = get_context(mContext);
            getStatus();
            return b;
        }

        public boolean isVoidKeycode(int keycode) {
            int XK_VoidSymbol = 0xffffff;
            return keycode <= 0 || keycode == XK_VoidSymbol;
        }

        private boolean onKey(int keycode, int mask) {
            if (isVoidKeycode(keycode)) return false;
            boolean b = process_key(keycode, mask);
            Log.info("b=" + b + ",keycode=" + keycode + ",mask=" + mask);
            getContexts();
            return b;
        }

        public boolean onKey(int[] event) {
            if (event != null && event.length == 2) return onKey(event[0], event[1]);
            return false;
        }

        public boolean onText(CharSequence text) {
            if (text == null || text.length() == 0) return false;
            boolean b = simulate_key_sequence(text.toString().replace("{}", "{braceleft}{braceright}"));
            Log.info("b=" + b + ",input=" + text);
            getContexts();
            return b;
        }

        public RimeCandidate[] getCandidates() {
            // TODO: do not confuse users with schema switches
            //if (!isComposing() && showSwitches) return mSchema.getCandidates();
            return mContext.getCandidates();
        }

        public String[] getSelectLabels() {
            if (mContext != null && mContext.size() > 0) {
                if (mContext.select_labels != null) return mContext.select_labels;
                if (mContext.menu.select_keys != null) return mContext.menu.select_keys.split("\\B");
                int n = mContext.size();
                String[] labels = new String[n];
                for (int i = 0; i < n; i++) {
                    labels[i] = String.valueOf((i + 1) % 10);
                }
                return labels;
            }
            return null;
        }

        public int getCandHighlightIndex() {
            return isComposing() ? mContext.menu.highlighted_candidate_index : -1;
        }

        public boolean commitComposition() {
            boolean b = commit_composition();
            getContexts();
            return b;
        }

        public void clearComposition() {
            clear_composition();
            getContexts();
        }

        public boolean selectCandidate(int index) {
            boolean b = select_candidate_on_current_page(index);
            getContexts();
            return b;
        }

        public void setOption(String option, boolean value) {
            if (mOnMessage) return;
            set_option(option, value);
        }

        public boolean getOption(String option) {
            return get_option(option);
        }

        public void toggleOption(String option) {
            boolean b = getOption(option);
            setOption(option, !b);
        }

        public void toggleOption(int i) {
            mSchema.toggleOption(i);
        }

        public void setProperty(String prop, String value) {
            if (mOnMessage) return;
            set_property(prop, value);
        }

        public String getProperty(String prop) {
            return get_property(prop);
        }

        public String getSchemaId() {
            return get_current_schema();
        }

        private boolean isEmpty(String s) {
            return s.contentEquals(".default"); //無方案
        }

        public boolean isEmpty() {
            return isEmpty(getSchemaId());
        }

        public String[] getSchemaNames() {
            int n = mSchemaList.size();
            String[] names = new String[n];
            int i = 0;
            for (Object o : mSchemaList) {
                Map<String, String> m = (Map<String, String>) o;
                names[i++] = m.get("name");
            }
            return names;
        }

        public int getSchemaIndex() {
            String schema_id = getSchemaId();
            int i = 0;
            for (Object o : mSchemaList) {
                Map<String, String> m = (Map<String, String>) o;
                if (m.get("schema_id").contentEquals(schema_id)) return i;
                i++;
            }
            return 0;
        }

        public String getSchemaName() {
            return mStatus.schema_name;
        }

        public boolean selectSchema(String id) {
            if (id.equals(getSchemaId()))
                return false;

            return selectSchemaInternal(id);
        }

        private boolean selectSchemaInternal(String schema_id) {
            boolean b = select_schema(schema_id);
            getContexts();
            return b;
        }

        public boolean selectSchema(int id) {
            int n = mSchemaList.size();
            if (id < 0 || id >= n) return false;
            String schema_id = getSchemaId();
            Map<String, String> m = (Map<String, String>) mSchemaList.get(id);
            String target = m.get("schema_id");
            if (target.contentEquals(schema_id)) return false;
            return selectSchema(target);
        }

        public Rime get(boolean full_check) {
            if (self == null) {
                if (full_check) Config.deployOpencc();
                self = new Rime(full_check);
            }
            return self;
        }

        public Rime get() {
            return get(false);
        }

        public String RimeGetInput() {
            String s = get_input();
            return s == null ? "" : s;
        }

        public int RimeGetCaretPos() {
            return get_caret_pos();
        }

        public void RimeSetCaretPos(int caret_pos) {
            set_caret_pos(caret_pos);
            getContexts();
        }

        // Set a listener for Javascript
        public void setMessageListener(Object func) {
            FunctionProxy funcProxy = FunctionProxy.getFunctionProxy(func);
            if (funcProxy == null) {
                Log.info("Error: 1st arg of setMessageListener must be a function");
                return;
            }

            mOnMessageFunc = funcProxy;
        }

        public void setRimeListener(RimeListener l) {
            mRimeListener = l;
        }

        public void onMessage(String message_type, String message_value) {
            mOnMessage = true;
            String msg = String.format("message: [%s] %s", message_type, message_value);
            Log.info(msg);
            if (mOnMessageFunc != null) {
                mOnMessageFunc.invoke(msg);
            }

            if (mRimeListener != null) {
                mRimeListener.onMessage(message_type, message_value);
            }
        }

        public String openccConvert(String line, String name) {
            if (name != null && name.length() > 0) {
                File f = new File(Config.getResDataDir("opencc"), name);
                if (f.exists()) return opencc_convert(line, f.getAbsolutePath());
            }
            return line;
        }

        public void check(boolean full_check) {
            start_maintenance(full_check);
            if (is_maintenance_mode()) join_maintenance_thread();
        }

        public boolean syncUserData() {
            boolean b = sync_user_data();
            destroy();
            get();
            return b;
        }

        // See here for API usage
        // https://github.com/rime/librime/blob/master/tools/rime_api_console.cc

        // init
        public native void setup(String shared_data_dir, String user_data_dir);

        public native void set_notification_handler();

        // entry and exit
        public native void initialize(String shared_data_dir, String user_data_dir);

        public native void finalize1();

        public native boolean start_maintenance(boolean full_check);

        public native boolean is_maintenance_mode();

        public native void join_maintenance_thread();

        // deployment
        public native void deployer_initialize(String shared_data_dir, String user_data_dir);

        public native boolean prebuild();

        public native boolean deploy();

        public native boolean deploy_schema(String schema_file);

        public native boolean deploy_config_file(String file_name, String version_key);

        public native boolean sync_user_data();

        // session management
        public native int create_session();

        public native boolean find_session();

        public native boolean destroy_session();

        public native void cleanup_stale_sessions();

        public native void cleanup_all_sessions();

        // input
        public native boolean process_key(int keycode, int mask);

        public native boolean commit_composition();

        public native void clear_composition();

        // output
        public native boolean get_commit(RimeCommit commit);

        public native boolean get_context(RimeContext context);

        public native boolean get_status(RimeStatus status);

        // runtime options
        public native void set_option(String option, boolean value);

        public native boolean get_option(String option);

        public native void set_property(String prop, String value);

        public native String get_property(String prop);

        public native List get_schema_list();

        public native String get_current_schema();

        public native boolean select_schema(String schema_id);

        // configuration
        public native Boolean config_get_bool(String name, String key);

        public native boolean config_set_bool(String name, String key, boolean value);

        public native Integer config_get_int(String name, String key);

        public native boolean config_set_int(String name, String key, int value);

        public native Double config_get_double(String name, String key);

        public native boolean config_set_double(String name, String key, double value);

        public native String config_get_string(String name, String key);

        public native boolean config_set_string(String name, String key, String value);

        public native int config_list_size(String name, String key);

        public native List config_get_list(String name, String key);

        public native Map config_get_map(String name, String key);

        public native Object config_get_value(String name, String key);

        public native Object schema_get_value(String name, String key);

        // testing
        public native boolean simulate_key_sequence(String key_sequence);

        public native String get_input();

        public native int get_caret_pos();

        public native void set_caret_pos(int caret_pos);

        public native boolean select_candidate(int index);

        public native boolean select_candidate_on_current_page(int index);

        public native String get_version();

        public native String get_librime_version();

        // module
        public native boolean run_task(String task_name);

        public native String get_shared_data_dir();

        public native String get_user_data_dir();

        public native String get_sync_dir();

        public native String get_user_id();

        // key_table
        public native int get_modifier_by_name(String name);

        public native int get_keycode_by_name(String name);

        // customize setting
        public native boolean customize_bool(String name, String key, boolean value);

        public native boolean customize_int(String name, String key, int value);

        public native boolean customize_double(String name, String key, double value);

        public native boolean customize_string(String name, String key, String value);

        public native List<Map<String, String>> get_available_schema_list();

        public native List<Map<String, String>> get_selected_schema_list();

        public native boolean select_schemas(String[] schema_id_list);

        // opencc
        public native String get_opencc_version();

        public native String opencc_convert(String line, String name);

        public native void opencc_convert_dictionary(
                String inputFileName, String outputFileName, String formatFrom, String formatTo);
    }
