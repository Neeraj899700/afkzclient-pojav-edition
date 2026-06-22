package net.kdt.pojavlaunch.fragments;

public class ResourcePackEntry {
    public final String fileName;
    public final String displayName;
    public final String description;
    public final int packFormat;
    public final boolean enabled;

    public ResourcePackEntry(String fileName, String displayName, String description, int packFormat, boolean enabled) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.description = description;
        this.packFormat = packFormat;
        this.enabled = enabled;
    }
}
