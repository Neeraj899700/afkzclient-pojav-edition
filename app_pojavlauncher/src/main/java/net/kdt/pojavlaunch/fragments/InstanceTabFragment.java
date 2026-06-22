package net.kdt.pojavlaunch.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceIconProvider;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.RTSpinnerAdapter;
import net.kdt.pojavlaunch.multirt.Runtime;
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog;
import net.kdt.pojavlaunch.utils.CropperUtils;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InstanceTabFragment extends Fragment implements CropperUtils.CropperReceiver {

    public static final String TAG = "InstanceTabFragment";

    private TextView mInstanceSelector;
    private TextView mTabMeta, mTabMods, mTabResourcePacks;
    private ScrollView mMetaContent;
    private ListView mModsList, mResourcePacksList;

    private ImageView mInstanceIcon;
    private EditText mEditorName;
    private TextView mEditorVersion;
    private Button mEditorVersionButton;
    private Spinner mEditorRuntime;
    private Spinner mEditorRenderer;
    private EditText mEditorJvmArgs;
    private TextView mEditorControl;
    private Button mEditorControlButton;
    private CheckBox mEditorSharedData;
    private Button mEditorSave;
    private Button mEditorDelete;

    private List<Instance> mInstances;
    private Instance mCurrentInstance;
    private List<String> mRenderNames;
    private int mRecommendedIconSize;
    private final ActivityResultLauncher<?> mCropperLauncher = CropperUtils.registerCropper(this, this);

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

        mInstanceIcon = view.findViewById(R.id.editor_icon);
        mEditorName = view.findViewById(R.id.editor_name);
        mEditorVersion = view.findViewById(R.id.editor_version);
        mEditorVersionButton = view.findViewById(R.id.editor_version_button);
        mEditorRuntime = view.findViewById(R.id.editor_runtime);
        mEditorRenderer = view.findViewById(R.id.editor_renderer);
        mEditorJvmArgs = view.findViewById(R.id.editor_jvm_args);
        mEditorControl = view.findViewById(R.id.editor_control);
        mEditorControlButton = view.findViewById(R.id.editor_control_button);
        mEditorSharedData = view.findViewById(R.id.editor_shared_data);
        mEditorSave = view.findViewById(R.id.editor_save);
        mEditorDelete = view.findViewById(R.id.editor_delete);

        loadInstances();

        mInstanceSelector.setOnClickListener(v -> showInstanceSelector());

        mTabMeta.setOnClickListener(v -> showTab(0));
        mTabMods.setOnClickListener(v -> showTab(1));
        mTabResourcePacks.setOnClickListener(v -> showTab(2));

        setupVersionSelector();
        setupControlSelector();
        setupRuntimeRenderer();
        setupSharedData();
        setupIcon();
        setupSaveDelete();

        if(!mInstances.isEmpty()) {
            selectInstance(mInstances.get(0));
        }
        showTab(0);
    }

    private void setupVersionSelector() {
        View.OnClickListener listener = v -> VersionSelectorDialog.open(requireContext(), false,
                (id, snapshot) -> mEditorVersion.setText(id));
        mEditorVersion.setOnClickListener(listener);
        mEditorVersionButton.setOnClickListener(listener);
    }

    private void setupControlSelector() {
        View.OnClickListener listener = v -> {
            Bundle bundle = new Bundle(3);
            bundle.putBoolean(FileSelectorFragment.BUNDLE_SELECT_FOLDER, false);
            bundle.putString(FileSelectorFragment.BUNDLE_ROOT_PATH, Tools.CTRLMAP_PATH);
            Tools.swapFragment(requireActivity(),
                    FileSelectorFragment.class, FileSelectorFragment.TAG, bundle);
        };
        mEditorControl.setOnClickListener(listener);
        mEditorControlButton.setOnClickListener(listener);
    }

    private void setupRuntimeRenderer() {
        List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        mEditorRuntime.setAdapter(new RTSpinnerAdapter(requireContext(), runtimes));

        RendererCompatUtil.RenderersList renderersList = RendererCompatUtil.getCompatibleRenderers(requireContext());
        mRenderNames = renderersList.rendererIds;
        List<String> renderList = new ArrayList<>(Arrays.asList(renderersList.rendererDisplayNames));
        renderList.add(requireContext().getString(R.string.global_default));
        mEditorRenderer.setAdapter(new android.widget.ArrayAdapter<>(requireContext(),
                R.layout.item_simple_list_1, renderList));
    }

    private void setupSharedData() {
        mEditorSharedData.setOnCheckedChangeListener((v, checked) -> {
            if(mCurrentInstance != null) {
                mCurrentInstance.sharedData = checked;
            }
            mEditorSharedData.setText(checked ? "Shared data: ON" : "Shared data: OFF");
        });
    }

    private void setupIcon() {
        mInstanceIcon.setOnClickListener(v -> {
            mRecommendedIconSize = Math.max(v.getWidth(), v.getHeight());
            CropperUtils.startCropper(mCropperLauncher);
        });
    }

    private void setupSaveDelete() {
        mEditorSave.setOnClickListener(v -> saveInstance());
        mEditorDelete.setOnClickListener(v -> {
            DeleteConfirmDialogFragment dialog = new DeleteConfirmDialogFragment();
            dialog.show(getChildFragmentManager(), "delete_dialog");
        });
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

        mInstanceIcon.setImageDrawable(
                InstanceIconProvider.fetchIcon(getResources(), mCurrentInstance));
        mEditorName.setText(nullToEmpty(mCurrentInstance.name));
        mEditorVersion.setText(mCurrentInstance.versionId);
        mEditorJvmArgs.setText(nullToEmpty(mCurrentInstance.jvmArgs));
        mEditorControl.setText(nullToEmpty(mCurrentInstance.controlLayout));
        mEditorSharedData.setChecked(mCurrentInstance.sharedData);

        String value = (String) ExtraCore.consumeValue(ExtraConstants.FILE_SELECTOR);
        if(value != null) {
            mEditorControl.setText(value);
        }

        List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        int jvmIndex = -1;
        if(mCurrentInstance.selectedRuntime != null) {
            jvmIndex = runtimes.indexOf(new Runtime(mCurrentInstance.selectedRuntime));
        }
        mEditorRuntime.setAdapter(new RTSpinnerAdapter(requireContext(), runtimes));
        if(jvmIndex == -1) jvmIndex = runtimes.size() - 1;
        mEditorRuntime.setSelection(jvmIndex);

        int rendererIndex = mRenderNames != null ? mRenderNames.indexOf(mCurrentInstance.getLaunchRenderer()) : -1;
        if(rendererIndex == -1 && mEditorRenderer.getAdapter() != null) {
            rendererIndex = mEditorRenderer.getAdapter().getCount() - 1;
        }
        if(rendererIndex >= 0) mEditorRenderer.setSelection(rendererIndex);
    }

    private void saveInstance() {
        if(mCurrentInstance == null) return;

        mCurrentInstance.versionId = mEditorVersion.getText().toString();
        mCurrentInstance.controlLayout = mEditorControl.getText().toString();
        mCurrentInstance.name = mEditorName.getText().toString();
        mCurrentInstance.jvmArgs = mEditorJvmArgs.getText().toString();

        if(mCurrentInstance.controlLayout.isEmpty()) mCurrentInstance.controlLayout = null;
        if(mCurrentInstance.jvmArgs.isEmpty()) mCurrentInstance.jvmArgs = null;

        Runtime selectedRuntime = (Runtime) mEditorRuntime.getSelectedItem();
        mCurrentInstance.selectedRuntime = (selectedRuntime.name.equals("<Default>") || selectedRuntime.versionString == null)
                ? null : selectedRuntime.name;

        if(mEditorRenderer.getSelectedItemPosition() == mRenderNames.size()) {
            mCurrentInstance.renderer = null;
        } else {
            mCurrentInstance.renderer = mRenderNames.get(mEditorRenderer.getSelectedItemPosition());
        }

        try {
            mCurrentInstance.write();
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
            String displayName = mCurrentInstance.name != null && !mCurrentInstance.name.isEmpty()
                    ? mCurrentInstance.name : "Unnamed";
            mInstanceSelector.setText(displayName);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String nullToEmpty(String in) {
        return in != null ? in : "";
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

    @Override
    public float getAspectRatio() { return 1f; }

    @Override
    public int getTargetMaxSide() { return mRecommendedIconSize; }

    @Override
    public void onCropped(Bitmap contentBitmap) {
        if(mCurrentInstance == null) return;
        mInstanceIcon.setImageBitmap(contentBitmap);
        try {
            mCurrentInstance.encodeNewIcon(contentBitmap);
        } catch (IOException e) {
            Tools.showErrorRemote(e);
        }
    }

    @Override
    public void onFailed(Exception exception) {
        Tools.showErrorRemote(exception);
    }
}
