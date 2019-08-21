package org.langwiki.brime;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.langwiki.brime.schema.ExternalStorage;
import org.langwiki.brime.schema.IMDF;
import org.langwiki.brime.schema.ImdfAdapter;
import org.langwiki.brime.schema.SchemaManager;

import java.util.List;

import jline.internal.Nullable;

public class RimeInstallSchema extends AppCompatActivity {
    private static final String TAG = IMEConfig.TAG;
    public static final String SDCARD_IS_NOT_WRITABLE = "SDCard is not writable!";
    public static final String UNINSTALL_NOT_SUPPORTED = "Sorry, uninstall is not yet supported!";

    public interface DoneCallback {
        void done(boolean succ);
    }

    public interface InstallCallback {
        void install(View view, String imeId, DoneCallback doneCallback);
        void uninstall(View view, String imeId, DoneCallback doneCallback);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SchemaInstallFragment()).commit();
    }

    public static class SchemaInstallFragment extends Fragment
            implements SchemaManager.SchemaManagerListener {
        SchemaManager sm;
        ListView listView;

        private InstallCallback installCallback = new InstallCallback() {
            @Override
            public void install(View view, String imeId, DoneCallback doneCallback) {
                if (!ExternalStorage.isWritable()) {
                    Toast.makeText(getContext(),
                            SDCARD_IS_NOT_WRITABLE,
                            Toast.LENGTH_LONG);
                    return;
                }

                new Thread(()->{
                    SchemaManager.getInstance().installSchema(getContext(), imeId, true);
                    getActivity().runOnUiThread(()->{
                        doneCallback.done(true);
                    });
                }).start();
            }

            @Override
            public void uninstall(View view, String imeId, DoneCallback doneCallback) {
                if (!ExternalStorage.isWritable()) {
                    Toast.makeText(getContext(),
                            SDCARD_IS_NOT_WRITABLE,
                            Toast.LENGTH_LONG);
                    return;
                }

                boolean uninstalled = false;

                if (true) {
                    Toast.makeText(getContext(),
                            UNINSTALL_NOT_SUPPORTED,
                            Toast.LENGTH_LONG);
                } else {
                    new Thread(() -> {
                        SchemaManager.getInstance().uninstallSchema(getContext(), imeId, true);
                    }).start();
                }

                getActivity().runOnUiThread(() -> {
                    doneCallback.done(uninstalled);
                });
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            sm = SchemaManager.getInstance();
            sm.addListener(this);

            View view = inflater.inflate(R.layout.layout_install_schema, container, false);
            listView = view.findViewById(R.id.ime_list);

            startDownload(false);
            return view;
        }

        protected void startDownload(final boolean refresh) {
            new Thread(()->{
                if (refresh) {
                    sm.clearCache();
                }
                sm.getInstallList();
            }) {
            }.start();
        }

        @Override
        public void onSchemaList(@Nullable List<IMDF> list) {
            if (list == null) {
                return;
            }

            // Add schema list, get installed states from SharedPreferences
            SharedPreferences pref = getContext().getSharedPreferences(SchemaManager.SHARED_PREF_NAME, 0); // 0 - for private mode
            for (IMDF imdf : list) {
                imdf.installed = pref.getBoolean(imdf.id, false);
            }

            getActivity().runOnUiThread(()->{
                listView.setAdapter(new ImdfAdapter(getContext(), R.layout.layout_schema_row, list, installCallback));
            });
        }

        public void refresh(View view) {
            Log.d(TAG, "refresh");
            //schemaParent.removeAll();
            startDownload(true);
        }
    }
}