package net.kdt.pojavlaunch.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;
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
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InstanceTabFragment extends Fragment implements CropperUtils.CropperReceiver {

    public static final String TAG = "InstanceTabFragment";

    private TextView mInstanceSelector;
    private TextView mTabMeta, mTabMods, mTabResourcePacks;
    private ScrollView mMetaContent;
    private LinearLayout mModsContainer, mResourcePacksContainer;
    private ListView mModsList, mResourcePacksList;
    private TextView mModsHeader;
    private EditText mModsSearch, mResourcePacksSearch;

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
    private TextView mEditorDiskUsage;
    private TextView mEditorLastPlayed;
    private EditText mEditorResWidth, mEditorResHeight;
    private Spinner mEditorAccount;
    private Button mEditorSave;
    private Button mEditorDelete;
    private Button mEditorOpenDir;
    private Button mEditorDuplicate;
    private Button mEditorExport;
    private Button mEditorOpenScreenshots;
    private TextView mEditorScreenshotsInfo;

    private List<Instance> mInstances;
    private Instance mCurrentInstance;
    private List<String> mRenderNames;
    private int mRecommendedIconSize;
    private FileListAdapter mModsAdapter, mResourcePacksAdapter;
    private List<ModEntry> mModEntries;
    private List<ResourcePackEntry> mResourcePackEntries;
    private List<MinecraftAccount> mAccounts;
    private boolean mAccountsLoaded;
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
        mModsContainer = view.findViewById(R.id.mods_container);
        mModsList = view.findViewById(R.id.mods_list);
        mModsHeader = view.findViewById(R.id.mods_header);
        mModsSearch = view.findViewById(R.id.mods_search);
        mResourcePacksContainer = view.findViewById(R.id.resourcepacks_container);
        mResourcePacksList = view.findViewById(R.id.resourcepacks_list);
        mResourcePacksSearch = view.findViewById(R.id.resourcepacks_search);

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
        mEditorDiskUsage = view.findViewById(R.id.editor_disk_usage);
        mEditorLastPlayed = view.findViewById(R.id.editor_last_played);
        mEditorResWidth = view.findViewById(R.id.editor_res_width);
        mEditorResHeight = view.findViewById(R.id.editor_res_height);
        mEditorAccount = view.findViewById(R.id.editor_account);
        mEditorSave = view.findViewById(R.id.editor_save);
        mEditorDelete = view.findViewById(R.id.editor_delete);
        mEditorOpenDir = view.findViewById(R.id.editor_open_dir);
        mEditorDuplicate = view.findViewById(R.id.editor_duplicate);
        mEditorExport = view.findViewById(R.id.editor_export);
        mEditorOpenScreenshots = view.findViewById(R.id.editor_open_screenshots);
        mEditorScreenshotsInfo = view.findViewById(R.id.editor_screenshots_info);

        loadInstances();

        mInstanceSelector.setOnClickListener(v -> showInstanceSelector());

        mTabMeta.setOnClickListener(v -> showTab(0));
        mTabMods.setOnClickListener(v -> {
            showTab(1);
            if(mModsAdapter != null) mModsAdapter.filter(mModsSearch.getText().toString());
        });
        mTabResourcePacks.setOnClickListener(v -> {
            showTab(2);
            if(mResourcePacksAdapter != null) mResourcePacksAdapter.filter(mResourcePacksSearch.getText().toString());
        });

        setupVersionSelector();
        setupControlSelector();
        setupRuntimeRenderer();
        setupSharedData();
        setupIcon();
        setupSaveDelete();
        setupSearch();
        setupDuplicateExport();
        setupScreenshots();
        loadAccounts();

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
        mEditorOpenDir.setOnClickListener(v -> openGameDirectory());
    }

    private void setupSearch() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                if(mModsAdapter != null && mModsSearch.isFocused()) {
                    mModsAdapter.filter(query);
                }
                if(mResourcePacksAdapter != null && mResourcePacksSearch.isFocused()) {
                    mResourcePacksAdapter.filter(query);
                }
            }
        };
        mModsSearch.addTextChangedListener(watcher);
        mResourcePacksSearch.addTextChangedListener(watcher);
    }

    private void setupDuplicateExport() {
        mEditorDuplicate.setOnClickListener(v -> duplicateProfile());
        mEditorExport.setOnClickListener(v -> exportProfile());
    }

    private void setupScreenshots() {
        mEditorOpenScreenshots.setOnClickListener(v -> openScreenshots());
    }

    private void loadAccounts() {
        try {
            Accounts accounts = Accounts.load();
            mAccounts = new ArrayList<>(accounts.accounts);
        } catch (IOException e) {
            mAccounts = new ArrayList<>();
        }
        List<String> names = new ArrayList<>();
        names.add("Default (global)");
        for(MinecraftAccount acc : mAccounts) {
            names.add(acc.username);
        }
        mEditorAccount.setAdapter(new ArrayAdapter<>(requireContext(),
                R.layout.item_simple_list_1, names));
        mAccountsLoaded = true;
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

        updateDiskUsage();
        updateLastPlayed();
        updateScreenshotsInfo();

        mEditorResWidth.setText(mCurrentInstance.resolutionWidth > 0 ? String.valueOf(mCurrentInstance.resolutionWidth) : "");
        mEditorResHeight.setText(mCurrentInstance.resolutionHeight > 0 ? String.valueOf(mCurrentInstance.resolutionHeight) : "");

        // Account spinner
        if(mAccountsLoaded && mAccounts != null) {
            int accIdx = 0; // Default
            if(mCurrentInstance.accountUUID != null) {
                for(int i = 0; i < mAccounts.size(); i++) {
                    if(mAccounts.get(i).profileId.equals(mCurrentInstance.accountUUID)) {
                        accIdx = i + 1;
                        break;
                    }
                }
            }
            final int idx = accIdx;
            mEditorAccount.setSelection(idx);
        }

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

        // Resolution
        try {
            int w = Integer.parseInt(mEditorResWidth.getText().toString());
            mCurrentInstance.resolutionWidth = Math.max(0, w);
        } catch(NumberFormatException e) { mCurrentInstance.resolutionWidth = 0; }
        try {
            int h = Integer.parseInt(mEditorResHeight.getText().toString());
            mCurrentInstance.resolutionHeight = Math.max(0, h);
        } catch(NumberFormatException e) { mCurrentInstance.resolutionHeight = 0; }

        // Account
        int accPos = mEditorAccount.getSelectedItemPosition();
        if(accPos > 0 && accPos - 1 < mAccounts.size()) {
            mCurrentInstance.accountUUID = mAccounts.get(accPos - 1).profileId;
        } else {
            mCurrentInstance.accountUUID = null;
        }

        mCurrentInstance.lastPlayed = System.currentTimeMillis();

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

    private void duplicateProfile() {
        if(mCurrentInstance == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Duplicate Profile");
        final EditText input = new EditText(requireContext());
        input.setHint("New profile name");
        input.setText(mCurrentInstance.name + " (Copy)");
        builder.setView(input);
        builder.setPositiveButton("Duplicate", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if(newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                final String copyName = newName;
                final Instance src = mCurrentInstance;
                Instance newInstance = Instances.createInstance(setter -> {
                    setter.name = copyName;
                    setter.versionId = src.versionId;
                    setter.selectedRuntime = src.selectedRuntime;
                    setter.renderer = src.renderer;
                    setter.jvmArgs = src.jvmArgs;
                    setter.controlLayout = src.controlLayout;
                    setter.sharedData = src.sharedData;
                    setter.resolutionWidth = src.resolutionWidth;
                    setter.resolutionHeight = src.resolutionHeight;
                    setter.accountUUID = src.accountUUID;
                }, null);

                // Copy icon
                File srcIcon = new File(src.mInstanceRoot, "icon.webp");
                if(srcIcon.exists()) {
                    File dstIcon = new File(newInstance.mInstanceRoot, "icon.webp");
                    try (FileInputStream fis = new FileInputStream(srcIcon);
                         FileOutputStream fos = new FileOutputStream(dstIcon)) {
                        byte[] buf = new byte[4096];
                        int n;
                        while((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
                    } catch(Exception ignored) {}
                }

                Instances.setSelectedInstance(newInstance);
                Toast.makeText(requireContext(), "Profile duplicated", Toast.LENGTH_SHORT).show();

                // Reload and select
                loadInstances();
                for(Instance inst : mInstances) {
                    if(inst.mInstanceRoot.equals(newInstance.mInstanceRoot)) {
                        selectInstance(inst);
                        break;
                    }
                }
            } catch (IOException e) {
                Toast.makeText(requireContext(), "Duplicate failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void exportProfile() {
        if(mCurrentInstance == null) return;
        String name = mCurrentInstance.name != null ? mCurrentInstance.name : "Unnamed";
        try {
            File exportsDir = new File(Tools.DIR_GAME_HOME, "exports");
            FileUtils.ensureDirectory(exportsDir);
            File exportFile = new File(exportsDir, name + ".json");
            JSONUtils.writeToFile(exportFile, mCurrentInstance);
            Toast.makeText(requireContext(), "Exported to " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateDiskUsage() {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        new Thread(() -> {
            long size = folderSize(gameDir);
            String human = humanReadableSize(size);
            requireActivity().runOnUiThread(() ->
                    mEditorDiskUsage.setText(human));
        }).start();
    }

    private void updateLastPlayed() {
        if(mCurrentInstance == null) return;
        if(mCurrentInstance.lastPlayed > 0) {
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    .format(new Date(mCurrentInstance.lastPlayed));
            mEditorLastPlayed.setText(date);
        } else {
            mEditorLastPlayed.setText("Never");
        }
    }

    private void updateScreenshotsInfo() {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        File screenshotsDir = new File(gameDir, "screenshots");
        if(screenshotsDir.exists()) {
            File[] shots = screenshotsDir.listFiles((dir, name) ->
                    name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"));
            if(shots != null && shots.length > 0) {
                mEditorScreenshotsInfo.setText(shots.length + " screenshot" + (shots.length != 1 ? "s" : ""));
                return;
            }
        }
        mEditorScreenshotsInfo.setText("No screenshots");
    }

    private void openScreenshots() {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        File screenshotsDir = new File(gameDir, "screenshots");
        if(!screenshotsDir.exists()) screenshotsDir.mkdirs();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(screenshotsDir), "resource/folder");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch(Exception e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_VIEW);
                fallback.setDataAndType(Uri.fromFile(screenshotsDir), "*/*");
                startActivity(fallback);
            } catch(Exception e2) {
                Toast.makeText(requireContext(), "No file manager found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static String nullToEmpty(String in) {
        return in != null ? in : "";
    }

    private void showTab(int tab) {
        mMetaContent.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        mModsContainer.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        mResourcePacksContainer.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);

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
        mModEntries = new ArrayList<>();

        File[] allFiles = modsDir.listFiles();
        int total = 0, enabled = 0;
        if(allFiles != null) {
            for(File f : allFiles) {
                String name = f.getName();
                boolean isEnabled = !name.endsWith(".disabled");
                String realName = name.replaceAll("\\.disabled$", "");
                if(!realName.endsWith(".jar")) continue;

                total++;
                if(isEnabled) enabled++;

                ModMetadata meta = readModMetadata(f);
                mModEntries.add(new ModEntry(realName,
                        meta != null ? meta.name : null,
                        meta != null ? meta.description : null,
                        meta != null ? meta.author : null,
                        meta != null ? meta.version : null,
                        isEnabled));
            }
        }

        int disabled = total - enabled;
        mModsHeader.setText(total + " mod" + (total != 1 ? "s" : "") + " loaded"
                + (disabled > 0 ? ", " + disabled + " disabled" : ""));

        mModsAdapter = new FileListAdapter(LayoutInflater.from(requireContext()), mModEntries,
                (position, isChecked) -> {
                    Object item = mModsAdapter.getItem(position);
                    if(item instanceof ModEntry) toggleMod((ModEntry) item);
                });
        mModsList.setAdapter(mModsAdapter);
    }

    private void loadResourcePacksList() {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        File rpDir = new File(gameDir, "resourcepacks");
        mResourcePackEntries = new ArrayList<>();

        File[] allFiles = rpDir.listFiles();
        if(allFiles != null) {
            for(File f : allFiles) {
                String name = f.getName();
                boolean enabled = !name.endsWith(".disabled");
                String realName = name.replaceAll("\\.disabled$", "");
                if(!realName.endsWith(".zip")) continue;

                PackMeta meta = readPackMeta(f);
                mResourcePackEntries.add(new ResourcePackEntry(realName,
                        meta != null ? meta.name : null,
                        meta != null ? meta.description : null,
                        meta != null ? meta.format : 0,
                        enabled, f));
            }
        }

        mResourcePacksAdapter = new FileListAdapter(LayoutInflater.from(requireContext()), mResourcePackEntries,
                (position, isChecked) -> {
                    Object item = mResourcePacksAdapter.getItem(position);
                    if(item instanceof ResourcePackEntry) toggleResourcePack((ResourcePackEntry) item);
                });
        mResourcePacksList.setAdapter(mResourcePacksAdapter);
    }

    private void toggleMod(ModEntry entry) {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        File modsDir = new File(gameDir, "mods");
        File source = new File(modsDir, entry.enabled ? entry.fileName : entry.fileName + ".disabled");
        File target = new File(modsDir, entry.enabled ? entry.fileName + ".disabled" : entry.fileName);
        if(source.renameTo(target)) {
            loadModsList();
        }
    }

    private void toggleResourcePack(ResourcePackEntry entry) {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        File rpDir = new File(gameDir, "resourcepacks");
        File source = new File(rpDir, entry.enabled ? entry.fileName : entry.fileName + ".disabled");
        File target = new File(rpDir, entry.enabled ? entry.fileName + ".disabled" : entry.fileName);
        if(source.renameTo(target)) {
            loadResourcePacksList();
        }
    }

    private void openGameDirectory() {
        if(mCurrentInstance == null) return;
        File gameDir = mCurrentInstance.getGameDirectory();
        if(!gameDir.exists()) {
            Toast.makeText(requireContext(), "Directory does not exist", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(gameDir), "resource/folder");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch(Exception e) {
            try {
                Intent fallback = new Intent(Intent.ACTION_VIEW);
                fallback.setDataAndType(Uri.fromFile(gameDir), "*/*");
                startActivity(fallback);
            } catch(Exception e2) {
                Toast.makeText(requireContext(), "No file manager found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private long folderSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if(files == null) return 0;
        for(File f : files) {
            if(f.isDirectory()) {
                size += folderSize(f);
            } else {
                size += f.length();
            }
        }
        return size;
    }

    private static class ModMetadata {
        String name, description, author, version;
    }

    private static class PackMeta {
        String name, description;
        int format;
    }

    private ModMetadata readModMetadata(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            if(entry != null) {
                java.util.Scanner scanner = new java.util.Scanner(zip.getInputStream(entry)).useDelimiter("\\A");
                String json = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
                ModMetadata meta = new ModMetadata();
                if(obj.has("name")) meta.name = obj.get("name").getAsString();
                if(obj.has("description")) meta.description = obj.get("description").getAsString();
                if(obj.has("version")) meta.version = obj.get("version").getAsString();
                if(obj.has("authors")) {
                    try { meta.author = obj.get("authors").getAsJsonArray().get(0).getAsString(); }
                    catch(Exception ignored) { meta.author = obj.get("authors").getAsString(); }
                }
                return meta;
            }
            entry = zip.getEntry("mcmod.info");
            if(entry != null) {
                java.util.Scanner scanner = new java.util.Scanner(zip.getInputStream(entry)).useDelimiter("\\A");
                String json = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                com.google.gson.JsonArray arr = new com.google.gson.JsonParser().parse(json).getAsJsonArray();
                if(arr.size() > 0) {
                    com.google.gson.JsonObject obj = arr.get(0).getAsJsonObject();
                    ModMetadata meta = new ModMetadata();
                    if(obj.has("name")) meta.name = obj.get("name").getAsString();
                    if(obj.has("description")) meta.description = obj.get("description").getAsString();
                    if(obj.has("version")) meta.version = obj.get("version").getAsString();
                    if(obj.has("author")) meta.author = obj.get("author").getAsString();
                    return meta;
                }
            }
        } catch(Exception ignored) {}
        return null;
    }

    private PackMeta readPackMeta(File file) {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry entry = zip.getEntry("pack.mcmeta");
            if(entry != null) {
                java.util.Scanner scanner = new java.util.Scanner(zip.getInputStream(entry)).useDelimiter("\\A");
                String json = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
                com.google.gson.JsonObject pack = obj.getAsJsonObject("pack");
                if(pack != null) {
                    PackMeta meta = new PackMeta();
                    if(pack.has("pack_format")) meta.format = pack.get("pack_format").getAsInt();
                    if(pack.has("description")) {
                        try { meta.description = pack.get("description").getAsString(); }
                        catch(Exception e) { meta.description = pack.get("description").toString(); }
                    }
                    return meta;
                }
            }
        } catch(Exception ignored) {}
        return null;
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
