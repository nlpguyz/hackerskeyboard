package org.langwiki.brime.schema;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.langwiki.brime.R;
import org.langwiki.brime.RimeInstallSchema;

import java.util.List;

public class ImdfAdapter extends ArrayAdapter<IMDF> {
    private List<IMDF> items;
    private final RimeInstallSchema.InstallCallback installCallback;

    public ImdfAdapter(Context context, int textViewResourceId, List<IMDF> items, RimeInstallSchema.InstallCallback installCallback) {
            super(context, textViewResourceId, items);
            this.items = items;
            this.installCallback = installCallback;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.layout_schema_row, null);
        }

        IMDF o = items.get(position);
        if (o != null) {
            SchemaManager sm = SchemaManager.getInstance();
            String name = sm.getLocaleString(getContext(), o.name);
            if (name == null) {
                name = o.id;
            }

            TextView tt = v.findViewById(R.id.tvImeName);
            if (tt != null) {
                tt.setText(name);
            }

            ProgressBar progressBar = v.findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);

            Button installButton = v.findViewById(R.id.buttonInstall);
            installButton.setTag(o);
            if (o.installed) {
                // Uninstall
                installButton.setText(getContext().getResources().getText(R.string.uninstall));
                installButton.setOnClickListener(view -> {
                    installButton.setText(R.string.uninstalling);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(true);
                    installCallback.uninstall(view, ((IMDF)view.getTag()).id, succ->{
                        if (succ) {
                            installButton.setText(R.string.install);
                        } else {
                            installButton.setText(R.string.uninstall_fail);
                        }
                        progressBar.setVisibility(View.INVISIBLE);
                    });
                });
            } else {
                // Install
                installButton.setText(getContext().getResources().getText(R.string.install));
                installButton.setOnClickListener(view -> {
                    installButton.setText(R.string.installing);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(true);
                    installCallback.install(view, ((IMDF)view.getTag()).id, succ->{
                        if (succ) {
                            installButton.setText(R.string.installed);
                            installButton.setOnClickListener(null);
                        } else {
                            installButton.setText(R.string.install_fail);
                        }
                        progressBar.setVisibility(View.INVISIBLE);
                    });
                });
            }
        }
        return v;
    }
}