package org.langwiki.brime;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
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
            new Thread() {
                @Override
                public void run() {
                    if (refresh) {
                        sm.clearCache();
                    }
                    sm.getInstallList();
                }
            }.start();
        }

        @Override
        public void onSchemaList(@Nullable List<IMDF> list) {
            if (list == null) {
                return;
            }

            // Add schema checkboxes
            for (IMDF imdf : list) {
                imdf.installed = false;
            }

            getActivity().runOnUiThread(()->{
                listView.setAdapter(new ImdfAdapter(getContext(), R.layout.layout_schema_row, list));
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    if (!ExternalStorage.isWritable()) {
                        Toast.makeText(getContext(),
                                SDCARD_IS_NOT_WRITABLE,
                                Toast.LENGTH_LONG);
                        return;
                    }

                    final String imeId = ""; //view.findViewById()
                    new Thread() {
                        @Override
                        public void run() {
                            SchemaManager.getInstance().installSchema(
                                    getContext(), imeId, true);
                        }
                    }.start();
                });
            });

        }

        public void refresh(View view) {
            Log.d(TAG, "refresh");
            //schemaParent.removeAll();
            startDownload(true);
        }
    }
}