package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import git.artdeell.mojo.R;

import java.util.List;

public class FileListAdapter extends ArrayAdapter<String[]> {

    public FileListAdapter(Context context, List<String[]> items) {
        super(context, R.layout.item_file_list, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_file_list, parent, false);
        }
        String[] item = getItem(position);
        TextView fileName = convertView.findViewById(R.id.file_name);
        TextView fileDesc = convertView.findViewById(R.id.file_description);
        if(item != null) {
            fileName.setText(item[0]);
            fileDesc.setText(item.length > 1 ? item[1] : "");
        }
        return convertView;
    }
}
