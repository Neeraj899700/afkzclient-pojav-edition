package net.kdt.pojavlaunch.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import git.artdeell.mojo.R;

import java.util.List;

public class FileListAdapter extends BaseAdapter {

    public interface ToggleListener {
        void onToggle(int position, boolean isChecked);
    }

    private final List<?> mItems;
    private final LayoutInflater mInflater;
    private final ToggleListener mToggleListener;

    public FileListAdapter(LayoutInflater inflater, List<?> items, ToggleListener listener) {
        mInflater = inflater;
        mItems = items;
        mToggleListener = listener;
    }

    @Override
    public int getCount() { return mItems.size(); }

    @Override
    public Object getItem(int position) { return mItems.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.item_file_list, parent, false);
        }

        TextView iconView = convertView.findViewById(R.id.file_icon);
        TextView nameView = convertView.findViewById(R.id.file_name);
        TextView descView = convertView.findViewById(R.id.file_description);
        SwitchCompat toggle = convertView.findViewById(R.id.file_toggle);

        Object item = mItems.get(position);
        String name = "";
        String desc = "";
        String icon = "📦";
        boolean enabled = true;

        if(item instanceof ModEntry) {
            ModEntry mod = (ModEntry) item;
            name = mod.displayName != null ? mod.displayName : mod.fileName;
            desc = mod.description != null && !mod.description.isEmpty() ? mod.description :
                    (mod.author != null ? "by " + mod.author : "");
            if(!mod.enabled) desc = "DISABLED - " + desc;
            icon = "🔧";
            enabled = mod.enabled;
        } else if(item instanceof ResourcePackEntry) {
            ResourcePackEntry rp = (ResourcePackEntry) item;
            name = rp.displayName != null ? rp.displayName : rp.fileName;
            String fmt = rp.packFormat > 0 ? " (format " + rp.packFormat + ")" : "";
            desc = rp.description != null && !rp.description.isEmpty() ? rp.description + fmt : fmt;
            if(!rp.enabled) desc = "DISABLED - " + desc;
            icon = "🎨";
            enabled = rp.enabled;
        }

        iconView.setText(icon);
        nameView.setText(name);
        descView.setText(desc);
        toggle.setChecked(enabled);
        toggle.setOnCheckedChangeListener(null);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(mToggleListener != null) {
                mToggleListener.onToggle(position, isChecked);
            }
        });

        return convertView;
    }
}
