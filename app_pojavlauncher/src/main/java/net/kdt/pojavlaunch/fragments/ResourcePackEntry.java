package net.kdt.pojavlaunch.fragments;

public class ResourcePackEntry {
    public final String fileName;
    public final String displayName;
    public final String description;
    public final int packFormat;
    public final boolean enabled;
    public final File file;

    public ResourcePackEntry(String fileName, String displayName, String description, int packFormat, boolean enabled, File file) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.description = description;
        this.packFormat = packFormat;
        this.enabled = enabled;
        this.file = file;
    }
}
