package net.kdt.pojavlaunch.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import git.artdeell.mojo.R;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileListAdapter extends BaseAdapter implements Filterable {

    public interface ToggleListener {
        void onToggle(int position, boolean isChecked);
    }

    private static final int COLOR_MOD = 0xFF57CC33;
    private static final int COLOR_PACK = 0xFF3388CC;

    private final List<?> mItems;
    private List<?> mFilteredItems;
    private final LayoutInflater mInflater;
    private final ToggleListener mToggleListener;
    private final boolean mShowToggle;
    private String mFilter;

    public FileListAdapter(LayoutInflater inflater, List<?> items, ToggleListener listener) {
        this(inflater, items, listener, true);
    }

    public FileListAdapter(LayoutInflater inflater, List<?> items, ToggleListener listener, boolean showToggle) {
        mInflater = inflater;
        mItems = items;
        mFilteredItems = items;
        mToggleListener = listener;
        mShowToggle = showToggle;
    }

    public void filter(String query) {
        mFilter = query.toLowerCase();
        if(mFilter.isEmpty()) {
            mFilteredItems = mItems;
        } else {
            List<Object> filtered = new ArrayList<>();
            for(Object item : mItems) {
                String name = "";
                if(item instanceof ModEntry) name = ((ModEntry) item).displayName;
                else if(item instanceof ResourcePackEntry) name = ((ResourcePackEntry) item).displayName;
                if(name != null && name.toLowerCase().contains(mFilter)) {
                    filtered.add(item);
                }
            }
            mFilteredItems = filtered;
        }
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults res = new FilterResults();
                res.values = mFilteredItems;
                res.count = mFilteredItems.size();
                return res;
            }
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {}
        };
    }

    @Override
    public int getCount() { return mFilteredItems.size(); }

    @Override
    public Object getItem(int position) { return mFilteredItems.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.item_file_list, parent, false);
        }

        TextView iconText = convertView.findViewById(R.id.file_icon_text);
        ImageView iconImage = convertView.findViewById(R.id.file_icon_image);
        TextView nameView = convertView.findViewById(R.id.file_name);
        TextView descView = convertView.findViewById(R.id.file_description);
        SwitchCompat toggle = convertView.findViewById(R.id.file_toggle);

        Object item = mFilteredItems.get(position);
        String name = "";
        String desc = "";
        boolean enabled = true;

        if(item instanceof ModEntry) {
            ModEntry mod = (ModEntry) item;
            name = mod.displayName != null ? mod.displayName : mod.fileName;
            String parts = "";
            if(mod.version != null && !mod.version.isEmpty()) parts = "v" + mod.version;
            if(mod.author != null && !mod.author.isEmpty()) {
                if(!parts.isEmpty()) parts += " ";
                parts += "by " + mod.author;
            }
            desc = mod.description != null && !mod.description.isEmpty() ? mod.description : parts;
            if(!mod.enabled) desc = "DISABLED - " + desc;
            enabled = mod.enabled;
            iconText.setText("M");
            iconText.setTextColor(0xFFFFFFFF);
            iconImage.setVisibility(View.GONE);
            iconText.setVisibility(View.VISIBLE);
            iconText.setPadding(0, 0, 0, 0);
            iconText.setBackgroundColor(COLOR_MOD);

            Bitmap modIcon = loadModIcon(mod.file, mod.iconPath);
            if(modIcon != null) {
                iconText.setVisibility(View.GONE);
                iconImage.setVisibility(View.VISIBLE);
                iconImage.setImageBitmap(modIcon);
            }
            toggle.setVisibility(mShowToggle ? View.VISIBLE : View.GONE);
        } else if(item instanceof ResourcePackEntry) {
            ResourcePackEntry rp = (ResourcePackEntry) item;
            name = rp.displayName != null ? rp.displayName : rp.fileName;
            String fmt = rp.packFormat > 0 ? " (format " + rp.packFormat + ")" : "";
            desc = rp.description != null && !rp.description.isEmpty() ? rp.description + fmt : fmt;
            enabled = rp.enabled;

            // Try to load pack.png thumbnail
            Bitmap thumbnail = loadPackThumbnail(rp.file);
            if(thumbnail != null) {
                iconText.setVisibility(View.INVISIBLE);
                iconImage.setVisibility(View.VISIBLE);
                iconImage.setImageBitmap(thumbnail);
            } else {
                iconText.setVisibility(View.VISIBLE);
                iconImage.setVisibility(View.GONE);
                iconText.setText("R");
                iconText.setTextColor(0xFFFFFFFF);
                iconText.setBackgroundResource(android.R.color.transparent);
                GradientDrawable rpBg = new GradientDrawable();
                rpBg.setShape(GradientDrawable.OVAL);
                rpBg.setSize(36, 36);
                rpBg.setColor(COLOR_PACK);
                iconText.setBackground(rpBg);
            }
            toggle.setVisibility(View.GONE);
        }

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

    private Bitmap loadPackThumbnail(java.io.File zipFile) {
        if(zipFile == null || !zipFile.exists()) return null;
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry entry = zip.getEntry("pack.png");
            if(entry != null) {
                try (InputStream is = zip.getInputStream(entry)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    if(bitmap != null) {
                        int size = (int) (36 * mInflater.getContext().getResources().getDisplayMetrics().density);
                        return Bitmap.createScaledBitmap(bitmap, size, size, true);
                    }
                }
            }
        } catch(Exception ignored) {}
        return null;
    }

    private Bitmap loadModIcon(java.io.File jarFile, String iconPath) {
        if(jarFile == null || !jarFile.exists()) return null;
        try (ZipFile zip = new ZipFile(jarFile)) {
            // Try the icon path from fabric.mod.json first
            if(iconPath != null) {
                ZipEntry entry = zip.getEntry(iconPath);
                if(entry != null) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if(bitmap != null) {
                            int size = (int) (36 * mInflater.getContext().getResources().getDisplayMetrics().density);
                            return Bitmap.createScaledBitmap(bitmap, size, size, true);
                        }
                    }
                }
            }
            // Fallbacks
            String[] candidates = {"pack.png", "logo.png", "assets/logo.png", "icon.png"};
            for(String path : candidates) {
                ZipEntry entry = zip.getEntry(path);
                if(entry != null) {
                    try (InputStream is = zip.getInputStream(entry)) {
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if(bitmap != null) {
                            int size = (int) (36 * mInflater.getContext().getResources().getDisplayMetrics().density);
                            return Bitmap.createScaledBitmap(bitmap, size, size, true);
                        }
                    }
                }
            }
        } catch(Exception ignored) {}
        return null;
    }
}
