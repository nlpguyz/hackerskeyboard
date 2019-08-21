package org.langwiki.brime.schema;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.langwiki.brime.R;

import java.util.List;

public class ImdfAdapter extends ArrayAdapter<IMDF> {
        private List<IMDF> items;

        public ImdfAdapter(Context context, int textViewResourceId, List<IMDF> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                              tt.setText("Name: " + name);
                        }
                }
                return v;
        }
}