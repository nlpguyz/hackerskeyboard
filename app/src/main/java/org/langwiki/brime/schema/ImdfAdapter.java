package org.langwiki.brime.schema;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
            v.setTag(o);

            SchemaManager sm = SchemaManager.getInstance();
            String name = sm.getLocaleString(getContext(), o.name);
            if (name == null) {
                name = o.id;
            }

            TextView tt = v.findViewById(R.id.tvImeName);
            if (tt != null) {
                tt.setText(name);
            }

            Button installButton = v.findViewById(R.id.buttonInstall);
            if (o.installed) {
                installButton.setText(getContext().getResources().getText(R.string.uninstall));
                installButton.setOnClickListener(view -> {
                    installCallback.uninstall(((IMDF)view.getTag()).id);
                });
            } else {
                installButton.setText(getContext().getResources().getText(R.string.install));
                installButton.setOnClickListener(view -> {
                    installCallback.install(((IMDF)view.getTag()).id);
                });
            }
        }
        return v;
    }
}