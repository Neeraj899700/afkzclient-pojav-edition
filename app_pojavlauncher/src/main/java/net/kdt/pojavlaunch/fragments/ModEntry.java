package net.kdt.pojavlaunch.fragments;

public class ModEntry {
    public final String fileName;
    public final String displayName;
    public final String description;
    public final String author;
    public final boolean enabled;

    public ModEntry(String fileName, String displayName, String description, String author, boolean enabled) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.description = description;
        this.author = author;
        this.enabled = enabled;
    }
}
