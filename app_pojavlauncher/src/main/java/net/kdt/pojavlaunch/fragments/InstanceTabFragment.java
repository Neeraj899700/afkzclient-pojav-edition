package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.RTSpinnerAdapter;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog;
import net.kdt.pojavlaunch.profiles.VersionSelectorListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InstanceTabFragment extends Fragment {

    public static final String TAG = "InstanceTabFragment";

    private TextView mInstanceSelector;
    private TextView mTabMeta, mTabMods, mTabResourcePacks;
    private ScrollView mMetaContent;
    private ListView mModsList, mResourcePacksList;

    private EditText mEditorName;
    private TextView mEditorVersion;
    private Spinner mEditorRuntime;
    private EditText mEditorJvmArgs;
    private Button mEditorSave;

    private List<Instance> mInstances;
    private Instance mCurrentInstance;
    private RTSpinnerAdapter mRuntimeAdapter;

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

        mEditorName = view.findViewById(R.id.editor_name);
        mEditorVersion = view.findViewById(R.id.editor_version);
        mEditorRuntime = view.findViewById(R.id.editor_runtime);
        mEditorJvmArgs = view.findViewById(R.id.editor_jvm_args);
        mEditorSave = view.findViewById(R.id.editor_save);

        loadInstances();

        mInstanceSelector.setOnClickListener(v -> showInstanceSelector());

        mTabMeta.setOnClickListener(v -> showTab(0));
        mTabMods.setOnClickListener(v -> showTab(1));
        mTabResourcePacks.setOnClickListener(v -> showTab(2));

        mEditorVersion.setOnClickListener(v -> {
            if(mCurrentInstance != null) {
                VersionSelectorDialog.open(requireActivity(), false, (version, isSnapshot) -> {
                    mCurrentInstance.versionId = version;
                    mEditorVersion.setText(version);
                });
            }
        });

        mEditorSave.setOnClickListener(v -> saveInstance());

        setupRuntimeSpinner();

        if(!mInstances.isEmpty()) {
            selectInstance(mInstances.get(0));
        }
        showTab(0);
    }

    private void setupRuntimeSpinner() {
        List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        mRuntimeAdapter = new RTSpinnerAdapter(requireContext(), runtimes);
        mEditorRuntime.setAdapter(mRuntimeAdapter);
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
        populateEditor();
    }

    private void populateEditor() {
        if(mCurrentInstance == null) return;
        mEditorName.setText(mCurrentInstance.name != null ? mCurrentInstance.name : "");
        mEditorVersion.setText(mCurrentInstance.versionId != null ? mCurrentInstance.versionId : "Not set");
        mEditorJvmArgs.setText(mCurrentInstance.jvmArgs != null ? mCurrentInstance.jvmArgs : "");

        if(mRuntimeAdapter != null) {
            String selectedRuntime = mCurrentInstance.selectedRuntime;
            for(int i = 0; i < mRuntimeAdapter.getCount(); i++) {
                Runtime rt = (Runtime) mRuntimeAdapter.getItem(i);
                if(rt.name.equals(selectedRuntime)) {
                    mEditorRuntime.setSelection(i);
                    break;
                }
            }
        }
    }

    private void saveInstance() {
        if(mCurrentInstance == null) return;
        String name = mEditorName.getText().toString().trim();
        if(!name.isEmpty()) mCurrentInstance.name = name;
        String jvmArgs = mEditorJvmArgs.getText().toString().trim();
        mCurrentInstance.jvmArgs = !jvmArgs.isEmpty() ? jvmArgs : null;
        if(mEditorRuntime.getSelectedItem() instanceof Runtime) {
            Runtime rt = (Runtime) mEditorRuntime.getSelectedItem();
            mCurrentInstance.selectedRuntime = rt.name;
        }
        try {
            mCurrentInstance.write();
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
            mInstanceSelector.setText(mCurrentInstance.name != null ? mCurrentInstance.name : "Unnamed");
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showTab(int tab) {
        mMetaContent.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        mModsList.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        mResourcePacksList.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

        int green = 0xFF57CC33;
        int white = 0xFFFFFFFF;
        mTabMeta.setTextColor(tab == 0 ? green : white);
        mTabMods.setTextColor(tab == 1 ? green : white);
        mTabResourcePacks.setTextColor(tab == 2 ? green : white);

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
