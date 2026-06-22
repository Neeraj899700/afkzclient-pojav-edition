package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.Tools;
import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InstanceTabFragment extends Fragment {

    public static final String TAG = "InstanceTabFragment";

    private TextView mInstanceSelector;
    private Button mTabMeta, mTabMods, mTabResourcePacks;
    private ScrollView mMetaContent;
    private ListView mModsList, mResourcePacksList;

    private TextView mMetaName, mMetaVersion, mMetaRuntime, mMetaRenderer, mMetaJvm, mMetaControl;

    private List<Instance> mInstances;
    private Instance mCurrentInstance;

    public InstanceTabFragment() {
        super(R.layout.fragment_instance_tab);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mInstanceSelector = view.findViewById(R.id.instance_selector);
        mTabMeta = view.findViewById(R.id.tab_meta);
        mTabMods = view.findViewById(R.id.tab_mods);
        mTabResourcePacks = view.findViewById(R.id.tab_resourcepacks);
        mMetaContent = view.findViewById(R.id.meta_content);
        mModsList = view.findViewById(R.id.mods_list);
        mResourcePacksList = view.findViewById(R.id.resourcepacks_list);

        mMetaName = view.findViewById(R.id.meta_name);
        mMetaVersion = view.findViewById(R.id.meta_version);
        mMetaRuntime = view.findViewById(R.id.meta_runtime);
        mMetaRenderer = view.findViewById(R.id.meta_renderer);
        mMetaJvm = view.findViewById(R.id.meta_jvm);
        mMetaControl = view.findViewById(R.id.meta_control);

        loadInstances();

        mInstanceSelector.setOnClickListener(v -> showInstanceSelector());

        mTabMeta.setOnClickListener(v -> showTab(0));
        mTabMods.setOnClickListener(v -> showTab(1));
        mTabResourcePacks.setOnClickListener(v -> showTab(2));

        if(!mInstances.isEmpty()) {
            selectInstance(mInstances.get(0));
        }
        showTab(0);
    }

    private void loadInstances() {
        try {
            mInstances = Instances.loadAllInstances();
        } catch (IOException e) {
            mInstances = new ArrayList<>();
            Toast.makeText(requireContext(), "Failed to load instances", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInstanceSelector() {
        if(mInstances.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_instance, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[mInstances.size()];
        for(int i = 0; i < mInstances.size(); i++) {
            names[i] = mInstances.get(i).name != null ? mInstances.get(i).name : "Unnamed";
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Profile")
                .setItems(names, (dialog, which) -> selectInstance(mInstances.get(which)))
                .show();
    }

    private void selectInstance(Instance instance) {
        mCurrentInstance = instance;
        Instances.setSelectedInstance(instance);
        String displayName = instance.name != null ? instance.name : "Unnamed";
        mInstanceSelector.setText(displayName);
        updateMetaView();
    }

    private void updateMetaView() {
        if(mCurrentInstance == null) return;
        mMetaName.setText(mCurrentInstance.name != null ? mCurrentInstance.name : "Unnamed");
        mMetaVersion.setText(mCurrentInstance.versionId != null ? mCurrentInstance.versionId : "Not set");
        mMetaRuntime.setText(mCurrentInstance.selectedRuntime != null ? mCurrentInstance.selectedRuntime : "Default");
        mMetaRenderer.setText(mCurrentInstance.renderer != null ? mCurrentInstance.renderer : "Default");
        mMetaJvm.setText(mCurrentInstance.jvmArgs != null ? mCurrentInstance.jvmArgs : "Default");
        mMetaControl.setText(mCurrentInstance.controlLayout != null ? mCurrentInstance.controlLayout : "Default");
    }

    private void showTab(int tab) {
        mMetaContent.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        mModsList.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        mResourcePacksList.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        mTabMeta.setSelected(tab == 0);
        mTabMods.setSelected(tab == 1);
        mTabResourcePacks.setSelected(tab == 2);

        if(tab == 1) loadModsList();
        else if(tab == 2) loadResourcePacksList();
    }

    private void loadModsList() {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        File modsDir = new File(gameDir, "mods");
        File[] modFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        List<String[]> items = new ArrayList<>();
        if(modFiles != null) {
            for(File f : modFiles) {
                items.add(new String[]{f.getName(), humanReadableSize(f.length())});
            }
        }
        mModsList.setAdapter(new FileListAdapter(requireContext(), items));
    }

    private void loadResourcePacksList() {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        File rpDir = new File(gameDir, "resourcepacks");
        File[] rpFiles = rpDir.listFiles((dir, name) -> name.endsWith(".zip"));
        List<String[]> items = new ArrayList<>();
        if(rpFiles != null) {
            for(File f : rpFiles) {
                items.add(new String[]{f.getName(), humanReadableSize(f.length())});
            }
        }
        mResourcePacksList.setAdapter(new FileListAdapter(requireContext(), items));
    }

    private String humanReadableSize(long bytes) {
        if(bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }
}
