package net.kdt.pojavlaunch.fragments;

public class ModEntry {
    public final String fileName;
    public final String displayName;
    public final String description;
    public final String author;
    public final String version;
    public final boolean enabled;

    public ModEntry(String fileName, String displayName, String description, String author, String version, boolean enabled) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.description = description;
        this.author = author;
        this.version = version;
        this.enabled = enabled;
    }
}
